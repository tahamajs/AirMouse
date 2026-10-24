package errors

import (
	"errors"
	"strings"
	"testing"
)

func TestAppErrors(t *testing.T) {
	errNF := NewNotFound("device not found")
	if !IsNotFound(errNF) {
		t.Errorf("Expected IsNotFound(errNF) == true")
	}
	if !strings.Contains(errNF.Error(), "[NOT_FOUND]") {
		t.Errorf("Expected NOT_FOUND prefix, got: %s", errNF.Error())
	}

	errInput := NewInvalidInput("bad payload")
	if IsNotFound(errInput) {
		t.Errorf("Expected IsNotFound(errInput) == false")
	}
	if !strings.Contains(errInput.Error(), "[INVALID_INPUT]") {
		t.Errorf("Expected INVALID_INPUT prefix, got: %s", errInput.Error())
	}

	errAuth := NewUnauthorized("no token")
	if !strings.Contains(errAuth.Error(), "[UNAUTHORIZED]") {
		t.Errorf("Expected UNAUTHORIZED prefix, got: %s", errAuth.Error())
	}

	rawErr := errors.New("sql failed")
	errInternal := NewInternal("db error", rawErr)
	if !strings.Contains(errInternal.Error(), "sql failed") {
		t.Errorf("Expected internal error to wrap raw error: %s", errInternal.Error())
	}

	if IsNotFound(errors.New("generic error")) {
		t.Errorf("Expected IsNotFound to be false for generic error")
	}
}
