// Package sysaction provides system-level actions for gestures and commands.
package sysaction

import (
	"log"
)

// Action represents a system action.
type Action string

const (
	// Media Controls
	ActionPlayPause   Action = "play_pause"
	ActionNextTrack   Action = "next_track"
	ActionPrevTrack   Action = "prev_track"
	ActionStop        Action = "stop"

	// Volume Controls
	ActionVolumeUp   Action = "volume_up"
	ActionVolumeDown Action = "volume_down"
	ActionMute       Action = "mute"

	// Mouse Actions
	ActionLeftClick   Action = "left_click"
	ActionRightClick  Action = "right_click"
	ActionMiddleClick Action = "middle_click"
	ActionDoubleClick Action = "double_click"
	ActionScrollUp    Action = "scroll_up"
	ActionScrollDown  Action = "scroll_down"

	// System Controls
	ActionLockScreen   Action = "lock_screen"
	ActionShowDesktop  Action = "show_desktop"
	ActionTaskView     Action = "task_view"
	ActionSwitchWindow Action = "switch_window"

	// Browser Controls
	ActionBrowserBack    Action = "browser_back"
	ActionBrowserForward Action = "browser_forward"
	ActionBrowserRefresh Action = "browser_refresh"
	ActionBrowserHome    Action = "browser_home"

	// Zoom Controls
	ActionZoomIn    Action = "zoom_in"
	ActionZoomOut   Action = "zoom_out"
	ActionZoomReset Action = "zoom_reset"

	// Window Controls
	ActionMinimizeWindow Action = "minimize_window"
	ActionMaximizeWindow Action = "maximize_window"
	ActionCloseWindow    Action = "close_window"
	ActionAltTab         Action = "alt_tab"

	// Text Editing
	ActionCopy      Action = "copy"
	ActionCut       Action = "cut"
	ActionPaste     Action = "paste"
	ActionUndo      Action = "undo"
	ActionRedo      Action = "redo"
	ActionSelectAll Action = "select_all"

	// Navigation
	ActionPageUp   Action = "page_up"
	ActionPageDown Action = "page_down"
	ActionHome     Action = "home"
	ActionEnd      Action = "end"
	ActionDelete   Action = "delete"
	ActionBackspace Action = "backspace"
	ActionEnter    Action = "enter"
	ActionEscape   Action = "escape"
	ActionTab      Action = "tab"

	// Function Keys
	ActionF1  Action = "f1"
	ActionF2  Action = "f2"
	ActionF3  Action = "f3"
	ActionF4  Action = "f4"
	ActionF5  Action = "f5"
	ActionF6  Action = "f6"
	ActionF7  Action = "f7"
	ActionF8  Action = "f8"
	ActionF9  Action = "f9"
	ActionF10 Action = "f10"
	ActionF11 Action = "f11"
	ActionF12 Action = "f12"

	// Power Controls
	ActionSleep    Action = "sleep"
	ActionShutdown Action = "shutdown"
	ActionRestart  Action = "restart"
	ActionLogout   Action = "logout"
)

// ActionMap maps gesture names to system actions.
var ActionMap = map[string]Action{
	"ThumbsUp":     ActionPlayPause,
	"ThumbsDown":   ActionStop,
	"LeftSwipe":    ActionPrevTrack,
	"RightSwipe":   ActionNextTrack,
	"UpSwipe":      ActionVolumeUp,
	"DownSwipe":    ActionVolumeDown,
	"CircleCW":     ActionVolumeUp,
	"CircleCCW":    ActionVolumeDown,
	"Peace":        ActionLockScreen,
	"Fist":         ActionMute,
	"ZoomIn":       ActionZoomIn,
	"ZoomOut":      ActionZoomOut,
	"DoubleTap":    ActionDoubleClick,
	"LongPress":    ActionRightClick,
	"Shake":        ActionShowDesktop,
	"Pinch":        ActionZoomReset,
}

