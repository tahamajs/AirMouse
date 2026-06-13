package personalization

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "time"
)

type TrainerClient struct {
    client      *http.Client
    endpoint    string
    apiKey      string
    timeout     time.Duration
    lastRequest time.Time
}

type FineTuneRequest struct {
    ModelPath   string                   `json:"model_path"`
    Buffer      []map[string]interface{} `json:"buffer"`
    OutputPath  string                   `json:"output_path"`
    Config      map[string]interface{}   `json:"config"`
}

type FineTuneResponse struct {
    Success     bool    `json:"success"`
    Message     string  `json:"message"`
    Loss        float64 `json:"loss"`
    Accuracy    float64 `json:"accuracy"`
    ModelHash   string  `json:"model_hash"`
    TrainingTime float64 `json:"training_time_seconds"`
}

func NewTrainerClient(endpoint string) *TrainerClient {
    return &TrainerClient{
        client:   &http.Client{Timeout: 30 * time.Minute},
        endpoint: endpoint,
        timeout:  30 * time.Minute,
    }
}

func (t *TrainerClient) SetAPIKey(key string) {
    t.apiKey = key
}

func (t *TrainerClient) Health() error {
    resp, err := t.client.Get(t.endpoint + "/health")
    if err != nil {
        return fmt.Errorf("health check failed: %w", err)
    }
    defer resp.Body.Close()
    
    if resp.StatusCode != http.StatusOK {
        return fmt.Errorf("health check returned %d", resp.StatusCode)
    }
    return nil
}

func (t *TrainerClient) FineTune(req *FineTuneRequest) error {
    // Rate limiting
    if time.Since(t.lastRequest) < time.Second {
        time.Sleep(time.Second - time.Since(t.lastRequest))
    }
    t.lastRequest = time.Now()
    
    data, err := json.Marshal(req)
    if err != nil {
        return fmt.Errorf("failed to marshal request: %w", err)
    }
    
    httpReq, err := http.NewRequest("POST", t.endpoint+"/fine_tune", bytes.NewBuffer(data))
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
    
    body, err := io.ReadAll(resp.Body)
    if err != nil {
        return fmt.Errorf("failed to read response: %w", err)
    }
    
    if resp.StatusCode != http.StatusOK {
        return fmt.Errorf("training failed with status %d: %s", resp.StatusCode, string(body))
    }
    
    var trainResp FineTuneResponse
    if err := json.Unmarshal(body, &trainResp); err != nil {
        return fmt.Errorf("failed to parse response: %w", err)
    }
    
    if !trainResp.Success {
        return fmt.Errorf("training failed: %s", trainResp.Message)
    }
    
    return nil
}

func (t *TrainerClient) GetStatus() (map[string]interface{}, error) {
    resp, err := t.client.Get(t.endpoint + "/status")
    if err != nil {
        return nil, err
    }
    defer resp.Body.Close()
    
    var status map[string]interface{}
    if err := json.NewDecoder(resp.Body).Decode(&status); err != nil {
        return nil, err
    }
    
    return status, nil
}

func (t *TrainerClient) CancelTraining() error {
    resp, err := t.client.Post(t.endpoint+"/cancel", "application/json", nil)
    if err != nil {
        return err
    }
    defer resp.Body.Close()
    return nil
}