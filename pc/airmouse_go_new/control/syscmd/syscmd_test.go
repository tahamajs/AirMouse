package syscmd

import (
	"errors"
	"testing"
)

func TestExecuteSystemCommand(t *testing.T) {
	orig := execute
	defer func() { execute = orig }()

	calledCmd := ""
	execute = func(cmd string) error {
		calledCmd = cmd
		if cmd == "fail_cmd" {
			return errors.New("command failed")
		}
		return nil
	}

	err := ExecuteSystemCommand("volume_up")
	if err != nil {
		t.Fatalf("Unexpected error: %v", err)
	}
	if calledCmd != "volume_up" {
		t.Fatalf("Expected calledCmd 'volume_up', got %s", calledCmd)
	}

	err = ExecuteSystemCommand("fail_cmd")
	if err == nil {
		t.Fatal("Expected error for fail_cmd")
	}

	execute = nil
	if err := ExecuteSystemCommand("noop"); err != nil {
		t.Fatalf("Expected nil when execute is nil, got %v", err)
	}
}
