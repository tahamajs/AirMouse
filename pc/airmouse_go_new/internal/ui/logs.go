package ui

import (
	"bytes"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/infra/logger"
)

type LogEntry struct {
	Time    time.Time
	Level   string
	Message string
}

var (
	logMu      sync.RWMutex
	logEntries []LogEntry
	logWidget  *widget.Entry
)

func init() {
	logger.SetHook(func(level, msg string) {
		logMu.Lock()
		defer logMu.Unlock()
		logEntries = append(logEntries, LogEntry{Time: time.Now(), Level: level, Message: msg})
		if len(logEntries) > 500 {
			logEntries = logEntries[len(logEntries)-500:]
		}
		refreshLogWidget()
	})
}

func refreshLogWidget() {
	if logWidget == nil {
		return
	}
	logMu.RLock()
	defer logMu.RUnlock()
	var buf bytes.Buffer
	for _, e := range logEntries {
		buf.WriteString(e.Time.Format("15:04:05"))
		buf.WriteString(" [")
		buf.WriteString(e.Level)
		buf.WriteString("] ")
		buf.WriteString(e.Message)
		buf.WriteString("\n")
	}
	logWidget.SetText(buf.String())
}

type LogsTab struct{}

func NewLogsTab() fyne.CanvasObject {
	logWidget = widget.NewMultiLineEntry()
	logWidget.Disable()
	logWidget.SetMinRowsVisible(20)

	searchEntry := widget.NewEntry()
	searchEntry.SetPlaceHolder("Filter...")

	levelSelect := widget.NewSelect([]string{"All", "INFO", "WARN", "ERROR"}, func(selected string) {
		filterLogs(selected, searchEntry.Text)
	})
	levelSelect.SetSelected("All")

	searchEntry.OnChanged = func(text string) {
		filterLogs(levelSelect.Selected, text)
	}

	clearBtn := widget.NewButton("Clear Logs", func() {
		logMu.Lock()
		logEntries = nil
		logMu.Unlock()
		logWidget.SetText("")
	})
	exportBtn := widget.NewButton("Export Logs", func() {
		dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
			if err == nil && writer != nil {
				defer writer.Close()
				logMu.RLock()
				defer logMu.RUnlock()
				for _, e := range logEntries {
					line := e.Time.Format("2006-01-02 15:04:05") + " [" + e.Level + "] " + e.Message + "\n"
					writer.Write([]byte(line))
				}
			}
		}, fyne.CurrentApp().Driver().AllWindows()[0])
	})

	filterLogs := func(level, search string) {
		logMu.RLock()
		defer logMu.RUnlock()
		var buf bytes.Buffer
		for _, e := range logEntries {
			if level != "All" && e.Level != level {
				continue
			}
			if search != "" && !bytes.Contains([]byte(e.Message), []byte(search)) {
				continue
			}
			buf.WriteString(e.Time.Format("15:04:05"))
			buf.WriteString(" [")
			buf.WriteString(e.Level)
			buf.WriteString("] ")
			buf.WriteString(e.Message)
			buf.WriteString("\n")
		}
		logWidget.SetText(buf.String())
	}

	toolbar := container.NewHBox(searchEntry, levelSelect, clearBtn, exportBtn)
	return container.NewBorder(toolbar, nil, nil, nil, container.NewScroll(logWidget))
}