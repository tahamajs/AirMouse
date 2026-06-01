package repository

import (
	"sync/atomic"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/domain/repository"
	"airmouse-go/internal/infra/mouse"
)

type mouseRepositoryImpl struct {
	ctrl          mouse.MouseController
	stats         *entity.Statistics
	profile       *entity.MovementProfile
	clicks        int64
	doubleClicks  int64
	rightClicks   int64
	scrolls       int64
	totalMovement int64
	movementCount int64
	totalScroll   int64
}

func NewMouseRepository(ctrl mouse.MouseController) repository.MouseRepository {
	return &mouseRepositoryImpl{
		ctrl:    ctrl,
		stats:   entity.NewStatistics(),
		profile: entity.DefaultMovementProfile(),
	}
}

func (r *mouseRepositoryImpl) Move(dx, dy float64) error {
	r.ctrl.Move(dx, dy)
	atomic.AddInt64(&r.movementCount, 1)
	atomic.AddInt64(&r.totalMovement, int64(dx*dx+dy*dy)) // approximate
	return nil
}

func (r *mouseRepositoryImpl) MoveSmooth(points []entity.Point, durationMs int) error {
	// For brevity, call regular move for each point (in real code, implement interpolation)
	for _, p := range points {
		r.ctrl.Move(p.X, p.Y)
	}
	return nil
}

func (r *mouseRepositoryImpl) Click(button entity.MouseButton) error {
	r.ctrl.Click(string(button))
	atomic.AddInt64(&r.clicks, 1)
	return nil
}

func (r *mouseRepositoryImpl) DoubleClick() error {
	r.ctrl.DoubleClick()
	atomic.AddInt64(&r.doubleClicks, 1)
	return nil
}

func (r *mouseRepositoryImpl) ClickAt(x, y int, button entity.MouseButton) error {
	if err := r.SetPosition(x, y); err != nil {
		return err
	}
	return r.Click(button)
}

func (r *mouseRepositoryImpl) Scroll(delta int) error {
	r.ctrl.Scroll(delta)
	atomic.AddInt64(&r.scrolls, 1)
	atomic.AddInt64(&r.totalScroll, int64(delta))
	return nil
}

func (r *mouseRepositoryImpl) GetPosition() (int, int, error) {
	// In real implementation, call platform‑specific function
	return 0, 0, nil
}

func (r *mouseRepositoryImpl) SetPosition(x, y int) error {
	// Placeholder – real implementation would call OS API
	return nil
}

func (r *mouseRepositoryImpl) GetMovementProfile() (*entity.MovementProfile, error) {
	return r.profile, nil
}

func (r *mouseRepositoryImpl) SetMovementProfile(profile *entity.MovementProfile) error {
	r.profile = profile
	return nil
}

func (r *mouseRepositoryImpl) GetStatistics() (*entity.Statistics, error) {
	return &entity.Statistics{
		TotalMovement:    float64(atomic.LoadInt64(&r.totalMovement)),
		MovementCount:    atomic.LoadInt64(&r.movementCount),
		ClickCount:       atomic.LoadInt64(&r.clicks),
		DoubleClickCount: atomic.LoadInt64(&r.doubleClicks),
		RightClickCount:  atomic.LoadInt64(&r.rightClicks),
		ScrollCount:      atomic.LoadInt64(&r.scrolls),
		TotalScrollDelta: atomic.LoadInt64(&r.totalScroll),
	}, nil
}

func (r *mouseRepositoryImpl) ResetStatistics() error {
	atomic.StoreInt64(&r.clicks, 0)
	atomic.StoreInt64(&r.doubleClicks, 0)
	atomic.StoreInt64(&r.rightClicks, 0)
	atomic.StoreInt64(&r.scrolls, 0)
	atomic.StoreInt64(&r.totalMovement, 0)
	atomic.StoreInt64(&r.movementCount, 0)
	atomic.StoreInt64(&r.totalScroll, 0)
	return nil
}
