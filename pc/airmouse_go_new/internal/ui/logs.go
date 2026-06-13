package ui

import (
    "bytes"
    "fmt"
    "sync"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/utils"
)

type LogEntry struct {
    Time    time.Time
    Level   string
    Message string
    Source  string
}

type LogsTab struct {
    logWidget   *widget.Entry
    filterEntry *widget.Entry
    levelSelect *widget.Select
    autoScroll  *widget.Check
    statusLabel *widget.Label
    clearBtn    *widget.Button
    exportBtn   *widget.Button
    pauseBtn    *widget.Button
    copyBtn     *widget.Button
    
    paused      bool
    logMu       sync.RWMutex
    logEntries  []LogEntry
    filter      string
    level       string
}

var (
    globalLogsTab *LogsTab
)

func init() {
    utils.SetLogHook(func(level, msg string) {
        if globalLogsTab != nil {
            globalLogsTab.AddLogEntry(level, msg, "")
        }
    })
}

func NewLogsTab() fyne.CanvasObject {
    tab := &LogsTab{
        logEntries: make([]LogEntry, 0, 1000),
        level:      "All",
        paused:     false,
    }
    
    globalLogsTab = tab
    
    // Main log display
    tab.logWidget = widget.NewMultiLineEntry()
    tab.logWidget.Disable()
    tab.logWidget.SetMinRowsVisible(25)
    tab.logWidget.Wrapping = fyne.TextWrapBreak
    
    // Filter controls
    tab.filterEntry = widget.NewEntry()
    tab.filterEntry.SetPlaceHolder("Search logs...")
    tab.filterEntry.OnChanged = func(s string) {
        tab.filter = s
        tab.refreshDisplay()
    }
    
    // Level filter
    levels := []string{"All", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"}
    tab.levelSelect = widget.NewSelect(levels, func(s string) {
        tab.level = s
        tab.refreshDisplay()
    })
    tab.levelSelect.SetSelected("All")
    
    // Auto-scroll checkbox
    tab.autoScroll = widget.NewCheck("Auto-scroll", func(on bool) {})
    tab.autoScroll.SetChecked(true)
    
    // Status label
    tab.statusLabel = widget.NewLabel("📊 0 entries")
    tab.statusLabel.Importance = widget.MediumImportance
    
    // Buttons
    tab.clearBtn = widget.NewButtonWithIcon("Clear", theme.DeleteIcon(), func() {
        tab.clearLogs()
    })
    
    tab.exportBtn = widget.NewButtonWithIcon("Export", theme.DownloadIcon(), func() {
        tab.exportLogs()
    })
    
    tab.pauseBtn = widget.NewButtonWithIcon("Pause", theme.MediaPauseIcon(), func() {
        tab.togglePause()
    })
    
    tab.copyBtn = widget.NewButtonWithIcon("Copy All", theme.ContentCopyIcon(), func() {
        tab.copyAllLogs()
    })
    
    // Statistics
    statsCard := tab.createStatsCard()
    
    // Toolbar
    toolbar := container.NewVBox(
        container.NewHBox(
            widget.NewLabel("🔍 Filter:"), tab.filterEntry,
            widget.NewLabel("Level:"), tab.levelSelect,
        ),
        container.NewHBox(
            tab.clearBtn,
            tab.exportBtn,
            tab.pauseBtn,
            tab.copyBtn,
            tab.autoScroll,
            tab.statusLabel,
        ),
    )
    
    // Main content
    content := container.NewBorder(
        toolbar,
        statsCard,
        nil, nil,
        container.NewScroll(tab.logWidget),
    )
    
    // Initial refresh
    tab.refreshDisplay()
    
    return content
}

func (t *LogsTab) AddLogEntry(level, message, source string) {
    if t.paused {
        return
    }
    
    t.logMu.Lock()
    defer t.logMu.Unlock()
    
    entry := LogEntry{
        Time:    time.Now(),
        Level:   level,
        Message: message,
        Source:  source,
    }
    
    t.logEntries = append(t.logEntries, entry)
    
    // Keep last 1000 entries
    if len(t.logEntries) > 1000 {
        t.logEntries = t.logEntries[len(t.logEntries)-1000:]
    }
    
    t.refreshDisplay()
}

func (t *LogsTab) refreshDisplay() {
    t.logMu.RLock()
    defer t.logMu.RUnlock()
    
    var buf bytes.Buffer
    filteredCount := 0
    
    for _, entry := range t.logEntries {
        // Level filter
        if t.level != "All" && entry.Level != t.level {
            continue
        }
        
        // Text filter
        if t.filter != "" && !bytes.Contains([]byte(entry.Message), []byte(t.filter)) &&
           !bytes.Contains([]byte(entry.Level), []byte(t.filter)) {
            continue
        }
        
        filteredCount++
        
        // Format with color codes (using emoji indicators)
        levelIcon := getLevelIcon(entry.Level)
        
        // Format line
        line := fmt.Sprintf("%s %s [%s] %s\n",
            entry.Time.Format("15:04:05.000"),
            levelIcon,
            entry.Level,
            entry.Message,
        )
        
        buf.WriteString(line)
    }
    
    // Update status
    t.statusLabel.SetText(fmt.Sprintf("📊 %d / %d entries", filteredCount, len(t.logEntries)))
    
    // Update widget
    t.logWidget.SetText(buf.String())
    
    // Auto-scroll to bottom
    if t.autoScroll.Checked && filteredCount > 0 {
        t.logWidget.CursorRow = len(t.logWidget.Text) - 1
    }
}

func (t *LogsTab) clearLogs() {
    dialog.ShowConfirm("Clear Logs", "Are you sure you want to clear all logs?", func(confirmed bool) {
        if confirmed {
            t.logMu.Lock()
            t.logEntries = make([]LogEntry, 0, 1000)
            t.logMu.Unlock()
            t.refreshDisplay()
            utils.LogInfo("Logs cleared by user")
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *LogsTab) exportLogs() {
    dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
        if err == nil && writer != nil {
            defer writer.Close()
            
            t.logMu.RLock()
            defer t.logMu.RUnlock()
            
            // Write header
            header := fmt.Sprintf("Air Mouse Pro Log Export\n"+
                "Date: %s\n"+
                "Total Entries: %d\n"+
                "----------------------------------------\n\n",
                time.Now().Format("2006-01-02 15:04:05"),
                len(t.logEntries))
            
            writer.Write([]byte(header))
            
            // Write entries
            for _, entry := range t.logEntries {
                line := fmt.Sprintf("%s [%s] %s\n",
                    entry.Time.Format("2006-01-02 15:04:05.000"),
                    entry.Level,
                    entry.Message)
                writer.Write([]byte(line))
            }
            
            dialog.ShowInformation("Export Complete", 
                fmt.Sprintf("Exported %d log entries", len(t.logEntries)), 
                fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *LogsTab) togglePause() {
    t.paused = !t.paused
    if t.paused {
        t.pauseBtn.SetIcon(theme.MediaPlayIcon())
        t.pauseBtn.SetText("Resume")
        utils.LogInfo("Log streaming paused")
    } else {
        t.pauseBtn.SetIcon(theme.MediaPauseIcon())
        t.pauseBtn.SetText("Pause")
        utils.LogInfo("Log streaming resumed")
        t.refreshDisplay()
    }
}

func (t *LogsTab) copyAllLogs() {
    t.logMu.RLock()
    defer t.logMu.RUnlock()
    
    var buf bytes.Buffer
    for _, entry := range t.logEntries {
        line := fmt.Sprintf("%s [%s] %s\n",
            entry.Time.Format("2006-01-02 15:04:05"),
            entry.Level,
            entry.Message)
        buf.WriteString(line)
    }
    
    clipboard := fyne.CurrentApp().Driver().AllWindows()[0].Clipboard()
    clipboard.SetContent(buf.String())
    
    dialog.ShowInformation("Copied", "All logs copied to clipboard", 
        fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *LogsTab) createStatsCard() fyne.CanvasObject {
    // Statistics display
    debugCount := widget.NewLabel("DEBUG: 0")
    infoCount := widget.NewLabel("INFO: 0")
    warnCount := widget.NewLabel("WARN: 0")
    errorCount := widget.NewLabel("ERROR: 0")
    
    // Update stats periodically
    go func() {
        for {
            time.Sleep(1 * time.Second)
            t.logMu.RLock()
            
            var debug, info, warn, errCount int
            for _, entry := range t.logEntries {
                switch entry.Level {
                case "DEBUG":
                    debug++
                case "INFO":
                    info++
                case "WARN":
                    warn++
                case "ERROR":
                    errCount++
                }
            }
            
            fyne.Do(func() {
                debugCount.SetText(fmt.Sprintf("🔍 DEBUG: %d", debug))
                infoCount.SetText(fmt.Sprintf("ℹ️ INFO: %d", info))
                warnCount.SetText(fmt.Sprintf("⚠️ WARN: %d", warn))
                errorCount.SetText(fmt.Sprintf("❌ ERROR: %d", errCount))
            })
            
            t.logMu.RUnlock()
        }
    }()
    
    return container.NewHBox(
        debugCount,
        infoCount,
        warnCount,
        errorCount,
    )
}

func getLevelIcon(level string) string {
    switch level {
    case "DEBUG":
        return "🔍"
    case "INFO":
        return "ℹ️"
    case "WARN":
        return "⚠️"
    case "ERROR":
        return "❌"
    case "FATAL":
        return "💀"
    default:
        return "📝"
    }
}