package ui

import (
	"bytes"
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	infraLogger "airmouse-go/internal/infra/logger"
	"airmouse-go/internal/utils"
)

// LogEntry represents a single log line.
type LogEntry struct {
	Time    time.Time
	Level   string
	Message string
	Source  string
}

// LogsTab provides a live log viewer with filtering and export.
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
	helpBtn     *widget.Button

	paused     atomic.Bool // safe for concurrent access
	logMu      sync.RWMutex
	logEntries []LogEntry
	filter     string
	level      string
}

var (
	globalLogsTab *LogsTab
	pendingLogsMu sync.Mutex
	pendingLogs   []LogEntry
)

func init() {
	utils.SetLogHook(func(level, msg string) {
		handleLogHook(level, msg)
	})
	infraLogger.AddHook(func(level infraLogger.Level, msg string) {
		handleLogHook(infraLoggerLevelToString(level), msg)
	})
}

func handleLogHook(level, msg string) {
	if globalLogsTab != nil {
		globalLogsTab.AddLogEntry(level, msg, "")
		return
	}
	pendingLogsMu.Lock()
	pendingLogs = append(pendingLogs, LogEntry{
		Time:    time.Now(),
		Level:   level,
		Message: msg,
	})
	pendingLogsMu.Unlock()
}

func drainPendingLogs(tab *LogsTab) {
	pendingLogsMu.Lock()
	defer pendingLogsMu.Unlock()
	if len(pendingLogs) == 0 {
		return
	}
	tab.logMu.Lock()
	tab.logEntries = append(tab.logEntries, pendingLogs...)
	if len(tab.logEntries) > 1000 {
		tab.logEntries = tab.logEntries[len(tab.logEntries)-1000:]
	}
	tab.logMu.Unlock()
	pendingLogs = nil
}

func infraLoggerLevelToString(level infraLogger.Level) string {
	switch level {
	case infraLogger.LevelDebug:
		return "DEBUG"
	case infraLogger.LevelInfo:
		return "INFO"
	case infraLogger.LevelWarn:
		return "WARN"
	case infraLogger.LevelError:
		return "ERROR"
	default:
		return "FATAL"
	}
}

