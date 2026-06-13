package utils

import (
	"log"
	"os"
)

var logger = log.New(os.Stdout, "", log.LstdFlags)

func LogInfo(format string, args ...interface{}) {
	if len(args) > 0 {
		logger.Printf("INFO: "+format, args...)
	} else {
		logger.Print("INFO: " + format)
	}
}

func LogError(format string, args ...interface{}) {
	if len(args) > 0 {
		logger.Printf("ERROR: "+format, args...)
	} else {
		logger.Print("ERROR: " + format)
	}
}

func LogDebug(format string, args ...interface{}) {
	if len(args) > 0 {
		logger.Printf("DEBUG: "+format, args...)
	} else {
		logger.Print("DEBUG: " + format)
	}
}

func LogFatal(format string, args ...interface{}) {
	if len(args) > 0 {
		logger.Fatalf("FATAL: "+format, args...)
	} else {
		logger.Fatal("FATAL: " + format)
	}
}

// Keep these for compatibility with existing code
type LogHook func(level string, msg string)

var hooks []LogHook

func InitLogger() {}
func AddLogHook(hook LogHook) { hooks = append(hooks, hook) }
func SetLogHook(hook LogHook) { hooks = []LogHook{hook} }
