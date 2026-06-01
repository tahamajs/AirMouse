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
)

var levelNames = map[Level]string{
	LevelDebug: "DEBUG",
	LevelInfo:  "INFO",
	LevelWarn:  "WARN",
	LevelError: "ERROR",
}

type Logger struct {
	level Level
	out   io.Writer
	mu    sync.Mutex
	file  *os.File
}

var defaultLogger *Logger
var once sync.Once

func Init(level string, logFile string) {
	once.Do(func() {
		lvl := LevelInfo
		switch level {
		case "debug":
			lvl = LevelDebug
		case "warn":
			lvl = LevelWarn
		case "error":
			lvl = LevelError
		}
		var writer io.Writer = os.Stdout
		if logFile != "" {
			dir := filepath.Dir(logFile)
			_ = os.MkdirAll(dir, 0755)
			f, err := os.OpenFile(logFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
			if err == nil {
				writer = io.MultiWriter(os.Stdout, f)
				defaultLogger = &Logger{level: lvl, out: writer, file: f}
				return
			}
		}
		defaultLogger = &Logger{level: lvl, out: writer}
	})
}

func log(level Level, format string, args ...interface{}) {
	if defaultLogger == nil || level < defaultLogger.level {
		return
	}
	msg := fmt.Sprintf(format, args...)
	_, file, line, _ := runtime.Caller(2)
	shortFile := filepath.Base(file)
	timestamp := time.Now().Format("2006-01-02 15:04:05.000")
	lineMsg := fmt.Sprintf("%s [%s] %s:%d: %s\n", timestamp, levelNames[level], shortFile, line, msg)
	defaultLogger.mu.Lock()
	defer defaultLogger.mu.Unlock()
	_, _ = defaultLogger.out.Write([]byte(lineMsg))
}

func Debug(format string, args ...interface{}) { log(LevelDebug, format, args...) }
func Info(format string, args ...interface{})  { log(LevelInfo, format, args...) }
func Warn(format string, args ...interface{})  { log(LevelWarn, format, args...) }
func Error(format string, args ...interface{}) { log(LevelError, format, args...) }

func Close() {
	if defaultLogger != nil && defaultLogger.file != nil {
		defaultLogger.file.Close()
	}
}
