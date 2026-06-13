package ui

import (
    "image/color"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/theme"
)

// CustomTheme implements fyne.Theme with custom colors
type CustomTheme struct {
    name        string
    primaryColor color.Color
    backgroundColor color.Color
    foregroundColor color.Color
}

// Predefined themes
var (
    // Dark theme (default)
    DarkTheme = &CustomTheme{
        name:           "dark",
        primaryColor:   color.RGBA{0, 122, 204, 255},
        backgroundColor: color.RGBA{30, 30, 40, 255},
        foregroundColor: color.RGBA{255, 255, 255, 255},
    }
    
    // Light theme
    LightTheme = &CustomTheme{
        name:           "light",
        primaryColor:   color.RGBA{0, 122, 204, 255},
        backgroundColor: color.RGBA{245, 245, 245, 255},
        foregroundColor: color.RGBA{0, 0, 0, 255},
    }
    
    // Pure black theme (OLED friendly)
    PureBlackTheme = &CustomTheme{
        name:           "pure_black",
        primaryColor:   color.RGBA{0, 150, 255, 255},
        backgroundColor: color.RGBA{0, 0, 0, 255},
        foregroundColor: color.RGBA{255, 255, 255, 255},
    }
    
    // High contrast theme (accessibility)
    HighContrastTheme = &CustomTheme{
        name:           "high_contrast",
        primaryColor:   color.RGBA{255, 255, 0, 255},
        backgroundColor: color.RGBA{0, 0, 0, 255},
        foregroundColor: color.RGBA{255, 255, 255, 255},
    }
    
    // Ocean theme
    OceanTheme = &CustomTheme{
        name:           "ocean",
        primaryColor:   color.RGBA{0, 150, 200, 255},
        backgroundColor: color.RGBA{10, 30, 50, 255},
        foregroundColor: color.RGBA{200, 230, 255, 255},
    }
    
    // Sunset theme
    SunsetTheme = &CustomTheme{
        name:           "sunset",
        primaryColor:   color.RGBA{255, 100, 50, 255},
        backgroundColor: color.RGBA{40, 20, 30, 255},
        foregroundColor: color.RGBA{255, 200, 150, 255},
    }
    
    // Forest theme
    ForestTheme = &CustomTheme{
        name:           "forest",
        primaryColor:   color.RGBA{50, 200, 100, 255},
        backgroundColor: color.RGBA{20, 40, 20, 255},
        foregroundColor: color.RGBA{200, 255, 200, 255},
    }
    
    // Purple theme
    PurpleTheme = &CustomTheme{
        name:           "purple",
        primaryColor:   color.RGBA{150, 80, 220, 255},
        backgroundColor: color.RGBA{30, 20, 40, 255},
        foregroundColor: color.RGBA{220, 200, 255, 255},
    }
    
    // Cherry theme
    CherryTheme = &CustomTheme{
        name:           "cherry",
        primaryColor:   color.RGBA{255, 80, 120, 255},
        backgroundColor: color.RGBA{40, 20, 25, 255},
        foregroundColor: color.RGBA{255, 200, 210, 255},
    }
    
    // Neon theme
    NeonTheme = &CustomTheme{
        name:           "neon",
        primaryColor:   color.RGBA{0, 255, 150, 255},
        backgroundColor: color.RGBA{0, 0, 0, 255},
        foregroundColor: color.RGBA{0, 255, 200, 255},
    }
    
    // Lavender theme
    LavenderTheme = &CustomTheme{
        name:           "lavender",
        primaryColor:   color.RGBA{180, 130, 255, 255},
        backgroundColor: color.RGBA{40, 30, 60, 255},
        foregroundColor: color.RGBA{230, 210, 255, 255},
    }
    
    // Mint theme
    MintTheme = &CustomTheme{
        name:           "mint",
        primaryColor:   color.RGBA{100, 200, 180, 255},
        backgroundColor: color.RGBA{20, 50, 40, 255},
        foregroundColor: color.RGBA{200, 255, 240, 255},
    }
    
    // Peach theme
    PeachTheme = &CustomTheme{
        name:           "peach",
        primaryColor:   color.RGBA{255, 180, 100, 255},
        backgroundColor: color.RGBA{50, 30, 20, 255},
        foregroundColor: color.RGBA{255, 220, 180, 255},
    }
    
    // Sky theme
    SkyTheme = &CustomTheme{
        name:           "sky",
        primaryColor:   color.RGBA{100, 180, 255, 255},
        backgroundColor: color.RGBA{20, 40, 70, 255},
        foregroundColor: color.RGBA{200, 230, 255, 255},
    }
)

func (t *CustomTheme) Color(name fyne.ThemeColorName, variant fyne.ThemeVariant) color.Color {
    switch name {
    case theme.ColorNamePrimary:
        return t.primaryColor
    case theme.ColorNameBackground:
        return t.backgroundColor
    case theme.ColorNameForeground:
        return t.foregroundColor
    case theme.ColorNameButton:
        return t.primaryColor
    case theme.ColorNameDisabled:
        return color.RGBA{128, 128, 128, 128}
    case theme.ColorNameError:
        return color.RGBA{255, 80, 80, 255}
    case theme.ColorNameHover:
        return color.RGBA{255, 255, 255, 30}
    case theme.ColorNamePressed:
        return color.RGBA{0, 0, 0, 30}
    case theme.ColorNameFocus:
        return t.primaryColor
    case theme.ColorNameInputBackground:
        return color.RGBA{255, 255, 255, 50}
    case theme.ColorNamePlaceHolder:
        return color.RGBA{200, 200, 200, 150}
    case theme.ColorNameScrollBar:
        return color.RGBA{255, 255, 255, 100}
    case theme.ColorNameShadow:
        return color.RGBA{0, 0, 0, 100}
    case theme.ColorNameHyperlink:
        return color.RGBA{0, 150, 255, 255}
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

// GetThemeByName returns theme by name
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
    case "cherry":
        return CherryTheme
    case "neon":
        return NeonTheme
    case "lavender":
        return LavenderTheme
    case "mint":
        return MintTheme
    case "peach":
        return PeachTheme
    case "sky":
        return SkyTheme
    default:
        return DarkTheme
    }
}

// GetAllThemes returns list of available theme names
func GetAllThemes() []string {
    return []string{
        "dark", "light", "pure_black", "high_contrast",
        "ocean", "sunset", "forest", "purple", "cherry",
        "neon", "lavender", "mint", "peach", "sky",
    }
}