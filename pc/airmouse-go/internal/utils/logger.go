package utils

import (
	"fmt"
	"log"
	"os"
	"sync"
)

type LogHook func(level, message string)

var (
	mu     sync.RWMutex
	hooks  []LogHook
	logger *log.Logger
)

func InitLogger() {
	logger = log.New(os.Stdout, "", log.LstdFlags)
}

func AddLogHook(hook LogHook) {
	mu.Lock()
	defer mu.Unlock()
	hooks = append(hooks, hook)
}

func SetLogHook(hook LogHook) {
	mu.Lock()
	defer mu.Unlock()
	hooks = []LogHook{hook}
}

func logMessage(level, format string, args ...interface{}) {
	msg := fmt.Sprintf(format, args...)
	if logger != nil {
		logger.Printf("[%s] %s", level, msg)
	}
	mu.RLock()
	defer mu.RUnlock()
	for _, hook := range hooks {
		go hook(level, msg)
	}
}

func LogInfo(format string, args ...interface{}) {
	logMessage("INFO", format, args...)
}

func LogError(format string, args ...interface{}) {
	logMessage("ERROR", format, args...)
}

func LogDebug(format string, args ...interface{}) {
	logMessage("DEBUG", format, args...)
}

func LogWarn(format string, args ...interface{}) {
	logMessage("WARN", format, args...)
}

func LogFatal(format string, args ...interface{}) {
	logMessage("FATAL", format, args...)
	os.Exit(1)
}