// Execute performs the given system action.
func Execute(a Action) {
	log.Printf("Executing action: %s", a)
	switch a {
	// Media Controls
	case ActionPlayPause:
		mediaKey("PlayPause")
	case ActionNextTrack:
		mediaKey("Next")
	case ActionPrevTrack:
		mediaKey("Previous")
	case ActionStop:
		mediaKey("Stop")

	// Volume Controls
	case ActionVolumeUp:
		volumeUp()
	case ActionVolumeDown:
		volumeDown()
	case ActionMute:
		volumeMute()

	// Mouse Actions
	case ActionLeftClick:
		mouseClick("left")
	case ActionRightClick:
		mouseClick("right")
	case ActionMiddleClick:
		mouseClick("middle")
	case ActionDoubleClick:
		mouseDoubleClick()
	case ActionScrollUp:
		mouseScroll(1)
	case ActionScrollDown:
		mouseScroll(-1)

	// System Controls
	case ActionLockScreen:
		lockScreen()
	case ActionShowDesktop:
		showDesktop()
	case ActionTaskView:
		taskView()
	case ActionSwitchWindow:
		switchWindow()

	// Browser Controls
	case ActionBrowserBack:
		keyCombination("browser_back", "")
	case ActionBrowserForward:
		keyCombination("browser_forward", "")
	case ActionBrowserRefresh:
		keyCombination("browser_refresh", "")
	case ActionBrowserHome:
		keyCombination("browser_home", "")

	// Zoom Controls
	case ActionZoomIn:
		zoomIn()
	case ActionZoomOut:
		zoomOut()
	case ActionZoomReset:
		zoomReset()

	// Window Controls
	case ActionMinimizeWindow:
		minimizeWindow()
	case ActionMaximizeWindow:
		maximizeWindow()
	case ActionCloseWindow:
		closeWindow()
	case ActionAltTab:
		altTab()

	// Text Editing
	case ActionCopy:
		keyCombination("copy", "")
	case ActionCut:
		keyCombination("cut", "")
	case ActionPaste:
		keyCombination("paste", "")
	case ActionUndo:
		keyCombination("undo", "")
	case ActionRedo:
		keyCombination("redo", "")
	case ActionSelectAll:
		keyCombination("select_all", "")

	// Navigation
	case ActionPageUp:
		keyTap("page_up")
	case ActionPageDown:
		keyTap("page_down")
	case ActionHome:
		keyTap("home")
	case ActionEnd:
		keyTap("end")
	case ActionDelete:
		keyTap("delete")
	case ActionBackspace:
		keyTap("backspace")
	case ActionEnter:
		keyTap("enter")
	case ActionEscape:
		keyTap("escape")
	case ActionTab:
		keyTap("tab")

	// Function Keys
	case ActionF1:
		keyTap("f1")
	case ActionF2:
		keyTap("f2")
	case ActionF3:
		keyTap("f3")
	case ActionF4:
		keyTap("f4")
	case ActionF5:
		keyTap("f5")
	case ActionF6:
		keyTap("f6")
	case ActionF7:
		keyTap("f7")
	case ActionF8:
		keyTap("f8")
	case ActionF9:
		keyTap("f9")
	case ActionF10:
		keyTap("f10")
	case ActionF11:
		keyTap("f11")
	case ActionF12:
		keyTap("f12")

	// Power Controls
	case ActionSleep:
		sleepSystem()
	case ActionShutdown:
		shutdownSystem()
	case ActionRestart:
		restartSystem()
	case ActionLogout:
		logoutSystem()

	default:
		log.Printf("Unknown action: %s", a)
	}
}

// ExecuteGesture executes an action based on a gesture name and confidence.
func ExecuteGesture(gesture string, confidence float64) {
	if confidence < 0.7 {
		log.Printf("Gesture confidence too low: %.2f", confidence)
		return
	}
	if action, exists := ActionMap[gesture]; exists {
		Execute(action)
		log.Printf("Gesture %s -> Action %s (confidence: %.2f)", gesture, action, confidence)
	} else {
		log.Printf("No mapping for gesture: %s", gesture)
	}
}

