package sysaction

import (
	"log"
	"strings"
)

// Action represents a system action that can be executed.
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

var actionMap = map[string]Action{
	"play_pause":   ActionPlayPause,
	"next_track":   ActionNextTrack,
	"prev_track":   ActionPrevTrack,
	"volume_up":    ActionVolumeUp,
	"volume_down":  ActionVolumeDown,
	"mute":         ActionMute,
	"stop":         ActionStop,
	"left_click":   ActionLeftClick,
	"right_click":  ActionRightClick,
	"double_click": ActionDoubleClick,
	"scroll_up":    ActionScrollUp,
	"scroll_down":  ActionScrollDown,
	"lock_screen":  ActionLockScreen,
	"show_desktop": ActionShowDesktop,
	"task_view":    ActionTaskView,
	"browser_back": ActionBrowserBack,
	"browser_forward": ActionBrowserForward,
	"zoom_in":      ActionZoomIn,
	"zoom_out":     ActionZoomOut,
}

// ParseAction converts a string to an Action.
func ParseAction(s string) (Action, bool) {
	a, ok := actionMap[strings.ToLower(s)]
	return a, ok
}

// Execute runs the given system action.
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
	case ActionShowDesktop:
		keyTap("show_desktop")
	case ActionTaskView:
		keyTap("task_view")
	case ActionBrowserBack:
		keyTap("browser_back")
	case ActionBrowserForward:
		keyTap("browser_forward")
	case ActionZoomIn:
		keyTap("zoom_in")
	case ActionZoomOut:
		keyTap("zoom_out")
	default:
		log.Printf("Unknown action: %s", a)
	}
}

// These functions are implemented in platform‑specific files.
func keyTap(key string)    {}
func mouseClick(btn string) {}
func mouseDoubleClick()    {}
func mouseScroll(delta int) {}
func lockScreen()          {}