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

type Logger struct {
    level      Level
    out        io.Writer
    mu         sync.Mutex
    file       *os.File
    useColor   bool
    hooks      []func(level Level, msg string)
    timestamp  bool
}

var defaultLogger *Logger
var once sync.Once

type Config struct {
    Level     string
    LogFile   string
    UseColor  bool
    Timestamp bool
}

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

        if cfg.LogFile != "" {
            dir := filepath.Dir(cfg.LogFile)
            os.MkdirAll(dir, 0755)
            f, err := os.OpenFile(cfg.LogFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
            if err == nil {
                writers = append(writers, f)
                defaultLogger = &Logger{
                    level:     lvl,
                    out:       io.MultiWriter(writers...),
                    file:      f,
                    useColor:  cfg.UseColor,
                    timestamp: cfg.Timestamp,
                    hooks:     make([]func(level Level, msg string), 0),
                }
                return
            }
        }

        defaultLogger = &Logger{
            level:     lvl,
            out:       io.MultiWriter(writers...),
            useColor:  cfg.UseColor,
            timestamp: cfg.Timestamp,
            hooks:     make([]func(level Level, msg string), 0),
        }
    })
}

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

    defaultLogger.mu.Lock()
    defer defaultLogger.mu.Unlock()
    defaultLogger.out.Write([]byte(logMsg))

    for _, hook := range defaultLogger.hooks {
        go hook(level, msg)
    }

    if level == LevelFatal {
        os.Exit(1)
    }
}

func Debug(format string, args ...interface{}) { emit(LevelDebug, format, args...) }
func Info(format string, args ...interface{})  { emit(LevelInfo, format, args...) }
func Warn(format string, args ...interface{})  { emit(LevelWarn, format, args...) }
func Error(format string, args ...interface{}) { emit(LevelError, format, args...) }
func Fatal(format string, args ...interface{}) { emit(LevelFatal, format, args...) }

func SetLevel(level Level) {
    if defaultLogger != nil {
        defaultLogger.level = level
    }
}

func GetLevel() Level {
    if defaultLogger != nil {
        return defaultLogger.level
    }
    return LevelInfo
}

func AddHook(hook func(level Level, msg string)) {
    if defaultLogger != nil {
        defaultLogger.mu.Lock()
        defer defaultLogger.mu.Unlock()
        defaultLogger.hooks = append(defaultLogger.hooks, hook)
    }
}

func Close() {
    if defaultLogger != nil && defaultLogger.file != nil {
        defaultLogger.file.Close()
    }
}
