package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
)

type SettingsTab struct {
	sensitivitySlider *widget.Slider
	sensitivityLabel  *widget.Label
	themeSelect       *widget.Select
	alwaysOnTopCheck  *widget.Check
	smoothingCheck    *widget.Check
	accelCheck        *widget.Check
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
	tab := &SettingsTab{}

	tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
	tab.sensitivitySlider.Value = cfg.Sensitivity
	tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	tab.sensitivitySlider.OnChanged = func(v float64) {
		cfg.SetSensitivity(v)
		mouse.SetSensitivity(v)
		tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	tab.themeSelect = widget.NewSelect(
		[]string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"},
		func(theme string) {
			cfg.SetTheme(theme)
			app := fyne.CurrentApp()
			app.Settings().SetTheme(getThemeByName(theme))
		},
	)
	tab.themeSelect.SetSelected(cfg.Theme)

	tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		cfg.Save()
		win := fyne.CurrentApp().Driver().AllWindows()[0]
		win.SetAlwaysOnTop(b)
	})
	tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		mouse.SetSmoothing(b)
	})
	tab.smoothingCheck.SetChecked(true)

	tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		mouse.SetAcceleration(b, 1.5)
	})
	tab.accelCheck.SetChecked(true)

	return container.NewVBox(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Cursor Sensitivity"),
		tab.sensitivitySlider,
		tab.sensitivityLabel,
		widget.NewSeparator(),
		widget.NewLabel("Appearance"),
		tab.themeSelect,
		tab.alwaysOnTopCheck,
		widget.NewSeparator(),
		widget.NewLabel("Mouse Behaviour"),
		tab.smoothingCheck,
		tab.accelCheck,
	)
}


personalizationCheck := widget.NewCheck("Enable AI Personalization", func(b bool) {
    cfg.EnablePersonalization = b
    cfg.Save()
})
personalizationCheck.SetChecked(cfg.EnablePersonalization)

bufferSlider := widget.NewSlider(500, 5000)
bufferSlider.Value = float64(cfg.PersonalizationBuffer)
bufferSlider.OnChanged = func(v float64) {
    cfg.PersonalizationBuffer = int(v)
    cfg.Save()
}
bufferSliderLabel := widget.NewLabel(fmt.Sprintf("Buffer Size: %d", cfg.PersonalizationBuffer))

intervalSlider := widget.NewSlider(600, 86400) // 10 minutes to 24 hours
intervalSlider.Value = float64(cfg.PersonalizationInterval)
intervalSlider.OnChanged = func(v float64) {
    cfg.PersonalizationInterval = int(v)
    cfg.Save()
}
intervalLabel := widget.NewLabel(fmt.Sprintf("Retrain Interval: %d seconds", cfg.PersonalizationInterval))

swapCheck := widget.NewCheck("Auto‑swap trained model", func(b bool) {
    cfg.AutoSwapModel = b
    cfg.Save()
})
swapCheck.SetChecked(cfg.AutoSwapModel)


package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
)

type SettingsTab struct {
	sensitivitySlider *widget.Slider
	sensitivityLabel  *widget.Label
	themeSelect       *widget.Select
	alwaysOnTopCheck  *widget.Check
	smoothingCheck    *widget.Check
	accelCheck        *widget.Check
	aiCheck           *widget.Check
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
	tab := &SettingsTab{}

	tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
	tab.sensitivitySlider.Value = cfg.Sensitivity
	tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	tab.sensitivitySlider.OnChanged = func(v float64) {
		cfg.SetSensitivity(v)
		mouse.SetSensitivity(v)
		tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	tab.themeSelect = widget.NewSelect(
		[]string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"},
		func(theme string) {
			cfg.SetTheme(theme)
			app := fyne.CurrentApp()
			app.Settings().SetTheme(getThemeByName(theme))
		},
	)
	tab.themeSelect.SetSelected(cfg.Theme)

	tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		cfg.Save()
		win := fyne.CurrentApp().Driver().AllWindows()[0]
		win.SetAlwaysOnTop(b)
	})
	tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		mouse.SetSmoothing(b)
	})
	tab.smoothingCheck.SetChecked(true)

	tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		mouse.SetAcceleration(b, 1.5)
	})
	tab.accelCheck.SetChecked(true)

	tab.aiCheck = widget.NewCheck("AI Smoothing (RNN model)", func(b bool) {
		mouse.EnableAISmoothing(b)
		cfg.EnableAISmoothing = b
		cfg.Save()
	})
	tab.aiCheck.SetChecked(cfg.EnableAISmoothing)

	return container.NewVBox(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Cursor Sensitivity"),
		tab.sensitivitySlider,
		tab.sensitivityLabel,
		widget.NewSeparator(),
		widget.NewLabel("Appearance"),
		tab.themeSelect,
		tab.alwaysOnTopCheck,
		widget.NewSeparator(),
		widget.NewLabel("Mouse Behaviour"),
		tab.smoothingCheck,
		tab.accelCheck,
		tab.aiCheck,
	)
}


