package errors

import "fmt"

type ErrorCode string

const (
	ErrNotFound     ErrorCode = "NOT_FOUND"
	ErrInvalidInput ErrorCode = "INVALID_INPUT"
	ErrUnauthorized ErrorCode = "UNAUTHORIZED"
	ErrInternal     ErrorCode = "INTERNAL_ERROR"
)

type AppError struct {
	Code    ErrorCode
	Message string
	Err     error
}

func (e *AppError) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("[%s] %s: %v", e.Code, e.Message, e.Err)
	}
	return fmt.Sprintf("[%s] %s", e.Code, e.Message)
}

func NewNotFound(message string) *AppError {
	return &AppError{Code: ErrNotFound, Message: message}
}

func NewInvalidInput(message string) *AppError {
	return &AppError{Code: ErrInvalidInput, Message: message}
}

func NewUnauthorized(message string) *AppError {
	return &AppError{Code: ErrUnauthorized, Message: message}
}

func NewInternal(message string, err error) *AppError {
	return &AppError{Code: ErrInternal, Message: message, Err: err}
}

func IsNotFound(err error) bool {
	if appErr, ok := err.(*AppError); ok {
		return appErr.Code == ErrNotFound
	}
	return false
}