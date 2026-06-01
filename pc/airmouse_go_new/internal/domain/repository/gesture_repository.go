package repository

import (
	"airmouse-go/internal/domain/entity"
)

// GestureRepository manages gesture templates.
type GestureRepository interface {
	// SaveTemplate stores a new or updates an existing gesture template.
	SaveTemplate(template *entity.GestureTemplate) error

	// GetTemplate retrieves a template by its unique ID or name.
	GetTemplate(idOrName string) (*entity.GestureTemplate, error)

	// ListTemplates returns all stored gesture templates, optionally filtered by type.
	ListTemplates(filterType entity.GestureType) ([]*entity.GestureTemplate, error)

	// DeleteTemplate removes a template by ID.
	DeleteTemplate(id string) error

	// DeleteAllByType removes all templates of a given type.
	DeleteAllByType(gestureType entity.GestureType) (int, error)

	// TemplateExists checks whether a template with the given name exists.
	TemplateExists(name string) (bool, error)

	// Count returns the total number of templates.
	Count() (int, error)

	// UpdateMetadata merges key‑value pairs into the template’s metadata.
	UpdateMetadata(id string, metadata map[string]interface{}) error
}
