package ui

import (
	"image/color"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/theme"
)

// PremiumTheme - Modern glassmorphism design
type PremiumTheme struct {
	DarkMode bool
}

var (
	PremiumDark  = &PremiumTheme{DarkMode: true}
	PremiumLight = &PremiumTheme{DarkMode: false}
)

// Color returns the color for a given theme color name
func (t *PremiumTheme) Color(name fyne.ThemeColorName, variant fyne.ThemeVariant) color.Color {
	switch name {
	case theme.ColorNameBackground:
		if t.DarkMode {
			return color.RGBA{10, 10, 20, 255} // Deep dark
		}
		return color.RGBA{240, 242, 248, 255}

	case theme.ColorNameForeground:
		if t.DarkMode {
			return color.RGBA{230, 235, 255, 255}
		}
		return color.RGBA{20, 25, 40, 255}

	case theme.ColorNamePrimary:
		return color.RGBA{99, 102, 241, 255} // Indigo

	case theme.ColorNameButton:
		return color.RGBA{99, 102, 241, 255}

	case theme.ColorNameDisabled:
		return color.RGBA{128, 128, 140, 180}

	case theme.ColorNameError:
		return color.RGBA{239, 68, 68, 255}

	case theme.ColorNameHover:
		if t.DarkMode {
			return color.RGBA{60, 60, 90, 255}
		}
		return color.RGBA{220, 220, 240, 255}

	case theme.ColorNamePressed:
		if t.DarkMode {
			return color.RGBA{80, 80, 120, 255}
		}
		return color.RGBA{180, 180, 210, 255}

	case theme.ColorNameInputBackground:
		if t.DarkMode {
			return color.RGBA{25, 25, 45, 255}
		}
		return color.RGBA{240, 240, 248, 255}

	case theme.ColorNamePlaceHolder:
		return color.RGBA{150, 155, 180, 180}

	case theme.ColorNameScrollBar:
		if t.DarkMode {
			return color.RGBA{60, 60, 90, 200}
		}
		return color.RGBA{200, 200, 220, 200}

	case theme.ColorNameShadow:
		return color.RGBA{0, 0, 0, 80}

	case theme.ColorNameHyperlink:
		return color.RGBA{96, 165, 250, 255}

	default:
		return theme.DefaultTheme().Color(name, variant)
	}
}

func (t *PremiumTheme) Font(style fyne.TextStyle) fyne.Resource {
	return theme.DefaultTheme().Font(style)
}

func (t *PremiumTheme) Icon(name fyne.ThemeIconName) fyne.Resource {
	return theme.DefaultTheme().Icon(name)
}

func (t *PremiumTheme) Size(name fyne.ThemeSizeName) float32 {
	switch name {
	case theme.SizeNameText:
		return 14
	case theme.SizeNamePadding:
		return 14
	case theme.SizeNameInnerPadding:
		return 10
	case theme.SizeNameScrollBar:
		return 8
	case theme.SizeNameSeparatorThickness:
		return 1
	default:
		return theme.DefaultTheme().Size(name)
	}
}
