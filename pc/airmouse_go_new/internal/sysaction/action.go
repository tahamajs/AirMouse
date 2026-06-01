package sysaction

type Action string

const (
	ActionPlayPause     Action = "play_pause"
	ActionNextTrack     Action = "next_track"
	ActionPrevTrack     Action = "prev_track"
	ActionVolumeUp      Action = "volume_up"
	ActionVolumeDown    Action = "volume_down"
	ActionMute          Action = "mute"
	ActionStop          Action = "stop"
	ActionLeftClick     Action = "left_click"
	ActionRightClick    Action = "right_click"
	ActionDoubleClick   Action = "double_click"
	ActionScrollUp      Action = "scroll_up"
	ActionScrollDown    Action = "scroll_down"
	ActionLockScreen    Action = "lock_screen"
	ActionShowDesktop   Action = "show_desktop"
	ActionTaskView      Action = "task_view"
	ActionBrowserBack   Action = "browser_back"
	ActionBrowserForward Action = "browser_forward"
	ActionZoomIn        Action = "zoom_in"
	ActionZoomOut       Action = "zoom_out"
)

func Execute(a Action) {
	switch a {
	case ActionPlayPause:
		keyTap("media_play_pause")
	case ActionNextTrack:
		keyTap("media_next")
	case ActionPrevTrack:
		keyTap("media_prev")
	case ActionVolumeUp:
		keyTap("audio_vol_up")
	case ActionVolumeDown:
		keyTap("audio_vol_down")
	case ActionMute:
		keyTap("audio_mute")
	case ActionStop:
		keyTap("media_stop")
	case ActionLeftClick:
		mouseClick("left")
	case ActionRightClick:
		mouseClick("right")
	case ActionDoubleClick:
		mouseDoubleClick()
	case ActionScrollUp:
		mouseScroll(1)
	case ActionScrollDown:
		mouseScroll(-1)
	case ActionLockScreen:
		lockScreen()
	default:
		// other actions
	}
}

// platform‑specific functions implemented in _darwin.go, _linux.go, _windows.go
func keyTap(key string)    {}
func mouseClick(btn string) {}
func mouseDoubleClick()    {}
func mouseScroll(delta int) {}
func lockScreen()          {}