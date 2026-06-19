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
