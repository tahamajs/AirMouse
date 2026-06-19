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

// Backward-compatible wrappers used by older tests and call sites.
func InitLogger() {
	logger.Init(logger.Config{})
}

func AddLogHook(hook func(level string, msg string)) {
	logger.AddHook(func(level logger.Level, msg string) {
		hook(levelString(level), msg)
	})
}

func SetLogHook(hook func(level string, msg string)) {
	AddLogHook(hook)
}

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
