package server

import (
	"time"

	"airmouse-go/internal/device"
)

// ClientInfo provides metadata and status about a connected AirMouse client.
type ClientInfo struct {
	ID             string    `json:"id"`
	Fingerprint    string    `json:"fingerprint,omitempty"`
	Name           string    `json:"name"`
	Type           string    `json:"type"` // "TCP", "WebSocket", "Bluetooth", "USB"
	Status         string    `json:"status"`
	ConnectedAt    time.Time `json:"connected_at"`
	LastActive     time.Time `json:"last_active"`
	BytesSent      int64     `json:"bytes_sent"`
	BytesRecv      int64     `json:"bytes_recv"`
	MessagesSent   int64     `json:"messages_sent"`
	MessagesRecv   int64     `json:"messages_recv"`
	IPAddress      string    `json:"ip_address,omitempty"`
	DeviceModel    string    `json:"device_model,omitempty"`
	AndroidVersion string    `json:"android_version,omitempty"`
}

// fromDeviceInfo converts internal device info to public ClientInfo.
func fromDeviceInfo(dev *device.DeviceInfo) ClientInfo {
	if dev == nil {
		return ClientInfo{}
	}
	return ClientInfo{
		ID:             dev.ID,
		Fingerprint:    dev.Fingerprint,
		Name:           dev.Name,
		Type:           string(dev.Type),
		Status:         string(dev.Status),
		ConnectedAt:    dev.ConnectedAt,
		LastActive:     dev.LastActive,
		BytesSent:      dev.BytesSent,
		BytesRecv:      dev.BytesRecv,
		MessagesSent:   dev.MessagesSent,
		MessagesRecv:   dev.MessagesRecv,
		IPAddress:      dev.IPAddress,
		DeviceModel:    dev.DeviceModel,
		AndroidVersion: dev.AndroidVersion,
	}
}
