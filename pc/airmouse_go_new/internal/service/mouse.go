package service

import (
    "math"
    "sync"
    "time"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/infra/logger"
)

// MouseService implements advanced mouse control logic
type MouseService interface {
    Move(dx, dy float64) error
    MoveRaw(dx, dy float64) error
    Click(button entity.MouseButton) error
    DoubleClick() error
    RightClick() error
    Scroll(delta int) error
    GetStats() (*entity.Statistics, error)
    ResetStats() error
    SetSensitivity(sensitivity float64)
    SetProfile(profile *entity.MovementProfile) error
    GetProfile() (*entity.MovementProfile, error)
    GetPosition() (x, y int, err error)
    SetPosition(x, y int) error
    Pause(seconds int) error
    IsPaused() bool
}

type mouseService struct {
    repo         repository.MouseRepository
    profile      *entity.MovementProfile
    lastX, lastY float64
    pausedUntil  time.Time
    mu           sync.RWMutex
}

func NewMouseService(repo repository.MouseRepository, initialProfile *entity.MovementProfile) MouseService {
    if initialProfile == nil {
        initialProfile = entity.DefaultMovementProfile()
    }
    return &mouseService{
        repo:    repo,
        profile: initialProfile,
    }
}

func (s *mouseService) Move(dx, dy float64) error {
    s.mu.RLock()
    if time.Now().Before(s.pausedUntil) {
        s.mu.RUnlock()
        return nil
    }
    s.mu.RUnlock()
    
    // Apply profile transformations
    if s.profile.Deadband > 0 {
        if math.Abs(dx) < s.profile.Deadband {
            dx = 0
        }
        if math.Abs(dy) < s.profile.Deadband {
            dy = 0
        }
    }
    
    if dx == 0 && dy == 0 {
        return nil
    }
    
    // Apply smoothing (EMA)
    s.mu.Lock()
    alpha := s.profile.SmoothingAlpha
    if alpha <= 0 {
        alpha = 0.3
    }
    s.lastX = alpha*dx + (1-alpha)*s.lastX
    s.lastY = alpha*dy + (1-alpha)*s.lastY
    smX, smY := s.lastX, s.lastY
    s.mu.Unlock()
    
    return s.repo.Move(smX, smY)
}

func (s *mouseService) MoveRaw(dx, dy float64) error {
    return s.repo.Move(dx, dy)
}

func (s *mouseService) Click(button entity.MouseButton) error {
    return s.repo.Click(button)
}

func (s *mouseService) DoubleClick() error {
    return s.repo.DoubleClick()
}

func (s *mouseService) RightClick() error {
    return s.repo.Click(entity.RightButton)
}

func (s *mouseService) Scroll(delta int) error {
    return s.repo.Scroll(delta)
}

func (s *mouseService) GetStats() (*entity.Statistics, error) {
    return s.repo.GetStatistics()
}

func (s *mouseService) ResetStats() error {
    return s.repo.ResetStatistics()
}

func (s *mouseService) SetSensitivity(sensitivity float64) {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if sensitivity < 0.1 {
        sensitivity = 0.1
    }
    if sensitivity > 3.0 {
        sensitivity = 3.0
    }
    s.profile.Sensitivity = sensitivity
    s.repo.SetMovementProfile(s.profile)
    
    logger.Debug("Mouse sensitivity set to %.2f", sensitivity)
}

func (s *mouseService) SetProfile(profile *entity.MovementProfile) error {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    s.profile = profile
    return s.repo.SetMovementProfile(profile)
}

func (s *mouseService) GetProfile() (*entity.MovementProfile, error) {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return s.profile, nil
}

func (s *mouseService) GetPosition() (x, y int, err error) {
    return s.repo.GetPosition()
}

func (s *mouseService) SetPosition(x, y int) error {
    return s.repo.SetPosition(x, y)
}

func (s *mouseService) Pause(seconds int) error {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if seconds <= 0 {
        s.pausedUntil = time.Time{}
        logger.Info("Mouse movement resumed")
    } else {
        s.pausedUntil = time.Now().Add(time.Duration(seconds) * time.Second)
        logger.Info("Mouse movement paused for %d seconds", seconds)
    }
    return nil
}

func (s *mouseService) IsPaused() bool {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return time.Now().Before(s.pausedUntil)
}
