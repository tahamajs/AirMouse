//go:build darwin

package sysaction

import (
    "os/exec"
    "strings"
)

// Key codes for macOS
var keyCodeMap = map[string]string{
    // Media Keys
    "PlayPause":  "100",
    "Next":       "101",
    "Previous":   "102",
    "Stop":       "103",
    
    // Volume Keys
    "VolumeUp":   "107",
    "VolumeDown": "108",
    "Mute":       "109",
    
    // Browser Keys
    "browser_back":     "115",
    "browser_forward":  "116",
    "browser_refresh":  "15",
    "browser_home":     "115",
    
    // Navigation
    "page_up":     "116",
    "page_down":   "121",
    "home":        "115",
    "end":         "119",
    "delete":      "117",
    "backspace":   "51",
    "enter":       "36",
    "escape":      "53",
    "tab":         "48",
    
    // Function Keys
    "f1":          "122",
    "f2":          "120",
    "f3":          "99",
    "f4":          "118",
    "f5":          "96",
    "f6":          "97",
    "f7":          "98",
    "f8":          "100",
    "f9":          "101",
    "f10":         "109",
    "f11":         "103",
    "f12":         "111",
}

func mediaKey(key string) {
    if keyCode, ok := keyCodeMap[key]; ok {
        script := fmt.Sprintf(`tell application "System Events" to key code %s`, keyCode)
        exec.Command("osascript", "-e", script).Run()
    }
}

func volumeUp() {
    script := `set volume output volume (output volume of (get volume settings) + 10)`
    exec.Command("osascript", "-e", script).Run()
}

func volumeDown() {
    script := `set volume output volume (output volume of (get volume settings) - 10)`
    exec.Command("osascript", "-e", script).Run()
}

func volumeMute() {
    script := `set volume output muted not (output muted of (get volume settings))`
    exec.Command("osascript", "-e", script).Run()
}

func keyTap(key string) {
    if keyCode, ok := keyCodeMap[key]; ok {
        script := fmt.Sprintf(`tell application "System Events" to key code %s`, keyCode)
        exec.Command("osascript", "-e", script).Run()
    }
}

func keyCombination(key, modifier string) {
    keyCode, ok := keyCodeMap[key]
    if !ok {
        return
    }
    
    var script string
    switch key {
    case "copy":
        script = `tell application "System Events" to keystroke "c" using command down`
    case "cut":
        script = `tell application "System Events" to keystroke "x" using command down`
    case "paste":
        script = `tell application "System Events" to keystroke "v" using command down`
    case "undo":
        script = `tell application "System Events" to keystroke "z" using command down`
    case "redo":
        script = `tell application "System Events" to keystroke "z" using {command down, shift down}`
    case "select_all":
        script = `tell application "System Events" to keystroke "a" using command down`
    default:
        script = fmt.Sprintf(`tell application "System Events" to key code %s`, keyCode)
    }
    
    exec.Command("osascript", "-e", script).Run()
}

func mouseClick(btn string) {
    var buttonNum string
    switch btn {
    case "left":
        buttonNum = "1"
    case "right":
        buttonNum = "2"
    case "middle":
        buttonNum = "3"
    default:
        return
    }
    
    script := fmt.Sprintf(`tell application "System Events" to click at {0, 0} button %s`, buttonNum)
    exec.Command("osascript", "-e", script).Run()
}

func mouseDoubleClick() {
    script := `tell application "System Events" to click at {0, 0} button 1 count 2`
    exec.Command("osascript", "-e", script).Run()
}

func mouseScroll(delta int) {
    script := fmt.Sprintf(`tell application "System Events" to keystroke (character number %d) using control down`, delta)
    exec.Command("osascript", "-e", script).Run()
}

func lockScreen() {
    // Method 1: CGSession
    cmd1 := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
    if err := cmd1.Run(); err != nil {
        // Method 2: pmset
        exec.Command("pmset", "displaysleepnow").Run()
    }
}

func showDesktop() {
    script := `tell application "Finder" to set collapsed of windows to true`
    exec.Command("osascript", "-e", script).Run()
}

func taskView() {
    // Mission Control
    script := `tell application "System Events" to key code 160 using control down`
    exec.Command("osascript", "-e", script).Run()
}

func switchWindow() {
    script := `tell application "System Events" to keystroke tab using command down`
    exec.Command("osascript", "-e", script).Run()
}

func minimizeWindow() {
    script := `tell application "System Events" to keystroke "m" using command down`
    exec.Command("osascript", "-e", script).Run()
}

func maximizeWindow() {
    script := `tell application "System Events" to click (first button whose subrole is "AXZoomButton") of front window of (first application process whose frontmost is true)`
    exec.Command("osascript", "-e", script).Run()
}

func closeWindow() {
    script := `tell application "System Events" to keystroke "w" using command down`
    exec.Command("osascript", "-e", script).Run()
}

func altTab() {
    script := `tell application "System Events" to keystroke tab using command down`
    exec.Command("osascript", "-e", script).Run()
}

func zoomIn() {
    script := `tell application "System Events" to keystroke "+" using command down`
    exec.Command("osascript", "-e", script).Run()
}

func zoomOut() {
    script := `tell application "System Events" to keystroke "-" using command down`
    exec.Command("osascript", "-e", script).Run()
}

func zoomReset() {
    script := `tell application "System Events" to keystroke "0" using command down`
    exec.Command("osascript", "-e", script).Run()
}

func sleepSystem() {
    exec.Command("pmset", "sleepnow").Run()
}

func shutdownSystem() {
    exec.Command("osascript", "-e", `tell app "System Events" to shut down`).Run()
}

func restartSystem() {
    exec.Command("osascript", "-e", `tell app "System Events" to restart`).Run()
}

func logoutSystem() {
    exec.Command("osascript", "-e", `tell app "System Events" to log out`).Run()
}