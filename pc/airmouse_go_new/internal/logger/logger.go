package logger

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"sync"
	"time"
)

// Level represents log severity.
type Level int

const (
	LevelDebug Level = iota
	LevelInfo
	LevelWarn
	LevelError
	LevelFatal
)

var levelNames = map[Level]string{
	LevelDebug: "DEBUG",
	LevelInfo:  "INFO",
	LevelWarn:  "WARN",
	LevelError: "ERROR",
	LevelFatal: "FATAL",
}

var levelColors = map[Level]string{
	LevelDebug: "\033[36m", // Cyan
	LevelInfo:  "\033[32m", // Green
	LevelWarn:  "\033[33m", // Yellow
	LevelError: "\033[31m", // Red
	LevelFatal: "\033[35m", // Magenta
}

const colorReset = "\033[0m"

// Logger is the main logging structure.
type Logger struct {
	level     Level
	out       io.Writer
	file      *os.File
	mu        sync.Mutex
	useColor  bool
	timestamp bool
	hooks     []func(level Level, msg string)
	// Rotation settings
	rotateSize  int64           // max size in bytes before rotation (0 = no rotation)
	rotateAge   time.Duration   // max age before rotation (0 = no rotation)
	rotateCount int             // number of rotated files to keep
	filePath    string
	currentSize int64
}

// Config for initializing the logger.
type Config struct {
	Level        string        // "debug", "info", "warn", "error"
	LogFile      string        // path to log file (empty = stdout only)
	UseColor     bool          // enable ANSI colors
	Timestamp    bool          // include timestamps
	RotateSize   int64         // max file size in MB (0 = no rotation)
	RotateAge    time.Duration // max age of log file (0 = no rotation)
	RotateCount  int           // number of rotated files to keep (0 = keep all)
}

var (
	defaultLogger *Logger
	once          sync.Once
	pendingHooks  []func(level Level, msg string)
)

