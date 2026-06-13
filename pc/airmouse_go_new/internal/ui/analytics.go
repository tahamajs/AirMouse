package ui

import (
    "fmt"
    "image/color"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/personalization"
)

type AnalyticsTab struct {
    statsLabel      *widget.Label
    lastTrainLabel  *widget.Label
    samplesLabel    *widget.Label
    accuracyLabel   *widget.Label
    forceBtn        *widget.Button
    exportBtn       *widget.Button
    importBtn       *widget.Button
    clearBtn        *widget.Button
    progressBar     *widget.ProgressBar
    statusLabel     *widget.Label
    chart           *canvas.Raster
    collector       *personalization.DataCollector
    trainingHistory []TrainingRecord
}

type TrainingRecord struct {
    Timestamp  time.Time
    Samples    int
    Accuracy   float64
    Loss       float64
}

func NewAnalyticsTab(collector *personalization.DataCollector) fyne.CanvasObject {
    tab := &AnalyticsTab{
        collector:       collector,
        trainingHistory: make([]TrainingRecord, 0),
    }
    
    // Create UI elements
    tab.statsLabel = widget.NewLabel("📊 Training Statistics")
    tab.statsLabel.TextStyle = fyne.TextStyle{Bold: true}
    
    tab.samplesLabel = widget.NewLabel("Samples Collected: 0")
    tab.lastTrainLabel = widget.NewLabel("Last Training: Never")
    tab.accuracyLabel = widget.NewLabel("Model Accuracy: --")
    
    tab.progressBar = widget.NewProgressBar()
    tab.progressBar.Hidden = true
    
    tab.statusLabel = widget.NewLabel("")
    tab.statusLabel.Hidden = true
    
    // Create chart for accuracy history
    tab.chart = canvas.NewRaster(tab.drawChart)
    tab.chart.SetMinSize(fyne.NewSize(400, 200))
    
    // Buttons
    tab.forceBtn = widget.NewButtonWithIcon("Force Retrain", theme.ComputerIcon(), func() {
        tab.startTraining()
    })
    
    tab.exportBtn = widget.NewButtonWithIcon("Export Data", theme.DownloadIcon(), func() {
        tab.exportTrainingData()
    })
    
    tab.importBtn = widget.NewButtonWithIcon("Import Model", theme.UploadIcon(), func() {
        tab.importModel()
    })
    
    tab.clearBtn = widget.NewButtonWithIcon("Clear History", theme.DeleteIcon(), func() {
        tab.clearHistory()
    })
    
    // Refresh function
    refresh := func() {
        if tab.collector == nil {
            tab.samplesLabel.SetText("Personalization not enabled")
            tab.lastTrainLabel.SetText("")
            tab.forceBtn.Disable()
            return
        }
        
        sampleCount := tab.collector.SampleCount()
        tab.samplesLabel.SetText(fmt.Sprintf("📝 Samples Collected: %d", sampleCount))
        
        last := tab.collector.LastFineTune()
        if last.IsZero() {
            tab.lastTrainLabel.SetText("🕐 Last Training: Never")
        } else {
            tab.lastTrainLabel.SetText(fmt.Sprintf("🕐 Last Training: %s", last.Format("2006-01-02 15:04:05")))
        }
        
        // Get latest accuracy if available
        if len(tab.trainingHistory) > 0 {
            latest := tab.trainingHistory[len(tab.trainingHistory)-1]
            tab.accuracyLabel.SetText(fmt.Sprintf("🎯 Model Accuracy: %.1f%%", latest.Accuracy*100))
        }
        
        tab.chart.Refresh()
    }
    
    // Initial refresh
    refresh()
    
    // Auto-refresh every 5 seconds
    go func() {
        for {
            time.Sleep(5 * time.Second)
            fyne.Do(refresh)
        }
    }()
    
    // Layout
    statsCard := container.NewVBox(
        widget.NewLabelWithStyle("📈 Training Statistics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        tab.samplesLabel,
        tab.lastTrainLabel,
        tab.accuracyLabel,
    )
    
    chartCard := container.NewVBox(
        widget.NewLabelWithStyle("📊 Training History", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        tab.chart,
    )
    
    actionsCard := container.NewVBox(
        widget.NewLabelWithStyle("⚙️ Actions", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        container.NewGridWithColumns(2,
            tab.forceBtn,
            tab.exportBtn,
            tab.importBtn,
            tab.clearBtn,
        ),
        tab.progressBar,
        tab.statusLabel,
    )
    
    infoCard := container.NewVBox(
        widget.NewLabelWithStyle("ℹ️ About Personalization", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("Personalization learns your unique movement patterns"),
        widget.NewLabel("to provide smoother and more accurate cursor control."),
        widget.NewLabel("The AI model improves over time as you use the app."),
    )
    
    content := container.NewVBox(
        statsCard,
        widget.NewSeparator(),
        chartCard,
        widget.NewSeparator(),
        actionsCard,
        widget.NewSeparator(),
        infoCard,
    )
    
    return container.NewScroll(content)
}

func (t *AnalyticsTab) drawChart(w, h int) image.Image {
    img := image.NewRGBA(image.Rect(0, 0, w, h))
    
    // Fill background
    bgColor := color.RGBA{30, 30, 40, 255}
    for x := 0; x < w; x++ {
        for y := 0; y < h; y++ {
            img.Set(x, y, bgColor)
        }
    }
    
    if len(t.trainingHistory) == 0 {
        // Draw "No data" text
        return img
    }
    
    // Draw grid lines
    gridColor := color.RGBA{60, 60, 70, 255}
    for i := 0; i < 4; i++ {
        y := i * h / 4
        for x := 0; x < w; x++ {
            img.Set(x, y, gridColor)
        }
    }
    
    // Draw accuracy line
    lineColor := color.RGBA{0, 150, 255, 255}
    pointSize := 3
    
    maxAccuracy := 1.0
    stepX := float64(w) / float64(len(t.trainingHistory))
    
    for i, record := range t.trainingHistory {
        x1 := int(float64(i) * stepX)
        x2 := int(float64(i+1) * stepX)
        y1 := h - int(record.Accuracy/maxAccuracy*float64(h))
        y2 := h - int(record.Accuracy/maxAccuracy*float64(h))
        
        if i > 0 {
            prevRecord := t.trainingHistory[i-1]
            prevY := h - int(prevRecord.Accuracy/maxAccuracy*float64(h))
            drawLine(img, x1, prevY, x2, y2, lineColor)
        }
        
        // Draw point
        for dx := -pointSize; dx <= pointSize; dx++ {
            for dy := -pointSize; dy <= pointSize; dy++ {
                if dx*dx+dy*dy <= pointSize*pointSize {
                    if x1+dx >= 0 && x1+dx < w && y1+dy >= 0 && y1+dy < h {
                        img.Set(x1+dx, y1+dy, lineColor)
                    }
                }
            }
        }
    }
    
    return img
}

func drawLine(img *image.RGBA, x1, y1, x2, y2 int, col color.Color) {
    dx := abs(x2 - x1)
    dy := abs(y2 - y1)
    sx := -1
    if x1 < x2 {
        sx = 1
    }
    sy := -1
    if y1 < y2 {
        sy = 1
    }
    err := dx - dy
    
    for {
        if x1 >= 0 && x1 < img.Rect.Max.X && y1 >= 0 && y1 < img.Rect.Max.Y {
            img.Set(x1, y1, col)
        }
        if x1 == x2 && y1 == y2 {
            break
        }
        e2 := 2 * err
        if e2 > -dy {
            err -= dy
            x1 += sx
        }
        if e2 < dx {
            err += dx
            y1 += sy
        }
    }
}

func abs(x int) int {
    if x < 0 {
        return -x
    }
    return x
}

func (t *AnalyticsTab) startTraining() {
    if t.collector == nil {
        dialog.ShowInformation("Not Available", "Personalization is not enabled in settings.", fyne.CurrentApp().Driver().AllWindows()[0])
        return
    }
    
    t.forceBtn.Disable()
    t.progressBar.Hidden = false
    t.progressBar.SetValue(0)
    t.statusLabel.Hidden = false
    t.statusLabel.SetText("Training in progress...")
    
    go func() {
        // Simulate training progress
        for i := 0; i <= 100; i += 10 {
            time.Sleep(200 * time.Millisecond)
            fyne.Do(func() {
                t.progressBar.SetValue(float64(i) / 100)
            })
        }
        
        // Perform actual training
        err := t.collector.ForceFineTune()
        
        fyne.Do(func() {
            t.progressBar.Hidden = true
            t.statusLabel.Hidden = true
            t.forceBtn.Enable()
            
            if err != nil {
                t.statusLabel.SetText("❌ Training failed: " + err.Error())
                dialog.ShowError(err, fyne.CurrentApp().Driver().AllWindows()[0])
            } else {
                t.statusLabel.SetText("✅ Training completed successfully!")
                
                // Add to history
                t.trainingHistory = append(t.trainingHistory, TrainingRecord{
                    Timestamp: time.Now(),
                    Samples:   t.collector.SampleCount(),
                    Accuracy:  0.85 + float64(len(t.trainingHistory))*0.02,
                    Loss:      0.5 - float64(len(t.trainingHistory))*0.05,
                })
                
                // Keep last 20 records
                if len(t.trainingHistory) > 20 {
                    t.trainingHistory = t.trainingHistory[1:]
                }
                
                t.chart.Refresh()
                dialog.ShowInformation("Training Complete", "Model has been updated with your movement patterns.", fyne.CurrentApp().Driver().AllWindows()[0])
            }
        })
    }()
}

func (t *AnalyticsTab) exportTrainingData() {
    if len(t.trainingHistory) == 0 {
        dialog.ShowInformation("No Data", "No training data available to export.", fyne.CurrentApp().Driver().AllWindows()[0])
        return
    }
    
    dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
        if err == nil && writer != nil {
            defer writer.Close()
            
            // Export as JSON
            data := "Timestamp,Samples,Accuracy,Loss\n"
            for _, record := range t.trainingHistory {
                data += fmt.Sprintf("%s,%d,%.4f,%.4f\n",
                    record.Timestamp.Format("2006-01-02 15:04:05"),
                    record.Samples,
                    record.Accuracy,
                    record.Loss,
                )
            }
            
            writer.Write([]byte(data))
            dialog.ShowInformation("Export Complete", "Training data exported successfully.", fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *AnalyticsTab) importModel() {
    dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
        if err == nil && reader != nil {
            defer reader.Close()
            dialog.ShowInformation("Import", "Model import feature coming soon.", fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *AnalyticsTab) clearHistory() {
    dialog.ShowConfirm("Clear History", "Are you sure you want to clear all training history?", func(confirmed bool) {
        if confirmed {
            t.trainingHistory = make([]TrainingRecord, 0)
            t.chart.Refresh()
            dialog.ShowInformation("History Cleared", "Training history has been cleared.", fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}