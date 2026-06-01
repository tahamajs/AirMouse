package personalization

import (
    "bytes"
    "encoding/json"
    "net/http"
    "time"
)

type TrainerClient struct {
    client   *http.Client
    endpoint string
}

func NewTrainerClient(endpoint string) *TrainerClient {
    return &TrainerClient{
        client:   &http.Client{Timeout: 10 * time.Minute},
        endpoint: endpoint,
    }
}

func (t *TrainerClient) Health() error {
    resp, err := t.client.Get(t.endpoint + "/health")
    if err != nil {
        return err
    }
    defer resp.Body.Close()
    return nil
}

type FineTuneRequest struct {
    ModelPath  string                   `json:"model_path"`
    Buffer     []map[string]interface{} `json:"buffer"`
    OutputPath string                   `json:"output_path"`
}

func (t *TrainerClient) FineTune(req *FineTuneRequest) error {
    data, err := json.Marshal(req)
    if err != nil {
        return err
    }
    resp, err := t.client.Post(t.endpoint+"/fine_tune", "application/json", bytes.NewBuffer(data))
    if err != nil {
        return err
    }
    defer resp.Body.Close()
    return nil
}