// Init initializes the default logger with the given config.
func Init(cfg Config) {
	once.Do(func() {
		lvl := LevelInfo
		switch cfg.Level {
		case "debug":
			lvl = LevelDebug
		case "warn":
			lvl = LevelWarn
		case "error":
			lvl = LevelError
		}

		var writers []io.Writer
		writers = append(writers, os.Stdout)

		var file *os.File
		if cfg.LogFile != "" {
			dir := filepath.Dir(cfg.LogFile)
			_ = os.MkdirAll(dir, 0755)
			f, err := os.OpenFile(cfg.LogFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
			if err == nil {
				file = f
				writers = append(writers, f)
			} else {
				fmt.Fprintf(os.Stderr, "Failed to open log file: %v\n", err)
			}
		}

		defaultLogger = &Logger{
			level:       lvl,
			out:         io.MultiWriter(writers...),
			file:        file,
			useColor:    cfg.UseColor,
			timestamp:   cfg.Timestamp,
			hooks:       make([]func(Level, string), 0),
			rotateSize:  cfg.RotateSize * 1024 * 1024, // MB to bytes
			rotateAge:   cfg.RotateAge,
			rotateCount: cfg.RotateCount,
			filePath:    cfg.LogFile,
		}

		// Apply pending hooks
		for _, hook := range pendingHooks {
			defaultLogger.hooks = append(defaultLogger.hooks, hook)
		}
		pendingHooks = nil

		// If rotation is enabled, start rotation goroutine.
		if defaultLogger.rotateAge > 0 {
			go defaultLogger.rotationDaemon()
		}
	})
}

// emit is the core logging function.
func emit(level Level, format string, args ...interface{}) {
	if defaultLogger == nil || level < defaultLogger.level {
		return
	}

	msg := fmt.Sprintf(format, args...)
	_, file, line, ok := runtime.Caller(2)
	if !ok {
		file = "unknown"
		line = 0
	}
	shortFile := filepath.Base(file)

	defaultLogger.mu.Lock()
	defer defaultLogger.mu.Unlock()

	// Format the log line
	var logMsg string
	if defaultLogger.timestamp {
		timestamp := time.Now().Format("2006-01-02 15:04:05.000")
		if defaultLogger.useColor {
			logMsg = fmt.Sprintf("%s %s[%s]%s %s:%d: %s\n",
				timestamp,
				levelColors[level],
				levelNames[level],
				colorReset,
				shortFile, line, msg)
		} else {
			logMsg = fmt.Sprintf("%s [%s] %s:%d: %s\n",
				timestamp, levelNames[level], shortFile, line, msg)
		}
	} else {
		if defaultLogger.useColor {
			logMsg = fmt.Sprintf("%s[%s]%s %s:%d: %s\n",
				levelColors[level],
				levelNames[level],
				colorReset,
				shortFile, line, msg)
		} else {
			logMsg = fmt.Sprintf("[%s] %s:%d: %s\n",
				levelNames[level], shortFile, line, msg)
		}
	}

	// Write to output
	written, _ := defaultLogger.out.Write([]byte(logMsg))
	defaultLogger.currentSize += int64(written)

	// Check rotation (size)
	if defaultLogger.rotateSize > 0 && defaultLogger.currentSize >= defaultLogger.rotateSize {
		defaultLogger.rotateNow()
	}

	// Execute hooks
	for _, hook := range defaultLogger.hooks {
		hook(level, msg) // hook expects (Level, string)
	}

	if level == LevelFatal {
		_ = defaultLogger.out.(io.Closer).Close()
		os.Exit(1)
	}
}

// rotationDaemon checks for age-based rotation periodically.
func (l *Logger) rotationDaemon() {
	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()
	for range ticker.C {
		l.mu.Lock()
		if l.filePath != "" && l.rotateAge > 0 {
			info, err := os.Stat(l.filePath)
			if err == nil && time.Since(info.ModTime()) > l.rotateAge {
				l.rotateNow()
			}
		}
		l.mu.Unlock()
	}
}

// rotateNow performs log rotation.
func (l *Logger) rotateNow() {
	if l.filePath == "" || l.file == nil {
		return
	}
	// Close current file
	l.file.Close()

	// Rotate files
	for i := l.rotateCount - 1; i >= 0; i-- {
		src := l.filePath
		if i > 0 {
			src = fmt.Sprintf("%s.%d", l.filePath, i)
		}
		dst := fmt.Sprintf("%s.%d", l.filePath, i+1)
		if i == 0 {
			// Current file is the source; move it to .1
			dst = fmt.Sprintf("%s.1", l.filePath)
			_ = os.Rename(l.filePath, dst)
		} else {
			_ = os.Rename(src, dst)
		}
	}
	// Remove excess files
	if l.rotateCount > 0 {
		for i := l.rotateCount; ; i++ {
			f := fmt.Sprintf("%s.%d", l.filePath, i+1)
			if _, err := os.Stat(f); os.IsNotExist(err) {
				break
			}
			_ = os.Remove(f)
		}
	}

	// Reopen file
	f, err := os.OpenFile(l.filePath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to reopen log file: %v\n", err)
		return
	}
	l.file = f
	l.out = io.MultiWriter(os.Stdout, f)
	l.currentSize = 0
}

// Public logging functions
func Debug(format string, args ...interface{}) { emit(LevelDebug, format, args...) }
func Info(format string, args ...interface{})  { emit(LevelInfo, format, args...) }
func Warn(format string, args ...interface{})  { emit(LevelWarn, format, args...) }
func Error(format string, args ...interface{}) { emit(LevelError, format, args...) }
func Fatal(format string, args ...interface{}) { emit(LevelFatal, format, args...) }

// SetLevel changes the log level after initialization.
func SetLevel(level Level) {
	if defaultLogger != nil {
		defaultLogger.mu.Lock()
		defaultLogger.level = level
		defaultLogger.mu.Unlock()
	}
}

// GetLevel returns the current log level.
func GetLevel() Level {
	if defaultLogger != nil {
		defaultLogger.mu.RLock()
		defer defaultLogger.mu.RUnlock()
		return defaultLogger.level
	}
	return LevelInfo
}

// AddHook registers a hook that receives (Level, msg).
func AddHook(hook func(level Level, msg string)) {
	if hook == nil {
		return
	}
	if defaultLogger == nil {
		pendingHooks = append(pendingHooks, hook)
		return
	}
	defaultLogger.mu.Lock()
	defer defaultLogger.mu.Unlock()
	defaultLogger.hooks = append(defaultLogger.hooks, hook)
}

// AddStringHook registers a hook that receives (level string, msg string).
// This is useful for compatibility with the utils package.
func AddStringHook(hook func(level string, msg string)) {
	if hook == nil {
		return
	}
	AddHook(func(l Level, msg string) {
		hook(levelNames[l], msg)
	})
}

// Close flushes and closes the log file.
func Close() {
	if defaultLogger != nil {
		defaultLogger.mu.Lock()
		defer defaultLogger.mu.Unlock()
		if defaultLogger.file != nil {
			_ = defaultLogger.file.Close()
			defaultLogger.file = nil
		}
	}
}