package control

import (
	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
)

type MouseController = mouse.Controller
var NewMouseController = mouse.NewController

var SetMovementPaused = common.SetMovementPaused
var ClearPause = common.ClearPause
var IsMovementPaused = common.IsMovementPaused

var ExecuteSystemCommand = syscmd.ExecuteSystemCommand

var HasAccessibilityPermission = mouse.HasAccessibilityPermission
var OpenAccessibilitySettings = mouse.OpenAccessibilitySettings
