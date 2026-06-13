// internal/utils/logger.go
package utils

import (
    "log"
    "os"
)

var logger = log.New(os.Stdout, "", log.LstdFlags)

func InitLogger() {}

func LogInfo(msg string) {
    logger.Printf("[INFO] %s", msg)
}

func LogError(msg string) {
    logger.Printf("[ERROR] %s", msg)
}

func LogDebug(msg string) {
    logger.Printf("[DEBUG] %s", msg)
}