// NewLogsTab creates a new logs management tab.
func NewLogsTab() fyne.CanvasObject {
	tab := &LogsTab{
		logEntries: make([]LogEntry, 0, 1000),
		level:      "All",
	}
	tab.paused.Store(false)

	globalLogsTab = tab
	drainPendingLogs(tab)
	utils.LogInfo("Logs tab initialized")

	// ----- Main log display -----
	tab.logWidget = widget.NewMultiLineEntry()
	tab.logWidget.Disable()
	tab.logWidget.SetMinRowsVisible(32)
	tab.logWidget.Wrapping = fyne.TextWrapBreak

	// ----- Filter controls -----
	tab.filterEntry = widget.NewEntry()
	tab.filterEntry.SetPlaceHolder("Search logs...")
	tab.filterEntry.OnChanged = func(s string) {
		tab.filter = s
		tab.refreshDisplay()
	}
	tab.filterEntry.ToolTip = "Filter log messages by text"

	// Level filter
	levels := []string{"All", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"}
	tab.levelSelect = widget.NewSelect(levels, nil)
	tab.levelSelect.SetSelected("All")
	tab.levelSelect.OnChanged = func(s string) {
		tab.level = s
		tab.refreshDisplay()
	}
	tab.levelSelect.ToolTip = "Filter by log level"

	// ----- Auto-scroll checkbox -----
	tab.autoScroll = widget.NewCheck("Auto-scroll", func(on bool) {
		tab.refreshDisplay()
	})
	tab.autoScroll.SetChecked(true)
	tab.autoScroll.ToolTip = "Automatically scroll to the bottom when new logs arrive"

	// ----- Status label -----
	tab.statusLabel = widget.NewLabel("📊 0 entries")
	tab.statusLabel.Importance = widget.MediumImportance

	// ----- Buttons with tooltips -----
	tab.clearBtn = widget.NewButtonWithIcon("Clear", theme.DeleteIcon(), func() {
		tab.clearLogs()
	})
	tab.clearBtn.ToolTip = "Clear all log entries (confirmation required)"

	tab.exportBtn = widget.NewButtonWithIcon("Export", theme.DownloadIcon(), func() {
		tab.exportLogs()
	})
	tab.exportBtn.ToolTip = "Export all logs to a text file"

	tab.pauseBtn = widget.NewButtonWithIcon("Pause", theme.MediaPauseIcon(), func() {
		tab.togglePause()
	})
	tab.pauseBtn.ToolTip = "Pause/Resume live log streaming"

	tab.copyBtn = widget.NewButtonWithIcon("Copy All", theme.ContentCopyIcon(), func() {
		tab.copyAllLogs()
	})
	tab.copyBtn.ToolTip = "Copy all visible logs to the clipboard"

	tab.helpBtn = widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		win := getCurrentWindow()
		if win != nil {
			ShowContextHelp(win, "logs")
		}
	})
	tab.helpBtn.ToolTip = "Show help for the Logs tab"

	// ----- Statistics card -----
	statsCard := tab.createStatsCard()

	// ----- Toolbar -----
	title := widget.NewLabelWithStyle("🧾 Live Log Stream", fyne.TextAlignLeading, fyne.TextStyle{Bold: true})

	toolbar := container.NewVBox(
		title,
		container.NewHBox(
			widget.NewLabel("🔍 Filter:"), tab.filterEntry,
			widget.NewLabel("Level:"), tab.levelSelect,
		),
		container.NewHBox(
			tab.clearBtn,
			tab.exportBtn,
			tab.pauseBtn,
			tab.copyBtn,
			tab.helpBtn,
			tab.autoScroll,
			tab.statusLabel,
		),
	)

	// ----- Main content -----
	content := container.NewBorder(
		toolbar,
		statsCard,
		nil, nil,
		container.NewScroll(tab.logWidget),
	)

	// ----- Initial refresh -----
	tab.refreshDisplay()

	return content
}

// AddLogEntry adds a new log entry (called from the global hook).
func (t *LogsTab) AddLogEntry(level, message, source string) {
	if t.paused.Load() {
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

	if len(t.logEntries) > 1000 {
		t.logEntries = t.logEntries[len(t.logEntries)-1000:]
	}

	RunOnMain(func() {
		t.refreshDisplay()
	})
}

// refreshDisplay updates the log view with current filters.
func (t *LogsTab) refreshDisplay() {
	if t.logWidget == nil || t.statusLabel == nil {
		return
	}

	t.logMu.RLock()
	entries := make([]LogEntry, len(t.logEntries))
	copy(entries, t.logEntries)
	filterText := t.filter
	level := t.level
	autoScroll := t.autoScroll != nil && t.autoScroll.Checked
	t.logMu.RUnlock()

	var buf bytes.Buffer
	filteredCount := 0

	for _, entry := range entries {
		if level != "All" && entry.Level != level {
			continue
		}
		if filterText != "" && !bytes.Contains([]byte(entry.Message), []byte(filterText)) &&
			!bytes.Contains([]byte(entry.Level), []byte(filterText)) {
			continue
		}
		filteredCount++
		levelIcon := getLevelIcon(entry.Level)
		line := fmt.Sprintf("%s %s [%s] %s\n",
			entry.Time.Format("15:04:05.000"),
			levelIcon,
			entry.Level,
			entry.Message,
		)
		buf.WriteString(line)
	}

	t.statusLabel.SetText(fmt.Sprintf("📊 %d / %d entries", filteredCount, len(entries)))
	t.logWidget.SetText(buf.String())

	if autoScroll && filteredCount > 0 {
		t.logWidget.CursorRow = len(t.logWidget.Text) - 1
	}
}

// clearLogs clears all log entries after confirmation.
func (t *LogsTab) clearLogs() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowConfirm("Clear Logs", "Are you sure you want to clear all logs?", func(confirmed bool) {
		if confirmed {
			t.logMu.Lock()
			t.logEntries = make([]LogEntry, 0, 1000)
			t.logMu.Unlock()
			t.refreshDisplay()
			t.setStatus("✅ Logs cleared", widget.SuccessImportance)
			utils.LogInfo("Logs cleared by user")
		}
	}, win)
}

