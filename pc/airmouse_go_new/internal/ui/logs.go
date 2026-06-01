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
    filter     string
    level      string = "All"
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
    var buf bytes.Buffer
    for _, e := range logEntries {
        // Filter by level
        if level != "All" && e.Level != level {
            continue
        }
        // Filter by text
        if filter != "" && !bytes.Contains([]byte(e.Message), []byte(filter)) {
            continue
        }
        buf.WriteString(e.Time.Format("15:04:05"))
        buf.WriteString(" [")
        buf.WriteString(e.Level)
        buf.WriteString("] ")
        buf.WriteString(e.Message)
        buf.WriteString("\n")
    }
    text := buf.String()
    logMu.RUnlock()
    fyne.Do(func() {
        logWidget.SetText(text)
    })
}

type LogsTab struct{}

func NewLogsTab() fyne.CanvasObject {
    logWidget = widget.NewMultiLineEntry()
    logWidget.Disable()
    logWidget.SetMinRowsVisible(20)

    // Filter UI
    searchEntry := widget.NewEntry()
    searchEntry.SetPlaceHolder("Filter messages...")
    searchEntry.OnChanged = func(s string) {
        filter = s
        refreshLogWidget()
    }

    levelSelect := widget.NewSelect([]string{"All", "INFO", "WARN", "ERROR"}, func(s string) {
        level = s
        refreshLogWidget()
    })
    levelSelect.SetSelected("All")

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
                    _, _ = writer.Write([]byte(line))
                }
            }
        }, fyne.CurrentApp().Driver().AllWindows()[0])
    })

    toolbar := container.NewHBox(
        widget.NewLabel("Filter:"), searchEntry,
        levelSelect,
        clearBtn, exportBtn,
    )
    return container.NewBorder(toolbar, nil, nil, nil, container.NewScroll(logWidget))
}