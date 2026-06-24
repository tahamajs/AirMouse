package utils

import "airmouse-go/internal/logger"

func LogInfo(msg string, args ...interface{}) {
	logger.Info(msg, args...)
}

func LogDebug(msg string, args ...interface{}) {
	logger.Debug(msg, args...)
}

func LogError(msg string, args ...interface{}) {
	logger.Error(msg, args...)
}

func LogWarn(msg string, args ...interface{}) {
	logger.Warn(msg, args...)
}

// InitLogger initialises the logger with default settings.
func InitLogger() {
	logger.Init(logger.Config{})
}

// AddLogHook registers a hook that receives log entries as (level, message).
func AddLogHook(hook func(level string, msg string)) {
	logger.AddHook(func(level logger.Level, msg string) {
		hook(levelString(level), msg)
	})
}

// SetLogHook is an alias for AddLogHook.
func SetLogHook(hook func(level string, msg string)) {
	AddLogHook(hook)
}

// levelString converts a logger.Level to its uppercase string representation.
func levelString(level logger.Level) string {
	switch level {
	case logger.LevelDebug:
		return "DEBUG"
	case logger.LevelInfo:
		return "INFO"
	case logger.LevelWarn:
		return "WARN"
	case logger.LevelError:
		return "ERROR"
	default:
		return "FATAL"
	}
}