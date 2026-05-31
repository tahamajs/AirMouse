package repository

import (
	"airmouse-go/internal/domain/entity"
)

// ClientRepository defines the interface for client management.
type ClientRepository interface {
	// Save adds or updates a client.
	Save(client *entity.Client) error

	// FindByID retrieves a client by its ID.
	FindByID(id string) (*entity.Client, error)

	// FindAll returns all connected clients.
	FindAll() ([]*entity.Client, error)

	// Remove deletes a client by ID.
	Remove(id string) error

	// UpdateLastActive updates the last active timestamp.
	UpdateLastActive(id string) error
}