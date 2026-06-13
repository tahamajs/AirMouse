package predictiveml

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "sync"
    "time"
)

type TrainerClient struct {
    serverURL   string
    client      *http.Client
    apiKey      string
    mu          sync.RWMutex
    lastTrain   time.Time
    trainCooldown time.Duration
}

type TrainRequest struct {
    Samples   []TrainingSample `json:"samples"`
    ModelPath string           `json:"model_path"`
    Config    map[string]interface{} `json:"config"`
}

type TrainResponse struct {
    Success   bool    `json:"success"`
    Message   string  `json:"message"`
    Loss      float64 `json:"loss"`
    ModelHash string  `json:"model_hash"`
}

func NewTrainerClient(serverURL, apiKey string) *TrainerClient {
    return &TrainerClient{
        serverURL:     serverURL,
        client:        &http.Client{Timeout: 30 * time.Second},
        apiKey:        apiKey,
        trainCooldown: 30 * time.Second,
    }
}

func (tc *TrainerClient) SendTrainingData(samples []TrainingSample) error {
    tc.mu.Lock()
    defer tc.mu.Unlock()
    
    if time.Since(tc.lastTrain) < tc.trainCooldown {
        return fmt.Errorf("training on cooldown, try again later")
    }
    
    req := TrainRequest{
        Samples:   samples,
        ModelPath: "",
        Config: map[string]interface{}{
            "epochs":      10,
            "batch_size":  32,
            "learning_rate": 0.001,
        },
    }
    
    data, err := json.Marshal(req)
    if err != nil {
        return fmt.Errorf("failed to marshal request: %w", err)
    }
    
    httpReq, err := http.NewRequest("POST", tc.serverURL+"/train", bytes.NewReader(data))
    if err != nil {
        return err
    }
    
    httpReq.Header.Set("Content-Type", "application/json")
    httpReq.Header.Set("Authorization", "Bearer "+tc.apiKey)
    
    resp, err := tc.client.Do(httpReq)
    if err != nil {
        return err
    }
    defer resp.Body.Close()
    
    body, err := io.ReadAll(resp.Body)
    if err != nil {
        return err
    }
    
    var trainResp TrainResponse
    if err := json.Unmarshal(body, &trainResp); err != nil {
        return err
    }
    
    if !trainResp.Success {
        return fmt.Errorf("training failed: %s", trainResp.Message)
    }
    
    tc.lastTrain = time.Now()
    return nil
}

func (tc *TrainerClient) GetModelStatus() (map[string]interface{}, error) {
    resp, err := tc.client.Get(tc.serverURL + "/status")
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