// GetAvailableActions returns all defined actions.
func GetAvailableActions() []Action {
	return []Action{
		ActionPlayPause, ActionNextTrack, ActionPrevTrack, ActionStop,
		ActionVolumeUp, ActionVolumeDown, ActionMute,
		ActionLeftClick, ActionRightClick, ActionMiddleClick, ActionDoubleClick,
		ActionScrollUp, ActionScrollDown,
		ActionLockScreen, ActionShowDesktop, ActionTaskView,
		ActionBrowserBack, ActionBrowserForward, ActionBrowserRefresh,
		ActionZoomIn, ActionZoomOut, ActionZoomReset,
		ActionMinimizeWindow, ActionMaximizeWindow, ActionCloseWindow,
		ActionCopy, ActionCut, ActionPaste, ActionUndo, ActionRedo, ActionSelectAll,
		ActionPageUp, ActionPageDown, ActionHome, ActionEnd, ActionDelete,
		ActionF1, ActionF2, ActionF3, ActionF4, ActionF5, ActionF6,
		ActionF7, ActionF8, ActionF9, ActionF10, ActionF11, ActionF12,
		ActionSleep, ActionShutdown, ActionRestart, ActionLogout,
	}
}

// GetActionDescription returns a human-readable description.
func GetActionDescription(a Action) string {
	descriptions := map[Action]string{
		ActionPlayPause:     "Play/Pause Media",
		ActionNextTrack:     "Next Track",
		ActionPrevTrack:     "Previous Track",
		ActionStop:          "Stop Media",
		ActionVolumeUp:      "Volume Up",
		ActionVolumeDown:    "Volume Down",
		ActionMute:          "Mute",
		ActionLeftClick:     "Left Click",
		ActionRightClick:    "Right Click",
		ActionMiddleClick:   "Middle Click",
		ActionDoubleClick:   "Double Click",
		ActionScrollUp:      "Scroll Up",
		ActionScrollDown:    "Scroll Down",
		ActionLockScreen:    "Lock Screen",
		ActionShowDesktop:   "Show Desktop",
		ActionTaskView:      "Task View",
		ActionBrowserBack:   "Browser Back",
		ActionBrowserForward: "Browser Forward",
		ActionBrowserRefresh: "Browser Refresh",
		ActionZoomIn:        "Zoom In",
		ActionZoomOut:       "Zoom Out",
		ActionZoomReset:     "Zoom Reset",
		ActionCopy:          "Copy",
		ActionCut:           "Cut",
		ActionPaste:         "Paste",
		ActionUndo:          "Undo",
		ActionRedo:          "Redo",
		ActionSelectAll:     "Select All",
		ActionSleep:         "Sleep",
		ActionShutdown:      "Shutdown",
		ActionRestart:       "Restart",
		ActionLogout:        "Logout",
	}
	if desc, ok := descriptions[a]; ok {
		return desc
	}
	return string(a)
}

// The following functions are platform-specific and defined in the respective
// *_linux.go, *_darwin.go, *_windows.go files.
var (
	mediaKey       func(key string)
	volumeUp       func()
	volumeDown     func()
	volumeMute     func()
	mouseClick     func(btn string)
	mouseDoubleClick func()
	mouseScroll    func(delta int)
	lockScreen     func()
	showDesktop    func()
	taskView       func()
	switchWindow   func()
	keyCombination func(key, modifier string)
	keyTap         func(key string)
	zoomIn         func()
	zoomOut        func()
	zoomReset      func()
	minimizeWindow func()
	maximizeWindow func()
	closeWindow    func()
	altTab         func()
	sleepSystem    func()
	shutdownSystem func()
	restartSystem  func()
	logoutSystem   func()
)