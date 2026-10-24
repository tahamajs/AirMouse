package logger

import (
	"bytes"
	"strings"
	"testing"
)

func TestLoggerLevelsAndOutput(t *testing.T) {
	var buf bytes.Buffer
	l := New(Config{
		Level:     "debug",
		UseColor:  false,
		Timestamp: false,
		Output:    &buf,
	})

	l.Debug("debug message %d", 1)
	l.Info("info message %s", "two")
	l.Warn("warn message")
	l.Error("error message")

	out := buf.String()
	if !strings.Contains(out, "[DEBUG]") || !strings.Contains(out, "debug message 1") {
		t.Errorf("Expected debug message in output, got: %s", out)
	}
	if !strings.Contains(out, "[INFO]") || !strings.Contains(out, "info message two") {
		t.Errorf("Expected info message in output, got: %s", out)
	}
	if !strings.Contains(out, "[WARN]") || !strings.Contains(out, "warn message") {
		t.Errorf("Expected warn message in output, got: %s", out)
	}
	if !strings.Contains(out, "[ERROR]") || !strings.Contains(out, "error message") {
		t.Errorf("Expected error message in output, got: %s", out)
	}
}

func TestLoggerFiltering(t *testing.T) {
	var buf bytes.Buffer
	l := New(Config{
		Level:     "warn",
		UseColor:  false,
		Timestamp: false,
		Output:    &buf,
	})

	l.Debug("should not appear")
	l.Info("should not appear either")
	l.Warn("warning appears")

	out := buf.String()
	if strings.Contains(out, "should not appear") {
		t.Errorf("Debug/Info should have been filtered out: %s", out)
	}
	if !strings.Contains(out, "warning appears") {
		t.Errorf("Expected warning in output: %s", out)
	}
}

func TestLoggerHooks(t *testing.T) {
	var buf bytes.Buffer
	l := New(Config{
		Level:  "info",
		Output: &buf,
	})

	hookTriggered := false
	l.AddHook(func(level Level, msg string) {
		if level == LevelInfo && strings.Contains(msg, "hook test") {
			hookTriggered = true
		}
	})

	l.Info("hook test message")
	if !hookTriggered {
		t.Errorf("Expected hook to be triggered")
	}
}
