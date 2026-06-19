package websocket

import "encoding/json"

func welcomeMessage(serverName, version string) []byte {
	body, _ := json.Marshal(map[string]any{
		"type":   "welcome",
		"server": serverName,
		"version": version,
	})
	return append(body, '\n')
}

func pongMessage() []byte {
	body, _ := json.Marshal(map[string]any{
		"type": "pong",
	})
	return append(body, '\n')
}
