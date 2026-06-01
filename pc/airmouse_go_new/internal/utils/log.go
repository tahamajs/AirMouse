package utils

import (
    "fmt"
    "os"

    "airmouse-go/internal/infra/logger"
    pkgutils "airmouse-go/internal/pkg/utils"
)

// InitLogger initializes the shared logger using environment defaults.
func InitLogger() {
    level := os.Getenv("AIRMOUSE_LOG_LEVEL")
    if level == "" {
        level = "info"
    }
    logger.Init(level, os.Getenv("AIRMOUSE_LOG_FILE"))
}

// LogInfo accepts a message and optional key/value pairs and forwards to infra logger.
func LogInfo(msg string, kv ...interface{}) {
    if len(kv) > 0 {
        // format key/value pairs
        for i := 0; i+1 < len(kv); i += 2 {
            msg = fmt.Sprintf("%s %v=%v", msg, kv[i], kv[i+1])
        }
        if len(kv)%2 == 1 {
            msg = fmt.Sprintf("%s %v", msg, kv[len(kv)-1])
        }
    }
    logger.Info("%s", msg)
}

func LogDebug(msg string, kv ...interface{}) {
    if len(kv) > 0 {
        for i := 0; i+1 < len(kv); i += 2 {
            msg = fmt.Sprintf("%s %v=%v", msg, kv[i], kv[i+1])
        }
        if len(kv)%2 == 1 {
            msg = fmt.Sprintf("%s %v", msg, kv[len(kv)-1])
        }
    }
    logger.Debug("%s", msg)
}

func LogError(msg string, kv ...interface{}) {
    if len(kv) > 0 {
        for i := 0; i+1 < len(kv); i += 2 {
            msg = fmt.Sprintf("%s %v=%v", msg, kv[i], kv[i+1])
        }
        if len(kv)%2 == 1 {
            msg = fmt.Sprintf("%s %v", msg, kv[len(kv)-1])
        }
    }
    logger.Error("%s", msg)
}

func LogFatal(msg string, kv ...interface{}) {
    LogError(msg, kv...)
    os.Exit(1)
}

// GenerateID proxies to pkg/utils.GenerateID for backward compatibility.
func GenerateID() string {
    return pkgutils.GenerateID()
}
