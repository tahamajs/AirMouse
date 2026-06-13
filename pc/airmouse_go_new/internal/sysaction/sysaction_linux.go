//go:build linux

package sysaction

import (
    "os/exec"
    "strings"
)

// Key codes for Linux (xdotool)
var keyMap = map[string]string{
    // Media Keys
    "PlayPause": "XF86AudioPlay",
    "Next":      "XF86AudioNext",
    "Previous":  "XF86AudioPrev",
    "Stop":      "XF86AudioStop",
    
    // Volume Keys
    "VolumeUp":   "XF86AudioRaiseVolume",
    "VolumeDown": "XF86AudioLowerVolume",
    "Mute":       "XF86AudioMute",
    
    // Browser Keys
    "browser_back":     "Alt+Left",
    "browser_forward":  "Alt+Right",
    "browser_refresh":  "Ctrl+R",
    "browser_home":     "Alt+Home",
    
    // Navigation
    "page_up":    "Page_Up",
    "page_down":  "Page_Down",
    "home":       "Home",
    "end":        "End",
    "delete":     "Delete",
    "backspace":  "BackSpace",
    "enter":      "Return",
    "escape":     "Escape",
    "tab":        "Tab",
    
    // Function Keys
    "f1":  "F1",
    "f2":  "F2",
    "f3":  "F3",
    "f4":  "F4",
    "f5":  "F5",
    "f6":  "F6",
    "f7":  "F7",
    "f8":  "F8",
    "f9":  "F9",
    "f10": "F10",
    "f11": "F11",
    "f12": "F12",
}

func mediaKey(key string) {
    if keyCode, ok := keyMap[key]; ok {
        exec.Command("xdotool", "key", keyCode).Run()
    }
}

func volumeUp() {
    exec.Command("amixer", "set", "Master", "5%+").Run()
}

func volumeDown() {
    exec.Command("amixer", "set", "Master", "5%-").Run()
}

func volumeMute() {
    exec.Command("amixer", "set", "Master", "toggle").Run()
}

func keyTap(key string) {
    if keyCode, ok := keyMap[key]; ok {
        exec.Command("xdotool", "key", keyCode).Run()
    }
}

func keyCombination(key, modifier string) {
    var combo string
    
    switch key {
    case "copy":
        combo = "ctrl+c"
    case "cut":
        combo = "ctrl+x"
    case "paste":
        combo = "ctrl+v"
    case "undo":
        combo = "ctrl+z"
    case "redo":
        combo = "ctrl+shift+z"
    case "select_all":
        combo = "ctrl+a"
    default:
        if keyCode, ok := keyMap[key]; ok {
            combo = keyCode
        }
    }
    
    if combo != "" {
        exec.Command("xdotool", "key", combo).Run()
    }
}

func mouseClick(btn string) {
    var button int
    switch btn {
    case "left":
        button = 1
    case "right":
        button = 3
    case "middle":
        button = 2
    default:
        return
    }
    
    exec.Command("xdotool", "click", "click", button).Run()
}

func mouseDoubleClick() {
    exec.Command("xdotool", "click", "--repeat", "2", "1").Run()
}

func mouseScroll(delta int) {
    if delta > 0 {
        exec.Command("xdotool", "click", "4").Run() // Scroll up
    } else {
        exec.Command("xdotool", "click", "5").Run() // Scroll down
    }
}

func lockScreen() {
    // Try different lock commands
    commands := [][]string{
        {"loginctl", "lock-session"},
        {"gnome-screensaver-command", "-l"},
        {"xdg-screensaver", "lock"},
        {"dm-tool", "lock"},
        {"gdmflexiserver", "--lock"},
    }
    
    for _, cmd := range commands {
        if err := exec.Command(cmd[0], cmd[1:]...).Run(); err == nil {
            return
        }
    }
}

func showDesktop() {
    exec.Command("xdotool", "key", "ctrl+alt+d").Run()
}

func taskView() {
    exec.Command("xdotool", "key", "super+tab").Run()
}

func switchWindow() {
    exec.Command("xdotool", "key", "alt+tab").Run()
}

func minimizeWindow() {
    exec.Command("xdotool", "key", "alt+F9").Run()
}

func maximizeWindow() {
    exec.Command("xdotool", "key", "alt+F10").Run()
}

func closeWindow() {
    exec.Command("xdotool", "key", "alt+F4").Run()
}

func altTab() {
    exec.Command("xdotool", "key", "alt+Tab").Run()
}

func zoomIn() {
    exec.Command("xdotool", "key", "ctrl+plus").Run()
}

func zoomOut() {
    exec.Command("xdotool", "key", "ctrl+minus").Run()
}

func zoomReset() {
    exec.Command("xdotool", "key", "ctrl+0").Run()
}

func sleepSystem() {
    exec.Command("systemctl", "suspend").Run()
}

func shutdownSystem() {
    exec.Command("shutdown", "-h", "now").Run()
}

func restartSystem() {
    exec.Command("shutdown", "-r", "now").Run()
}

func logoutSystem() {
    exec.Command("gnome-session-quit", "--no-prompt").Run()
}