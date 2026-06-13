package websocket

import (
	"encoding/json"
	"fmt"
)

func decodeWireMessage(line []byte) (msgType string, payload map[string]any, id *string, err error) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(line, &raw); err != nil {
		return "", nil, nil, err
	}

	if t, ok := raw["type"]; ok {
		if err := json.Unmarshal(t, &msgType); err != nil {
			return "", nil, nil, err
		}
	}

	payload = map[string]any{}
	if p, ok := raw["payload"]; ok {
		if err := json.Unmarshal(p, &payload); err != nil {
			return "", nil, nil, err
		}
	} else {
		for k, v := range raw {
			if k == "type" || k == "id" {
				continue
			}
			var value any
			if err := json.Unmarshal(v, &value); err != nil {
				continue
			}
			payload[k] = value
		}
	}

	if rawID, ok := raw["id"]; ok {
		if s, err := rawMessageToString(rawID); err == nil {
			id = &s
		}
	}

	return msgType, payload, id, nil
}

func rawMessageToString(raw json.RawMessage) (string, error) {
	var s string
	if err := json.Unmarshal(raw, &s); err == nil {
		return s, nil
	}
	var n json.Number
	if err := json.Unmarshal(raw, &n); err == nil {
		return n.String(), nil
	}
	var i int64
	if err := json.Unmarshal(raw, &i); err == nil {
		return fmt.Sprintf("%d", i), nil
	}
	return "", fmt.Errorf("unsupported id format")
}

func ackMessage(id *string) []byte {
	if id == nil || *id == "" {
		return nil
	}
	body, _ := json.Marshal(map[string]any{
		"type": "ack",
		"id":   *id,
	})
	return append(body, '\n')
}

func number(v any) float64 {
	switch t := v.(type) {
	case float64:
		return t
	case float32:
		return float64(t)
	case int:
		return float64(t)
	case int64:
		return float64(t)
	case json.Number:
		f, _ := t.Float64()
		return f
	default:
		return 0
	}
}