// exportLogs saves all logs to a file.
func (t *LogsTab) exportLogs() {
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

		t.logMu.RLock()
		defer t.logMu.RUnlock()

		header := fmt.Sprintf("Air Mouse Pro Log Export\n"+
			"Date: %s\n"+
			"Total Entries: %d\n"+
			"----------------------------------------\n\n",
			time.Now().Format("2006-01-02 15:04:05"),
			len(t.logEntries))
		if _, err := writer.Write([]byte(header)); err != nil {
			dialog.ShowError(err, win)
			return
		}

		for _, entry := range t.logEntries {
			line := fmt.Sprintf("%s [%s] %s\n",
				entry.Time.Format("2006-01-02 15:04:05.000"),
				entry.Level,
				entry.Message)
			if _, err := writer.Write([]byte(line)); err != nil {
				dialog.ShowError(err, win)
				return
			}
		}

		t.setStatus(fmt.Sprintf("✅ Exported %d log entries", len(t.logEntries)), widget.SuccessImportance)
		dialog.ShowInformation("Export Complete",
			fmt.Sprintf("Exported %d log entries", len(t.logEntries)),
			win)
		utils.LogInfo("Logs exported: %d entries", len(t.logEntries))
	}, win)
}

// togglePause pauses/resumes log streaming.
func (t *LogsTab) togglePause() {
	newState := !t.paused.Load()
	t.paused.Store(newState)

	if newState {
		t.pauseBtn.SetIcon(theme.MediaPlayIcon())
		t.pauseBtn.SetText("Resume")
		t.setStatus("⏸️ Log streaming paused", widget.WarningImportance)
		utils.LogInfo("Log streaming paused")
	} else {
		t.pauseBtn.SetIcon(theme.MediaPauseIcon())
		t.pauseBtn.SetText("Pause")
		t.setStatus("▶️ Log streaming resumed", widget.SuccessImportance)
		utils.LogInfo("Log streaming resumed")
		RunOnMain(func() {
			t.refreshDisplay()
		})
	}
}

// copyAllLogs copies all logs to the clipboard.
func (t *LogsTab) copyAllLogs() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
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

	clipboard := win.Clipboard()
	clipboard.SetContent(buf.String())

	t.setStatus("✅ Logs copied to clipboard", widget.SuccessImportance)
	dialog.ShowInformation("Copied", "All logs copied to clipboard", win)
}

// setStatus updates the status label with a message and auto‑clears after 5 seconds.
func (t *LogsTab) setStatus(msg string, importance widget.Importance) {
	t.statusLabel.SetText(msg)
	t.statusLabel.Importance = importance
	time.AfterFunc(5*time.Second, func() {
		RunOnMain(func() {
			if t.statusLabel != nil {
				// Restore the entry count display
				t.refreshDisplay()
			}
		})
	})
}

// createStatsCard returns a panel with live log‑level statistics.
func (t *LogsTab) createStatsCard() fyne.CanvasObject {
	debugCount := widget.NewLabel("🔍 DEBUG: 0")
	infoCount := widget.NewLabel("ℹ️ INFO: 0")
	warnCount := widget.NewLabel("⚠️ WARN: 0")
	errorCount := widget.NewLabel("❌ ERROR: 0")

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
			t.logMu.RUnlock()

			RunOnMain(func() {
				debugCount.SetText(fmt.Sprintf("🔍 DEBUG: %d", debug))
				infoCount.SetText(fmt.Sprintf("ℹ️ INFO: %d", info))
				warnCount.SetText(fmt.Sprintf("⚠️ WARN: %d", warn))
				errorCount.SetText(fmt.Sprintf("❌ ERROR: %d", errCount))
			})
		}
	}()

	return container.NewHBox(
		debugCount,
		infoCount,
		warnCount,
		errorCount,
	)
}

// getLevelIcon returns an emoji icon for a given log level.
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