package ui

import (
	"encoding/json"
	"fmt"
	"strings"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
)

// GestureTemplate represents a single gesture template.
type GestureTemplate struct {
	Name       string  `json:"name"`
	Type       string  `json:"type"`
	Action     string  `json:"action"`
	Confidence float64 `json:"confidence"`
}

// defaultGestures returns the built‑in gesture templates.
func defaultGestures() []GestureTemplate {
	return []GestureTemplate{
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
	}
}

// GesturesTab is the gestures management tab.
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
	resetBtn    *widget.Button
	helpBtn     *widget.Button
	testBtn     *widget.Button
	statusLabel *widget.Label
	overview    *widget.Label
	activeCount *widget.Label
	mu          sync.RWMutex // protects templates
}

// NewGesturesTab creates the gestures management tab.
func NewGesturesTab() fyne.CanvasObject {
	tab := &GesturesTab{
		templates: defaultGestures(),
		selected:  -1,
	}

	tab.overview = widget.NewLabel("Gestures help you map hand motion to high-level actions like media control, locking, and zooming.")
	tab.overview.Wrapping = fyne.TextWrapWord
	tab.activeCount = widget.NewLabel("")

	// ----- Header -----
	header := container.NewHBox(
		widget.NewLabelWithStyle("✋ Gesture Recognition", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
	)

	// ----- Search & Filter -----
	tab.searchEntry = widget.NewEntry()
	tab.searchEntry.SetPlaceHolder("Search gestures...")
	tab.searchEntry.OnChanged = func(s string) {
		tab.refreshList()
	}

	gestureTypes := []string{"All", "Hand", "Swipe", "Circular", "Pinch"}
	tab.filterType = widget.NewSelect(gestureTypes, nil)
	tab.filterType.SetSelected("All")
	tab.filterType.OnChanged = func(s string) {
		tab.refreshList()
	}

	// ----- Gesture List -----
	tab.list = widget.NewList(
		func() int { return len(tab.getFilteredTemplates()) },
		func() fyne.CanvasObject {
			return container.NewHBox(
				widget.NewLabel(""), // name
				widget.NewLabel(""), // type
				widget.NewLabel(""), // action
			)
		},
		func(id int, obj fyne.CanvasObject) {
			templates := tab.getFilteredTemplates()
			if id < 0 || id >= len(templates) {
				return
			}
			g := templates[id]
			hbox := obj.(*fyne.Container)
			if len(hbox.Objects) >= 3 {
				nameLabel := hbox.Objects[0].(*widget.Label)
				typeLabel := hbox.Objects[1].(*widget.Label)
				actionLabel := hbox.Objects[2].(*widget.Label)

				nameLabel.SetText(fmt.Sprintf("✋ %s", g.Name))
				typeLabel.SetText(fmt.Sprintf("[%s]", g.Type))
				actionLabel.SetText(fmt.Sprintf("→ %s", g.Action))

				if id == tab.selected {
					nameLabel.TextStyle = fyne.TextStyle{Bold: true}
				} else {
					nameLabel.TextStyle = fyne.TextStyle{}
				}
			}
		},
	)

	tab.list.OnSelected = func(id int) {
		tab.selected = id
		tab.updateButtons()
	}

	// ----- Buttons with tooltips -----
	tab.addBtn = widget.NewButtonWithIcon("Add Gesture", theme.ContentAddIcon(), func() {
		tab.showAddGestureDialog()
	})
	tab.addBtn.Importance = widget.HighImportance

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
	tab.deleteBtn.Importance = widget.DangerImportance

	tab.importBtn = widget.NewButtonWithIcon("Import", theme.DownloadIcon(), func() {
		tab.importGestures()
	})

	tab.exportBtn = widget.NewButtonWithIcon("Export", theme.UploadIcon(), func() {
		tab.exportGestures()
	})

	tab.resetBtn = widget.NewButtonWithIcon("Reset to Defaults", theme.ViewRefreshIcon(), func() {
		tab.resetToDefaults()
	})
	tab.resetBtn.Importance = widget.WarningImportance

	tab.helpBtn = widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		win := getCurrentWindow()
		if win != nil {
			ShowContextHelp(win, "gestures")
		}
	})

	tab.testBtn = widget.NewButtonWithIcon("Test Gesture", theme.MediaPlayIcon(), func() {
		tab.testGesture()
	})

	tab.statusLabel = widget.NewLabel("")
	tab.statusLabel.Importance = widget.MediumImportance

	// ----- Stats & Instructions -----
	statsCard := tab.createStatsCard()

	instructions := widget.NewRichTextFromMarkdown(
		"## Gesture Workflow\n\n" +
			"1. Select a gesture type from the filter.\n" +
			"2. Add or edit templates to match the motion you want.\n" +
			"3. Use Test Gesture to confirm the mapping.\n" +
			"4. Keep confidence high and actions simple for demos.\n\n" +
			"## Best Practices\n\n" +
			"- Train gestures 5 to 10 times for consistency.\n" +
			"- Use distinct motions for distinct actions.\n" +
			"- Start with media and navigation actions before advanced controls.")

	summaryCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("✋ Gesture Overview", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.overview,
		tab.activeCount,
		widget.NewLabel("Recommended mappings: swipe for navigation, circular motion for volume, pinch for zoom."),
	)))

	controlCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("🧰 Gesture Tools", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewHBox(
			tab.addBtn,
			tab.editBtn,
			tab.deleteBtn,
		),
		container.NewHBox(
			tab.importBtn,
			tab.exportBtn,
			tab.resetBtn,
			tab.helpBtn,
			tab.testBtn,
		),
		tab.statusLabel,
	)))

	// ----- Layout -----
	searchCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("🔎 Search & Filter", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewHBox(
			widget.NewLabel("🔍"),
			tab.searchEntry,
		),
		container.NewHBox(
			widget.NewLabel("Type:"),
			tab.filterType,
		),
	)))

	leftColumn := container.NewVBox(
		summaryCard,
		searchCard,
		controlCard,
		statsCard,
	)

	rightColumn := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("📚 Template Library", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewScroll(tab.list),
	)))

	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		container.NewGridWithColumns(2, leftColumn, rightColumn),
		instructions,
	)

	tab.refreshList()
	return content
}

