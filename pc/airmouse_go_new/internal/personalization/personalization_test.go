package personalization

import (
	"os"
	"path/filepath"
	"testing"
)

func TestDataCollector(t *testing.T) {
	dc := NewDataCollector()
	if dc == nil {
		t.Fatal("Failed to create DataCollector")
	}

	for i := 0; i < 20; i++ {
		dc.AddSample(float64(i), float64(i*2), 10.0, 20.0)
	}

	if dc.SampleCount() != 20 {
		t.Errorf("Expected 20 samples, got %d", dc.SampleCount())
	}
	if dc.TotalSamples() != 20 {
		t.Errorf("Expected 20 total samples, got %d", dc.TotalSamples())
	}

	samples := dc.GetBuffer()
	if len(samples) != 20 {
		t.Errorf("Expected buffer length 20, got %d", len(samples))
	}

	stats := dc.GetStatistics()
	if stats == nil {
		t.Errorf("Expected non-nil statistics")
	}

	// Test Export and Import
	tmpDir := t.TempDir()
	exportPath := filepath.Join(tmpDir, "samples.json")
	if err := dc.ExportData(exportPath); err != nil {
		t.Fatalf("ExportData failed: %v", err)
	}

	if _, err := os.Stat(exportPath); err != nil {
		t.Fatalf("Exported file missing: %v", err)
	}

	dc.Clear()
	if dc.SampleCount() != 0 {
		t.Errorf("Expected 0 samples after clear, got %d", dc.SampleCount())
	}

	if err := dc.ImportData(exportPath); err != nil {
		t.Fatalf("ImportData failed: %v", err)
	}
	if dc.SampleCount() != 20 {
		t.Errorf("Expected 20 samples after import, got %d", dc.SampleCount())
	}
}

func TestTrainerClient(t *testing.T) {
	client := NewTrainerClient("http://127.0.0.1:9999")
	if client == nil {
		t.Fatal("Failed to create TrainerClient")
	}

	client.SetAPIKey("test-api-key")
	_ = client.Health() // Expect error or graceful handle since dummy endpoint
}
