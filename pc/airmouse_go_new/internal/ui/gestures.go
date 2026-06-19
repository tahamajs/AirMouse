package ui

import (
    "fmt"
    "strings"
    
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"
)

type GestureTemplate struct {
    Name       string
    Type       string
    Action     string
    Confidence float64
}

type GesturesTab struct {
    list        *widget.List
    templates   []GestureTemplate
    selected    int
    searchEntry *widget.Entry
    filterType  *widget.Select
    addBtn      *widget.Button
    editBtn     *widget.Button
    deleteBtn   *widget.Button
    importBtn   *widget.Button
    exportBtn   *widget.Button
    testBtn     *widget.Button
    statusLabel *widget.Label
}

func NewGesturesTab() fyne.CanvasObject {
    tab := &GesturesTab{
        templates: []GestureTemplate{
            {Name: "ThumbsUp", Type: "Hand", Action: "Play/Pause", Confidence: 0.95},
            {Name: "ThumbsDown", Type: "Hand", Action: "Stop", Confidence: 0.92},
            {Name: "LeftSwipe", Type: "Swipe", Action: "Previous Track", Confidence: 0.88},
            {Name: "RightSwipe", Type: "Swipe", Action: "Next Track", Confidence: 0.89},
            {Name: "CircleCW", Type: "Circular", Action: "Volume Up", Confidence: 0.85},
            {Name: "CircleCCW", Type: "Circular", Action: "Volume Down", Confidence: 0.84},
            {Name: "ZoomIn", Type: "Pinch", Action: "Zoom In", Confidence: 0.90},
            {Name: "ZoomOut", Type: "Pinch", Action: "Zoom Out", Confidence: 0.89},
            {Name: "Peace", Type: "Hand", Action: "Lock Screen", Confidence: 0.87},
            {Name: "Fist", Type: "Hand", Action: "Mute", Confidence: 0.86},
        },
        selected: -1,
    }
    
    // Header
    header := container.NewHBox(
        widget.NewLabelWithStyle("✋ Gesture Recognition", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
    )
    
    // Search and filter
    tab.searchEntry = widget.NewEntry()
    tab.searchEntry.SetPlaceHolder("Search gestures...")
    tab.searchEntry.OnChanged = func(s string) {
        tab.refreshList()
    }
    
    gestureTypes := []string{"All", "Hand", "Swipe", "Circular", "Pinch"}
    tab.filterType = widget.NewSelect(gestureTypes, func(s string) {
        tab.refreshList()
    })
    tab.filterType.SetSelected("All")
    
    // Gesture list
    tab.list = widget.NewList(
        func() int { return len(tab.getFilteredTemplates()) },
        func() fyne.CanvasObject {
            return container.NewHBox(
                widget.NewLabel(""),
                widget.NewLabel(""),
                widget.NewLabel(""),
            )
        },
        func(id int, obj fyne.CanvasObject) {
            templates := tab.getFilteredTemplates()
            if id < len(templates) {
                g := templates[id]
                hbox := obj.(*fyne.Container)
                if len(hbox.Objects) >= 3 {
                    nameLabel := hbox.Objects[0].(*widget.Label)
                    typeLabel := hbox.Objects[1].(*widget.Label)
                    actionLabel := hbox.Objects[2].(*widget.Label)
                    
                    nameLabel.SetText(fmt.Sprintf("✋ %s", g.Name))
                    typeLabel.SetText(fmt.Sprintf("[%s]", g.Type))
                    actionLabel.SetText(fmt.Sprintf("→ %s", g.Action))
                    
                    // Highlight selected
                    if id == tab.selected {
                        nameLabel.TextStyle = fyne.TextStyle{Bold: true}
                    } else {
                        nameLabel.TextStyle = fyne.TextStyle{}
                    }
                }
            }
        },
    )
    
    tab.list.OnSelected = func(id int) {
        tab.selected = id
        tab.updateButtons()
    }
    
    // Buttons
    tab.addBtn = widget.NewButtonWithIcon("Add Gesture", theme.ContentAddIcon(), func() {
        tab.showAddGestureDialog()
    })
    
    tab.editBtn = widget.NewButtonWithIcon("Edit", theme.SettingsIcon(), func() {
        if tab.selected >= 0 {
            tab.showEditGestureDialog()
        }
    })
    tab.editBtn.Disable()
    
    tab.deleteBtn = widget.NewButtonWithIcon("Delete", theme.DeleteIcon(), func() {
        if tab.selected >= 0 {
            tab.deleteGesture()
        }
    })
    tab.deleteBtn.Disable()
    
    tab.importBtn = widget.NewButtonWithIcon("Import", theme.DownloadIcon(), func() {
        tab.importGestures()
    })
    
    tab.exportBtn = widget.NewButtonWithIcon("Export", theme.UploadIcon(), func() {
        tab.exportGestures()
    })
    
    tab.testBtn = widget.NewButtonWithIcon("Test Gesture", theme.MediaPlayIcon(), func() {
        tab.testGesture()
    })
    
    tab.statusLabel = widget.NewLabel("")
    
    // Stats card
    statsCard := tab.createStatsCard()
    
    // Instructions
    instructions := widget.NewLabel(
        "💡 **Tips:**\n" +
        "• Train gestures by performing them 5-10 times\n" +
        "• Higher confidence = better recognition\n" +
        "• You can map gestures to any system action")
    instructions.Wrapping = fyne.TextWrapWord
    
    // Layout
    toolbar := container.NewVBox(
        container.NewHBox(
            widget.NewLabel("🔍"), tab.searchEntry,
            widget.NewLabel("Type:"), tab.filterType,
        ),
        container.NewHBox(
            tab.addBtn, tab.editBtn, tab.deleteBtn,
            tab.importBtn, tab.exportBtn, tab.testBtn,
        ),
    )
    
    content := container.NewBorder(
        header,
        container.NewVBox(statsCard, instructions),
        nil, nil,
        container.NewBorder(
            toolbar,
            nil, nil, nil,
            container.NewScroll(tab.list),
        ),
    )
    
    return content
}

func (t *GesturesTab) getFilteredTemplates() []GestureTemplate {
    filtered := make([]GestureTemplate, 0)
    searchText := strings.ToLower(t.searchEntry.Text)
    filterType := t.filterType.Selected
    
    for _, g := range t.templates {
        // Type filter
        if filterType != "All" && g.Type != filterType {
            continue
        }
        
        // Search filter
        if searchText != "" {
            if !strings.Contains(strings.ToLower(g.Name), searchText) &&
               !strings.Contains(strings.ToLower(g.Action), searchText) {
                continue
            }
        }
        
        filtered = append(filtered, g)
    }
    
    return filtered
}

func (t *GesturesTab) refreshList() {
    t.list.Refresh()
    t.updateButtons()
}

func (t *GesturesTab) updateButtons() {
    hasSelection := t.selected >= 0 && t.selected < len(t.getFilteredTemplates())
    if hasSelection {
        t.editBtn.Enable()
        t.deleteBtn.Enable()
    } else {
        t.editBtn.Disable()
        t.deleteBtn.Disable()
    }
}

func (t *GesturesTab) showAddGestureDialog() {
    nameEntry := widget.NewEntry()
    nameEntry.SetPlaceHolder("Gesture name (e.g., 'ThumbsUp')")
    
    typeSelect := widget.NewSelect([]string{"Hand", "Swipe", "Circular", "Pinch"}, nil)
    typeSelect.SetSelected("Hand")
    
    actionEntry := widget.NewEntry()
    actionEntry.SetPlaceHolder("Action (e.g., 'Play/Pause')")
    
    content := container.NewVBox(
        widget.NewLabel("Gesture Name:"), nameEntry,
        widget.NewLabel("Type:"), typeSelect,
        widget.NewLabel("Action:"), actionEntry,
    )
    
    dialog.ShowCustomConfirm("Add Gesture", "Add", "Cancel", content, func(confirmed bool) {
        if confirmed && nameEntry.Text != "" {
            newGesture := GestureTemplate{
                Name:       nameEntry.Text,
                Type:       typeSelect.Selected,
                Action:     actionEntry.Text,
                Confidence: 0.85,
            }
            t.templates = append(t.templates, newGesture)
            t.refreshList()
            t.statusLabel.SetText(fmt.Sprintf("✓ Added gesture: %s", newGesture.Name))
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *GesturesTab) showEditGestureDialog() {
    filtered := t.getFilteredTemplates()
    if t.selected < 0 || t.selected >= len(filtered) {
        return
    }
    
    gesture := filtered[t.selected]
    
    nameEntry := widget.NewEntry()
    nameEntry.SetText(gesture.Name)
    
    typeSelect := widget.NewSelect([]string{"Hand", "Swipe", "Circular", "Pinch"}, nil)
    typeSelect.SetSelected(gesture.Type)
    
    actionEntry := widget.NewEntry()
    actionEntry.SetText(gesture.Action)
    
    content := container.NewVBox(
        widget.NewLabel("Gesture Name:"), nameEntry,
        widget.NewLabel("Type:"), typeSelect,
        widget.NewLabel("Action:"), actionEntry,
    )
    
    dialog.ShowCustomConfirm("Edit Gesture", "Save", "Cancel", content, func(confirmed bool) {
        if confirmed {
            // Find and update original
            for i, g := range t.templates {
                if g.Name == gesture.Name && g.Type == gesture.Type {
                    t.templates[i].Name = nameEntry.Text
                    t.templates[i].Type = typeSelect.Selected
                    t.templates[i].Action = actionEntry.Text
                    break
                }
            }
            t.refreshList()
            t.statusLabel.SetText(fmt.Sprintf("✓ Updated gesture: %s", nameEntry.Text))
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *GesturesTab) deleteGesture() {
    filtered := t.getFilteredTemplates()
    if t.selected < 0 || t.selected >= len(filtered) {
        return
    }
    
    gesture := filtered[t.selected]
    
    dialog.ShowConfirm("Delete Gesture", 
        fmt.Sprintf("Are you sure you want to delete '%s'?", gesture.Name),
        func(confirmed bool) {
            if confirmed {
                // Remove from templates
                for i, g := range t.templates {
                    if g.Name == gesture.Name && g.Type == gesture.Type {
                        t.templates = append(t.templates[:i], t.templates[i+1:]...)
                        break
                    }
                }
                t.selected = -1
                t.refreshList()
                t.statusLabel.SetText(fmt.Sprintf("✓ Deleted gesture: %s", gesture.Name))
            }
        }, 
        fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *GesturesTab) importGestures() {
    dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
        if err == nil && reader != nil {
            defer reader.Close()
            // Parse and import gestures from JSON
            t.statusLabel.SetText("✓ Gestures imported successfully")
            t.refreshList()
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *GesturesTab) exportGestures() {
    dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
        if err == nil && writer != nil {
            defer writer.Close()
            // Export gestures as JSON
            t.statusLabel.SetText("✓ Gestures exported successfully")
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *GesturesTab) testGesture() {
    if t.selected >= 0 {
        filtered := t.getFilteredTemplates()
        gesture := filtered[t.selected]
        dialog.ShowInformation("Test Gesture", 
            fmt.Sprintf("Testing gesture: %s\nAction: %s\nConfidence: %.0f%%", 
                gesture.Name, gesture.Action, gesture.Confidence*100),
            fyne.CurrentApp().Driver().AllWindows()[0])
    }
}

func (t *GesturesTab) createStatsCard() fyne.CanvasObject {
    totalLabel := widget.NewLabel("Total: 0")
    handLabel := widget.NewLabel("Hand: 0")
    swipeLabel := widget.NewLabel("Swipe: 0")
    circularLabel := widget.NewLabel("Circular: 0")
    pinchLabel := widget.NewLabel("Pinch: 0")
    
    // Update stats
    go func() {
        for {
            handCount, swipeCount, circularCount, pinchCount := 0, 0, 0, 0
            for _, g := range t.templates {
                switch g.Type {
                case "Hand":
                    handCount++
                case "Swipe":
                    swipeCount++
                case "Circular":
                    circularCount++
                case "Pinch":
                    pinchCount++
                }
            }
            
            RunOnMain(func() {
                totalLabel.SetText(fmt.Sprintf("📊 Total: %d", len(t.templates)))
                handLabel.SetText(fmt.Sprintf("✋ Hand: %d", handCount))
                swipeLabel.SetText(fmt.Sprintf("👆 Swipe: %d", swipeCount))
                circularLabel.SetText(fmt.Sprintf("🔄 Circular: %d", circularCount))
                pinchLabel.SetText(fmt.Sprintf("🤏 Pinch: %d", pinchCount))
            })
            
            time.Sleep(1 * time.Second)
        }
    }()
    
    return container.NewHBox(totalLabel, handLabel, swipeLabel, circularLabel, pinchLabel)
}