// getFilteredTemplates returns filtered list based on search and type.
func (t *GesturesTab) getFilteredTemplates() []GestureTemplate {
	searchText := ""
	filterType := "All"
	if t.searchEntry != nil {
		searchText = t.searchEntry.Text
	}
	if t.filterType != nil && t.filterType.Selected != "" {
		filterType = t.filterType.Selected
	}

	t.mu.RLock()
	defer t.mu.RUnlock()
	return filterGestureTemplates(t.templates, searchText, filterType)
}

// filterGestureTemplates filters the templates by search text and type.
func filterGestureTemplates(templates []GestureTemplate, searchText, filterType string) []GestureTemplate {
	filtered := make([]GestureTemplate, 0)
	searchText = strings.ToLower(searchText)

	for _, g := range templates {
		if filterType != "All" && g.Type != filterType {
			continue
		}
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

// refreshList updates the list and buttons.
func (t *GesturesTab) refreshList() {
	if t.list == nil || t.editBtn == nil || t.deleteBtn == nil {
		return
	}
	t.list.Refresh()
	if t.activeCount != nil {
		filtered := t.getFilteredTemplates()
		t.activeCount.SetText(fmt.Sprintf("Active templates shown: %d", len(filtered)))
	}
	t.updateButtons()
}

// updateButtons enables/disables edit/delete based on selection.
func (t *GesturesTab) updateButtons() {
	if t.editBtn == nil || t.deleteBtn == nil {
		return
	}
	filtered := t.getFilteredTemplates()
	hasSelection := t.selected >= 0 && t.selected < len(filtered)
	if hasSelection {
		t.editBtn.Enable()
		t.deleteBtn.Enable()
	} else {
		t.editBtn.Disable()
		t.deleteBtn.Disable()
	}
}

// showAddGestureDialog displays the add gesture dialog.
func (t *GesturesTab) showAddGestureDialog() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
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
			t.mu.Lock()
			t.templates = append(t.templates, newGesture)
			t.mu.Unlock()
			t.refreshList()
			t.setStatus("✓ Added gesture: "+newGesture.Name, widget.SuccessImportance)
		}
	}, win)
}

// showEditGestureDialog displays the edit gesture dialog.
func (t *GesturesTab) showEditGestureDialog() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
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
			t.mu.Lock()
			for i, g := range t.templates {
				if g.Name == gesture.Name && g.Type == gesture.Type {
					t.templates[i].Name = nameEntry.Text
					t.templates[i].Type = typeSelect.Selected
					t.templates[i].Action = actionEntry.Text
					break
				}
			}
			t.mu.Unlock()
			t.refreshList()
			t.setStatus("✓ Updated gesture: "+nameEntry.Text, widget.SuccessImportance)
		}
	}, win)
}

