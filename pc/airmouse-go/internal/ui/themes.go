package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/theme"
)

var (
	_ fyne.Theme = (*CustomTheme)(nil)
)

type CustomTheme struct {
	name       string
	primary    fyne.ThemeColorName
	background fyne.ThemeColorName
	text       fyne.ThemeColorName
}

func (t *CustomTheme) Color(name fyne.ThemeColorName, variant fyne.ThemeVariant) fyne.Color {
	switch name {
	case theme.ColorNamePrimary:
		return theme.PrimaryColorNamed("blue")
	case theme.ColorNameBackground:
		return theme.DefaultTheme().Color(theme.ColorNameBackground, theme.VariantDark)
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

func getThemeByName(name string) fyne.Theme {
	switch name {
	case "light":
		return theme.LightTheme()
	case "dark":
		return theme.DarkTheme()
	case "pure_black":
		return &CustomTheme{name: "pure_black"}
	default:
		return theme.DarkTheme()
	}
}