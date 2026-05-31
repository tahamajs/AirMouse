package repository

import (
	"airmouse-go/internal/domain/entity"
)

// ClientRepository defines operations for storing and retrieving connected clients.
type ClientRepository interface {
	// Add stores a new client.
	Add(client *entity.Client) error

	// Remove deletes a client by ID.
	Remove(id string) error

	// Get returns a client by ID, or nil if not found.
	Get(id string) (*entity.Client, error)

	// GetByName returns a client by its display name (case‑insensitive).
	GetByName(name string) (*entity.Client, error)

	// List returns all currently connected clients.
	List() ([]*entity.Client, error)

	// ListByStatus returns clients with a given status.
	ListByStatus(status entity.ClientStatus) ([]*entity.Client, error)

	// Count returns the total number of stored clients.
	Count() (int, error)

	// UpdateLastActive refreshes the last activity timestamp.
	UpdateLastActive(id string) error

	// UpdateHeartbeat updates the last heartbeat timestamp and optionally latency.
	UpdateHeartbeat(id string, latencyMs int) error

	// UpdateBytes increments sent/received byte counters.
	UpdateBytes(id string, sent, recv int64) error

	// UpdateStatus changes the client's status.
	UpdateStatus(id string, status entity.ClientStatus) error

	// PruneInactive removes clients that have been idle for longer than the given duration.
	PruneInactive(maxIdle time.Duration) (int, error)

	// Exists checks whether a client with the given ID exists.
	Exists(id string) (bool, error)
}