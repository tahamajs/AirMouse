//go:build linux

package sysaction

import (
    "fmt"
    "os/exec"
)

// DBus-based system actions for better integration
func mediaKeyDBus(key string) {
    var action string
    switch key {
    case "PlayPause":
        action = "Play"
    case "Next":
        action = "Next"
    case "Previous":
        action = "Previous"
    case "Stop":
        action = "Stop"
    default:
        return
    }
    
    cmd := exec.Command("dbus-send", "--print-reply", "--dest=org.mpris.MediaPlayer2.Player",
        "/org/mpris/MediaPlayer2", fmt.Sprintf("org.mpris.MediaPlayer2.Player.%s", action))
    cmd.Run()
}

func volumeUpDBus() {
    exec.Command("pactl", "set-sink-volume", "@DEFAULT_SINK@", "+5%").Run()
}

func volumeDownDBus() {
    exec.Command("pactl", "set-sink-volume", "@DEFAULT_SINK@", "-5%").Run()
}

func volumeMuteDBus() {
    exec.Command("pactl", "set-sink-mute", "@DEFAULT_SINK@", "toggle").Run()
}