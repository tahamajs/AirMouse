package ui

import (
	"image/color"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/theme"
)

// ------------------------------------------------------------
// CustomTheme – implements fyne.Theme with custom colours.
// ------------------------------------------------------------

type CustomTheme struct {
	name             string
	primaryColor     color.Color
	backgroundColor  color.Color
	foregroundColor  color.Color
	secondaryColor   color.Color
	successColor     color.Color
	warningColor     color.Color
	errorColor       color.Color
	hoverColor       color.Color
	pressedColor     color.Color
	focusColor       color.Color
	inputBgColor     color.Color
	placeholderColor color.Color
	scrollBarColor   color.Color
	shadowColor      color.Color
	hyperlinkColor   color.Color
}

// ------------------------------------------------------------
// Predefined themes
// ------------------------------------------------------------

var (
	// DarkTheme – default dark mode.
	DarkTheme = &CustomTheme{
		name:             "dark",
		primaryColor:     color.RGBA{99, 102, 241, 255},
		backgroundColor:  color.RGBA{15, 23, 42, 255},
		foregroundColor:  color.RGBA{241, 245, 249, 255},
		secondaryColor:   color.RGBA{30, 41, 59, 255},
		successColor:     color.RGBA{16, 185, 129, 255},
		warningColor:     color.RGBA{245, 158, 11, 255},
		errorColor:       color.RGBA{239, 68, 68, 255},
		hoverColor:       color.RGBA{51, 65, 85, 255},
		pressedColor:     color.RGBA{71, 85, 105, 255},
		focusColor:       color.RGBA{99, 102, 241, 200},
		inputBgColor:     color.RGBA{30, 41, 59, 255},
		placeholderColor: color.RGBA{148, 163, 184, 150},
		scrollBarColor:   color.RGBA{71, 85, 105, 200},
		shadowColor:      color.RGBA{0, 0, 0, 100},
		hyperlinkColor:   color.RGBA{96, 165, 250, 255},
	}

	// LightTheme – bright mode.
	LightTheme = &CustomTheme{
		name:             "light",
		primaryColor:     color.RGBA{37, 99, 235, 255},
		backgroundColor:  color.RGBA{241, 245, 249, 255},
		foregroundColor:  color.RGBA{15, 23, 42, 255},
		secondaryColor:   color.RGBA{226, 232, 240, 255},
		successColor:     color.RGBA{16, 185, 129, 255},
		warningColor:     color.RGBA{245, 158, 11, 255},
		errorColor:       color.RGBA{239, 68, 68, 255},
		hoverColor:       color.RGBA{203, 213, 225, 255},
		pressedColor:     color.RGBA{148, 163, 184, 255},
		focusColor:       color.RGBA{37, 99, 235, 200},
		inputBgColor:     color.RGBA{255, 255, 255, 255},
		placeholderColor: color.RGBA{71, 85, 105, 170},
		scrollBarColor:   color.RGBA{203, 213, 225, 200},
		shadowColor:      color.RGBA{0, 0, 0, 30},
		hyperlinkColor:   color.RGBA{29, 78, 216, 255},
	}

	// PureBlackTheme – OLED‑friendly all‑black.
	PureBlackTheme = &CustomTheme{
		name:             "pure_black",
		primaryColor:     color.RGBA{0, 150, 255, 255},
		backgroundColor:  color.RGBA{0, 0, 0, 255},
		foregroundColor:  color.RGBA{255, 255, 255, 255},
		secondaryColor:   color.RGBA{30, 30, 30, 255},
		successColor:     color.RGBA{0, 255, 0, 255},
		warningColor:     color.RGBA{255, 255, 0, 255},
		errorColor:       color.RGBA{255, 0, 0, 255},
		hoverColor:       color.RGBA{40, 40, 40, 255},
		pressedColor:     color.RGBA{60, 60, 60, 255},
		focusColor:       color.RGBA{0, 150, 255, 200},
		inputBgColor:     color.RGBA{20, 20, 20, 255},
		placeholderColor: color.RGBA{128, 128, 128, 150},
		scrollBarColor:   color.RGBA{60, 60, 60, 200},
		shadowColor:      color.RGBA{0, 0, 0, 200},
		hyperlinkColor:   color.RGBA{0, 150, 255, 255},
	}

	// HighContrastTheme – accessibility‑first.
	HighContrastTheme = &CustomTheme{
		name:             "high_contrast",
		primaryColor:     color.RGBA{255, 255, 0, 255},
		backgroundColor:  color.RGBA{0, 0, 0, 255},
		foregroundColor:  color.RGBA{255, 255, 255, 255},
		secondaryColor:   color.RGBA{50, 50, 50, 255},
		successColor:     color.RGBA{0, 255, 0, 255},
		warningColor:     color.RGBA{255, 255, 0, 255},
		errorColor:       color.RGBA{255, 0, 0, 255},
		hoverColor:       color.RGBA{80, 80, 80, 255},
		pressedColor:     color.RGBA{100, 100, 100, 255},
		focusColor:       color.RGBA{255, 255, 0, 200},
		inputBgColor:     color.RGBA{30, 30, 30, 255},
		placeholderColor: color.RGBA{200, 200, 200, 150},
		scrollBarColor:   color.RGBA{100, 100, 100, 200},
		shadowColor:      color.RGBA{0, 0, 0, 200},
		hyperlinkColor:   color.RGBA{0, 255, 255, 255},
	}

	// OceanTheme – calm blue tones.
	OceanTheme = &CustomTheme{
		name:             "ocean",
		primaryColor:     color.RGBA{0, 150, 200, 255},
		backgroundColor:  color.RGBA{10, 30, 50, 255},
		foregroundColor:  color.RGBA{200, 230, 255, 255},
		secondaryColor:   color.RGBA{20, 50, 80, 255},
		successColor:     color.RGBA{0, 200, 150, 255},
		warningColor:     color.RGBA{255, 150, 50, 255},
		errorColor:       color.RGBA{255, 80, 80, 255},
		hoverColor:       color.RGBA{30, 70, 110, 255},
		pressedColor:     color.RGBA{40, 90, 140, 255},
		focusColor:       color.RGBA{0, 150, 200, 200},
		inputBgColor:     color.RGBA{15, 40, 65, 255},
		placeholderColor: color.RGBA{150, 180, 210, 150},
		scrollBarColor:   color.RGBA{50, 100, 150, 200},
		shadowColor:      color.RGBA{0, 0, 0, 100},
		hyperlinkColor:   color.RGBA{0, 200, 255, 255},
	}

	// SunsetTheme – warm orange/red tones.
	SunsetTheme = &CustomTheme{
		name:             "sunset",
		primaryColor:     color.RGBA{255, 100, 50, 255},
		backgroundColor:  color.RGBA{40, 20, 30, 255},
		foregroundColor:  color.RGBA{255, 200, 150, 255},
		secondaryColor:   color.RGBA{60, 30, 45, 255},
		successColor:     color.RGBA{100, 200, 100, 255},
		warningColor:     color.RGBA{255, 150, 0, 255},
		errorColor:       color.RGBA{255, 60, 60, 255},
		hoverColor:       color.RGBA{80, 40, 60, 255},
		pressedColor:     color.RGBA{100, 50, 75, 255},
		focusColor:       color.RGBA{255, 100, 50, 200},
		inputBgColor:     color.RGBA{50, 25, 40, 255},
		placeholderColor: color.RGBA{200, 150, 120, 150},
		scrollBarColor:   color.RGBA{100, 60, 80, 200},
		shadowColor:      color.RGBA{0, 0, 0, 100},
		hyperlinkColor:   color.RGBA{255, 150, 100, 255},
	}

	// ForestTheme – earthy greens.
	ForestTheme = &CustomTheme{
		name:             "forest",
		primaryColor:     color.RGBA{50, 200, 100, 255},
		backgroundColor:  color.RGBA{20, 40, 20, 255},
		foregroundColor:  color.RGBA{200, 255, 200, 255},
		secondaryColor:   color.RGBA{30, 60, 30, 255},
		successColor:     color.RGBA{0, 255, 100, 255},
		warningColor:     color.RGBA{255, 200, 0, 255},
		errorColor:       color.RGBA{255, 80, 80, 255},
		hoverColor:       color.RGBA{40, 80, 40, 255},
		pressedColor:     color.RGBA{50, 100, 50, 255},
		focusColor:       color.RGBA{50, 200, 100, 200},
		inputBgColor:     color.RGBA{25, 50, 25, 255},
		placeholderColor: color.RGBA{150, 200, 150, 150},
		scrollBarColor:   color.RGBA{60, 120, 60, 200},
		shadowColor:      color.RGBA{0, 0, 0, 100},
		hyperlinkColor:   color.RGBA{100, 255, 150, 255},
	}

	// PurpleTheme – deep purple tones.
	PurpleTheme = &CustomTheme{
		name:             "purple",
		primaryColor:     color.RGBA{150, 80, 220, 255},
		backgroundColor:  color.RGBA{30, 20, 40, 255},
		foregroundColor:  color.RGBA{220, 200, 255, 255},
		secondaryColor:   color.RGBA{45, 30, 60, 255},
		successColor:     color.RGBA{100, 200, 150, 255},
		warningColor:     color.RGBA{255, 150, 50, 255},
		errorColor:       color.RGBA{255, 80, 120, 255},
		hoverColor:       color.RGBA{60, 40, 80, 255},
		pressedColor:     color.RGBA{75, 50, 100, 255},
		focusColor:       color.RGBA{150, 80, 220, 200},
		inputBgColor:     color.RGBA{35, 25, 50, 255},
		placeholderColor: color.RGBA{180, 160, 220, 150},
		scrollBarColor:   color.RGBA{80, 60, 120, 200},
		shadowColor:      color.RGBA{0, 0, 0, 100},
		hyperlinkColor:   color.RGBA{180, 120, 255, 255},
	}
)

