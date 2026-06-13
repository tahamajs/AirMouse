package repository

import (
    "time"

    "airmouse-go/internal/domain/entity"
)

// ClientRepository defines operations for storing and retrieving connected clients
type ClientRepository interface {
    Add(client *entity.Client) error
    Remove(id string) error
    Get(id string) (*entity.Client, error)
    GetByName(name string) (*entity.Client, error)
    List() ([]*entity.Client, error)
    ListByStatus(status entity.ClientStatus) ([]*entity.Client, error)
    ListActive() ([]*entity.Client, error)
    Count() (int, error)
    CountActive() (int, error)
    UpdateLastActive(id string) error
    UpdateHeartbeat(id string, latencyMs int) error
    UpdateBytes(id string, sent, recv int64) error
    UpdateStatus(id string, status entity.ClientStatus) error
    UpdateCapabilities(id string, caps entity.ClientCapabilities) error
    PruneInactive(maxIdle time.Duration) (int, error)
    Exists(id string) (bool, error)
    BlockDevice(id string) error
    IsBlocked(id string) bool
}

// GestureRepository manages gesture templates
type GestureRepository interface {
    SaveTemplate(template *entity.GestureTemplate) error
    GetTemplate(idOrName string) (*entity.GestureTemplate, error)
    ListTemplates(filterType entity.GestureType) ([]*entity.GestureTemplate, error)
    ListAllTemplates() ([]*entity.GestureTemplate, error)
    DeleteTemplate(id string) error
    DeleteAllByType(gestureType entity.GestureType) (int, error)
    TemplateExists(name string) (bool, error)
    Count() (int, error)
    CountByType() (map[entity.GestureType]int, error)
    UpdateMetadata(id string, metadata map[string]interface{}) error
    IncrementUsage(id string, score float64) error
    SearchTemplates(query string) ([]*entity.GestureTemplate, error)
}

// MouseRepository abstracts the actual mouse control hardware
type MouseRepository interface {
    Move(dx, dy float64) error
    MoveSmooth(points []entity.Point, durationMs int) error
    Click(button entity.MouseButton) error
    DoubleClick() error
    ClickAt(x, y int, button entity.MouseButton) error
    Scroll(delta int) error
    GetPosition() (x, y int, err error)
    SetPosition(x, y int) error
    GetMovementProfile() (*entity.MovementProfile, error)
    SetMovementProfile(profile *entity.MovementProfile) error
    GetStatistics() (*entity.Statistics, error)
    ResetStatistics() error
}v