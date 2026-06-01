//go:build gui

package ui

import (
	"bytes"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/utils"
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
	utils.SetLogHook(func(level, msg string) {
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

type LogsTab struct {
	root fyne.CanvasObject
}

func NewLogsTab() fyne.CanvasObject {
	logWidget = widget.NewMultiLineEntry()
	logWidget.Disable()
	logWidget.SetMinRowsVisible(20)

	clearBtn := widget.NewButton("Clear Logs", func() {
		logMu.Lock()
		logEntries = nil
		logMu.Unlock()
		logWidget.SetText("")
	})
	exportBtn := widget.NewButton("Export Logs", func() {
		if app := fyne.CurrentApp(); app != nil && len(app.Driver().AllWindows()) > 0 {
			dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
				if err == nil && writer != nil {
					defer writer.Close()
					logMu.RLock()
					defer logMu.RUnlock()
					for _, e := range logEntries {
						line := e.Time.Format("2006-01-02 15:04:05") + " [" + e.Level + "] " + e.Message + "\n"
						_, _ = writer.Write([]byte(line))
					}
				}
			}, app.Driver().AllWindows()[0])
		}
	})

	return container.NewBorder(
		container.NewHBox(clearBtn, exportBtn),
		nil, nil, nil,
		container.NewScroll(logWidget),
	)
}

func (l *LogsTab) Clear() {
	logMu.Lock()
	logEntries = nil
	logMu.Unlock()
	if logWidget != nil {
		logWidget.SetText("")
	}
}