// ------------------------------------------------------------
// fyne.Theme interface implementation
// ------------------------------------------------------------

func (t *CustomTheme) Color(name fyne.ThemeColorName, variant fyne.ThemeVariant) color.Color {
	switch name {
	case theme.ColorNamePrimary:
		return t.primaryColor
	case theme.ColorNameBackground:
		return t.backgroundColor
	case theme.ColorNameForeground:
		return t.foregroundColor
	case theme.ColorNameButton:
		return t.secondaryColor
	case theme.ColorNameDisabled:
		return color.RGBA{128, 128, 128, 128}
	case theme.ColorNameError:
		return t.errorColor
	case theme.ColorNameHover:
		return t.hoverColor
	case theme.ColorNamePressed:
		return t.pressedColor
	case theme.ColorNameFocus:
		return t.focusColor
	case theme.ColorNameInputBackground:
		return t.inputBgColor
	case theme.ColorNamePlaceHolder:
		return t.placeholderColor
	case theme.ColorNameScrollBar:
		return t.scrollBarColor
	case theme.ColorNameShadow:
		return t.shadowColor
	case theme.ColorNameHyperlink:
		return t.hyperlinkColor
	default:
		return theme.DefaultTheme().Color(name, variant)
	}
}

func (t *CustomTheme) Font(style fyne.TextStyle) fyne.Resource {
	return theme.DefaultTheme().Font(style)
}

func (t *CustomTheme) Icon(name fyne.ThemeIconName) fyne.Resource {
	return theme.DefaultTheme().Icon(name)
}

func (t *CustomTheme) Size(name fyne.ThemeSizeName) float32 {
	return theme.DefaultTheme().Size(name)
}

// ------------------------------------------------------------
// Theme lookup helpers
// ------------------------------------------------------------

// getThemeByName returns a fyne.Theme by name.
func getThemeByName(name string) fyne.Theme {
	switch name {
	case "light":
		return LightTheme
	case "dark":
		return DarkTheme
	case "pure_black":
		return PureBlackTheme
	case "high_contrast":
		return HighContrastTheme
	case "ocean":
		return OceanTheme
	case "sunset":
		return SunsetTheme
	case "forest":
		return ForestTheme
	case "purple":
		return PurpleTheme
	default:
		return DarkTheme
	}
}

// GetAllThemes returns a slice of all available theme names.
func GetAllThemes() []string {
	return []string{
		"dark",
		"light",
		"pure_black",
		"high_contrast",
		"ocean",
		"sunset",
		"forest",
		"purple",
	}
}