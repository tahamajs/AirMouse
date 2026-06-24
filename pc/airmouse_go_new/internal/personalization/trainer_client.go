package personalization

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// TrainerClient communicates with an external training service.
type TrainerClient struct {
	client      *http.Client
	endpoint    string
	apiKey      string
	timeout     time.Duration
	lastRequest time.Time
}

// FineTuneRequest is the payload sent to the trainer.
type FineTuneRequest struct {
	ModelPath  string                   `json:"model_path"`
	Buffer     []map[string]interface{} `json:"buffer"`
	OutputPath string                   `json:"output_path"`
	Config     map[string]interface{}   `json:"config"`
}

// FineTuneResponse is the response from the trainer.
type FineTuneResponse struct {
	Success      bool    `json:"success"`
	Message      string  `json:"message"`
	Loss         float64 `json:"loss"`
	Accuracy     float64 `json:"accuracy"`
	ModelHash    string  `json:"model_hash"`
	TrainingTime float64 `json:"training_time_seconds"`
}

// NewTrainerClient creates a new trainer client.
func NewTrainerClient(endpoint string) *TrainerClient {
	return &TrainerClient{
		client:   &http.Client{Timeout: 30 * time.Minute},
		endpoint: endpoint,
		timeout:  30 * time.Minute,
	}
}

// SetAPIKey sets the API key for authentication.
func (t *TrainerClient) SetAPIKey(key string) {
	t.apiKey = key
}

// Health checks if the training service is available.
func (t *TrainerClient) Health() error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, t.endpoint+"/health", nil)
	if err != nil {
		return fmt.Errorf("failed to create health request: %w", err)
	}
	resp, err := t.client.Do(req)
	if err != nil {
		return fmt.Errorf("health check failed: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("health check returned status %d", resp.StatusCode)
	}
	return nil
}

// FineTune sends a fine‑tuning request to the training service.
func (t *TrainerClient) FineTune(req *FineTuneRequest) error {
	// Rate limiting: at most one request per second
	if time.Since(t.lastRequest) < time.Second {
		time.Sleep(time.Second - time.Since(t.lastRequest))
	}
	t.lastRequest = time.Now()

	body, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("failed to marshal request: %w", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), t.timeout)
	defer cancel()
	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, t.endpoint+"/fine_tune", bytes.NewBuffer(body))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")
	if t.apiKey != "" {
		httpReq.Header.Set("Authorization", "Bearer "+t.apiKey)
	}

	resp, err := t.client.Do(httpReq)
	if err != nil {
		return fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read response: %w", err)
	}

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("training failed with status %d: %s", resp.StatusCode, string(respBody))
	}

	var trainResp FineTuneResponse
	if err := json.Unmarshal(respBody, &trainResp); err != nil {
		return fmt.Errorf("failed to parse response: %w", err)
	}
	if !trainResp.Success {
		return fmt.Errorf("training failed: %s", trainResp.Message)
	}
	return nil
}

// GetStatus retrieves the current status of the training service.
func (t *TrainerClient) GetStatus() (map[string]interface{}, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, t.endpoint+"/status", nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create status request: %w", err)
	}
	resp, err := t.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get status: %w", err)
	}
	defer resp.Body.Close()

	var status map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&status); err != nil {
		return nil, fmt.Errorf("failed to decode status: %w", err)
	}
	return status, nil
}

// CancelTraining cancels an ongoing training job.
func (t *TrainerClient) CancelTraining() error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, t.endpoint+"/cancel", nil)
	if err != nil {
		return fmt.Errorf("failed to create cancel request: %w", err)
	}
	resp, err := t.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to cancel training: %w", err)
	}
	defer resp.Body.Close()
	return nil
}