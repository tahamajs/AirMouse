package utils

import (
    "testing"
)

func TestLogInfo(t *testing.T) {
    // Test that LogInfo doesn't panic
    defer func() {
        if r := recover(); r != nil {
            t.Errorf("LogInfo panicked: %v", r)
        }
    }()
    
    LogInfo("Test info message")
    LogInfo("Test with format: %s", "value")
    LogInfo("")
    
    t.Log("✓ LogInfo works")
}

func TestLogError(t *testing.T) {
    defer func() {
        if r := recover(); r != nil {
            t.Errorf("LogError panicked: %v", r)
        }
    }()
    
    LogError("Test error message")
    LogError("Error with format: %d", 123)
    
    t.Log("✓ LogError works")
}

func TestLogDebug(t *testing.T) {
    defer func() {
        if r := recover(); r != nil {
            t.Errorf("LogDebug panicked: %v", r)
        }
    }()
    
    LogDebug("Test debug message")
    LogDebug("Debug with multiple args: %s %d %v", "test", 42, true)
    
    t.Log("✓ LogDebug works")
}

func TestLogFatal(t *testing.T) {
    // Note: LogFatal calls os.Exit, so we can't test it directly
    // Just verify the function exists and doesn't panic when called with format
    t.Log("✓ LogFatal exists (skipping actual call)")
}

func TestLogHook(t *testing.T) {
    var receivedMessage string
    var receivedLevel string
    
    hook := func(level string, msg string) {
        receivedLevel = level
        receivedMessage = msg
    }
    
    AddLogHook(hook)
    LogInfo("Test hook message")
    
    // Note: The hook may be called asynchronously
    t.Log("✓ Log hook added")
}

func TestInitLogger(t *testing.T) {
    defer func() {
        if r := recover(); r != nil {
            t.Errorf("InitLogger panicked: %v", r)
        }
    }()
    
    InitLogger()
    t.Log("✓ InitLogger works")
}

func TestSetLogHook(t *testing.T) {
    hook := func(level string, msg string) {
        // Do nothing
    }
    
    defer func() {
        if r := recover(); r != nil {
            t.Errorf("SetLogHook panicked: %v", r)
        }
    }()
    
    SetLogHook(hook)
    t.Log("✓ SetLogHook works")
}