// deleteGesture deletes the selected gesture after confirmation.
func (t *GesturesTab) deleteGesture() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	filtered := t.getFilteredTemplates()
	if t.selected < 0 || t.selected >= len(filtered) {
		return
	}
	gesture := filtered[t.selected]

	dialog.ShowConfirm("Delete Gesture",
		fmt.Sprintf("Are you sure you want to delete '%s'?", gesture.Name),
		func(confirmed bool) {
			if confirmed {
				t.mu.Lock()
				for i, g := range t.templates {
					if g.Name == gesture.Name && g.Type == gesture.Type {
						t.templates = append(t.templates[:i], t.templates[i+1:]...)
						break
					}
				}
				t.mu.Unlock()
				t.selected = -1
				t.refreshList()
				t.setStatus("✓ Deleted gesture: "+gesture.Name, widget.SuccessImportance)
			}
		},
		win)
}

// importGestures loads gesture templates from a JSON file.
func (t *GesturesTab) importGestures() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, win)
			}
			return
		}
		defer reader.Close()

		var imported []GestureTemplate
		decoder := json.NewDecoder(reader)
		if err := decoder.Decode(&imported); err != nil {
			dialog.ShowError(fmt.Errorf("failed to parse JSON: %w", err), win)
			return
		}

		t.mu.Lock()
		t.templates = imported
		t.mu.Unlock()
		t.selected = -1
		t.refreshList()
		t.setStatus(fmt.Sprintf("✓ Imported %d gestures", len(imported)), widget.SuccessImportance)
	}, win)
}

// exportGestures saves gesture templates to a JSON file.
func (t *GesturesTab) exportGestures() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, win)
			}
			return
		}
		defer writer.Close()

		t.mu.RLock()
		data, err := json.MarshalIndent(t.templates, "", "  ")
		t.mu.RUnlock()
		if err != nil {
			dialog.ShowError(fmt.Errorf("failed to marshal: %w", err), win)
			return
		}

		if _, err := writer.Write(data); err != nil {
			dialog.ShowError(fmt.Errorf("failed to write: %w", err), win)
			return
		}
		t.setStatus(fmt.Sprintf("✓ Exported %d gestures", len(t.templates)), widget.SuccessImportance)
	}, win)
}

// resetToDefaults restores the default gesture templates after confirmation.
func (t *GesturesTab) resetToDefaults() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowConfirm("Reset to Defaults",
		"This will replace all current gestures with the default set. Continue?",
		func(confirmed bool) {
			if confirmed {
				t.mu.Lock()
				t.templates = defaultGestures()
				t.mu.Unlock()
				t.selected = -1
				t.refreshList()
				t.setStatus("✓ Reset to default gestures", widget.SuccessImportance)
			}
		},
		win)
}

// testGesture shows information about the selected gesture.
func (t *GesturesTab) testGesture() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if t.selected >= 0 {
		filtered := t.getFilteredTemplates()
		if t.selected >= len(filtered) {
			return
		}
		gesture := filtered[t.selected]
		dialog.ShowInformation("Test Gesture",
			fmt.Sprintf("Testing gesture: %s\nAction: %s\nConfidence: %.0f%%",
				gesture.Name, gesture.Action, gesture.Confidence*100),
			win)
	}
}

// setStatus updates the status label with a message and importance.
func (t *GesturesTab) setStatus(msg string, importance widget.Importance) {
	t.statusLabel.SetText(msg)
	t.statusLabel.Importance = importance
	// Clear after 5 seconds
	time.AfterFunc(5*time.Second, func() {
		RunOnMain(func() {
			if t.statusLabel != nil {
				t.statusLabel.SetText("")
				t.statusLabel.Importance = widget.MediumImportance
			}
		})
	})
}

// createStatsCard returns a live statistics panel.
func (t *GesturesTab) createStatsCard() fyne.CanvasObject {
	totalLabel := widget.NewLabel("Total: 0")
	handLabel := widget.NewLabel("Hand: 0")
	swipeLabel := widget.NewLabel("Swipe: 0")
	circularLabel := widget.NewLabel("Circular: 0")
	pinchLabel := widget.NewLabel("Pinch: 0")

	go func() {
		for {
			// Read templates under lock
			t.mu.RLock()
			templatesCopy := make([]GestureTemplate, len(t.templates))
			copy(templatesCopy, t.templates)
			t.mu.RUnlock()

			handCount, swipeCount, circularCount, pinchCount := 0, 0, 0, 0
			for _, g := range templatesCopy {
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
				totalLabel.SetText(fmt.Sprintf("📊 Total: %d", len(templatesCopy)))
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