// inside NewSettingsTab
predictiveCheck := widget.NewCheck("Predictive Movement (Kalman filter)", func(b bool) {
    cfg.SetPredictiveEnabled(b)
    mouse.EnablePredictive(b)
})
predictiveCheck.SetChecked(cfg.EnablePredictive)

blendSlider := widget.NewSlider(0, 1)
blendSlider.Step = 0.05
blendSlider.Value = cfg.PredictiveBlendFactor
blendLabel := widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", cfg.PredictiveBlendFactor))
blendSlider.OnChanged = func(v float64) {
    cfg.SetPredictiveBlendFactor(v)
    mouse.SetPredictiveBlendFactor(v)
    blendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
}


package ui

import (
    "fmt"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
)

type SettingsTab struct {
    sensitivitySlider *widget.Slider
    sensitivityLabel  *widget.Label
    themeSelect       *widget.Select
    alwaysOnTopCheck  *widget.Check
    smoothingCheck    *widget.Check
    accelCheck        *widget.Check
    aiCheck           *widget.Check
    predictiveCheck   *widget.Check
    blendSlider       *widget.Slider
    blendLabel        *widget.Label
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
    tab := &SettingsTab{}

    // Sensitivity
    tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
    tab.sensitivitySlider.Value = cfg.Sensitivity
    tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
    tab.sensitivitySlider.OnChanged = func(v float64) {
        cfg.SetSensitivity(v)
        mouse.SetSensitivity(v)
        tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
    }

    // Theme
    tab.themeSelect = widget.NewSelect(
        []string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"},
        func(theme string) {
            cfg.SetTheme(theme)
            app := fyne.CurrentApp()
            app.Settings().SetTheme(getThemeByName(theme))
        },
    )
    tab.themeSelect.SetSelected(cfg.Theme)

    // Always on top
    tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
        cfg.AlwaysOnTop = b
        cfg.Save()
        win := fyne.CurrentApp().Driver().AllWindows()[0]
        win.SetAlwaysOnTop(b)
    })
    tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

    // Smoothing
    tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
        mouse.SetSmoothing(b)
    })
    tab.smoothingCheck.SetChecked(true)

    // Acceleration
    tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
        mouse.SetAcceleration(b, 1.5)
    })
    tab.accelCheck.SetChecked(true)

    // AI smoothing
    tab.aiCheck = widget.NewCheck("AI Smoothing (RNN model)", func(b bool) {
        mouse.EnableAISmoothing(b)
        cfg.EnableAISmoothing = b
        cfg.Save()
    })
    tab.aiCheck.SetChecked(cfg.EnableAISmoothing)

    // Predictive movement (Kalman)
    tab.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman filter)", func(b bool) {
        mouse.EnablePredictive(b)
        cfg.SetPredictiveEnabled(b)
    })
    tab.predictiveCheck.SetChecked(cfg.EnablePredictive)

    tab.blendSlider = widget.NewSlider(0, 1)
    tab.blendSlider.Step = 0.05
    tab.blendSlider.Value = cfg.PredictiveBlendFactor
    tab.blendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", cfg.PredictiveBlendFactor))
    tab.blendSlider.OnChanged = func(v float64) {
        cfg.SetPredictiveBlendFactor(v)
        mouse.SetPredictiveBlendFactor(v)
        tab.blendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
    }

    // Assemble UI
    return container.NewVBox(
        widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("Cursor Sensitivity"),
        tab.sensitivitySlider,
        tab.sensitivityLabel,
        widget.NewSeparator(),
        widget.NewLabel("Appearance"),
        tab.themeSelect,
        tab.alwaysOnTopCheck,
        widget.NewSeparator(),
        widget.NewLabel("Mouse Behaviour"),
        tab.smoothingCheck,
        tab.accelCheck,
        tab.aiCheck,
        widget.NewSeparator(),
        widget.NewLabel("Latency Compensation"),
        tab.predictiveCheck,
        tab.blendSlider,
        tab.blendLabel,
    )
}