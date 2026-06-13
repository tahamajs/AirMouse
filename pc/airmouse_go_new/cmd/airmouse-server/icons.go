// icons.go
package main

var iconData = []byte{
    // PNG icon data - you can embed a 16x16 or 32x32 PNG
    // For development, use a simple placeholder
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG header
    // ... full PNG data
}

var runningIconData = []byte{
    // Green icon for running state
}

var stoppedIconData = []byte{
    // Gray icon for stopped state
}

var playIcon = []byte{}
var stopIcon = []byte{}
var restartIcon = []byte{}