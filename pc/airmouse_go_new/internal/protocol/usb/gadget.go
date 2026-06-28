package usb

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"airmouse-go/internal/utils"
)

// USBGadget represents a Linux USB gadget configuration
type USBGadget struct {
	enabled      bool
	gadgetDir    string
	vendorID     string
	productID    string
	manufacturer string
	product      string
	serial       string
	configIndex  int
}

// GadgetConfig holds USB gadget configuration
type GadgetConfig struct {
	VendorID     string
	ProductID    string
	Manufacturer string
	Product      string
	Serial       string
	MaxPower     int
}

// DefaultGadgetConfig returns default configuration
func DefaultGadgetConfig() GadgetConfig {
	return GadgetConfig{
		VendorID:     "0x1d6b",
		ProductID:    "0x0104",
		Manufacturer: "AirMouse",
		Product:      "AirMouse HID",
		Serial:       "1234567890",
		MaxPower:     100,
	}
}

// NewUSBGadget creates a new USB gadget
func NewUSBGadget(cfg GadgetConfig) *USBGadget {
	return &USBGadget{
		gadgetDir:    "/sys/kernel/config/usb_gadget/airmouse",
		vendorID:     cfg.VendorID,
		productID:    cfg.ProductID,
		manufacturer: cfg.Manufacturer,
		product:      cfg.Product,
		serial:       cfg.Serial,
		configIndex:  1,
	}
}

// Setup configures the USB gadget as a HID mouse
func (g *USBGadget) Setup() error {
	// Check if configfs is available
	if _, err := os.Stat("/sys/kernel/config/usb_gadget"); os.IsNotExist(err) {
		return fmt.Errorf("configfs not available")
	}

	// Clean up any existing gadget
	g.Teardown()

	// Create gadget directory
	if err := os.MkdirAll(g.gadgetDir, 0755); err != nil {
		return fmt.Errorf("failed to create gadget dir: %w", err)
	}

	// Set USB IDs
	if err := g.writeFile(filepath.Join(g.gadgetDir, "idVendor"), g.vendorID); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(g.gadgetDir, "idProduct"), g.productID); err != nil {
		return err
	}

	// Set device descriptors
	stringsDir := filepath.Join(g.gadgetDir, "strings/0x409")
	if err := os.MkdirAll(stringsDir, 0755); err != nil {
		return err
	}

	if err := g.writeFile(filepath.Join(stringsDir, "manufacturer"), g.manufacturer); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(stringsDir, "product"), g.product); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(stringsDir, "serialnumber"), g.serial); err != nil {
		return err
	}

	// Create configuration
	configDir := filepath.Join(g.gadgetDir, fmt.Sprintf("configs/c.%d", g.configIndex))
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return err
	}

	// Set configuration strings
	configStringsDir := filepath.Join(configDir, "strings/0x409")
	if err := os.MkdirAll(configStringsDir, 0755); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(configStringsDir, "configuration"), "HID Mouse"); err != nil {
		return err
	}

	// Set max power
	if err := g.writeFile(filepath.Join(configDir, "MaxPower"), "100"); err != nil {
		return err
	}

	// Create HID function
	hidDir := filepath.Join(g.gadgetDir, "functions/hid.usb0")
	if err := os.MkdirAll(hidDir, 0755); err != nil {
		return err
	}

	// Configure HID
	if err := g.writeFile(filepath.Join(hidDir, "protocol"), "2"); err != nil { // Mouse protocol
		return err
	}
	if err := g.writeFile(filepath.Join(hidDir, "subclass"), "1"); err != nil { // Boot interface
		return err
	}
	if err := g.writeFile(filepath.Join(hidDir, "report_length"), "8"); err != nil { // 8-byte reports
		return err
	}

	// Write HID report descriptor
	reportDesc := []byte{
		0x05, 0x01, // Usage Page (Generic Desktop)
		0x09, 0x02, // Usage (Mouse)
		0xA1, 0x01, // Collection (Application)
		0x09, 0x01, //   Usage (Pointer)
		0xA1, 0x00, //   Collection (Physical)
		0x05, 0x09, //     Usage Page (Button)
		0x19, 0x01, //     Usage Minimum (1)
		0x29, 0x03, //     Usage Maximum (3)
		0x15, 0x00, //     Logical Minimum (0)
		0x25, 0x01, //     Logical Maximum (1)
		0x95, 0x03, //     Report Count (3)
		0x75, 0x01, //     Report Size (1)
		0x81, 0x02, //     Input (Data, Var, Abs)
		0x95, 0x01, //     Report Count (1)
		0x75, 0x05, //     Report Size (5)
		0x81, 0x01, //     Input (Const)
		0x05, 0x01, //     Usage Page (Generic Desktop)
		0x09, 0x30, //     Usage (X)
		0x09, 0x31, //     Usage (Y)
		0x16, 0x01, 0x80, // Logical Minimum (-32767)
		0x26, 0xFF, 0x7F, // Logical Maximum (32767)
		0x75, 0x10, //     Report Size (16)
		0x95, 0x02, //     Report Count (2)
		0x81, 0x06, //     Input (Data, Var, Rel)
		0x09, 0x38, //     Usage (Wheel)
		0x15, 0x81, //     Logical Minimum (-127)
		0x25, 0x7F, //     Logical Maximum (127)
		0x75, 0x08, //     Report Size (8)
		0x95, 0x01, //     Report Count (1)
		0x81, 0x06, //     Input (Data, Var, Rel)
		0xC0, //   End Collection
		0xC0, // End Collection
	}

	if err := g.writeFile(filepath.Join(hidDir, "report_desc"), string(reportDesc)); err != nil {
		return err
	}

	// Symlink function to configuration
	if err := os.Symlink(hidDir, filepath.Join(configDir, "hid.usb0")); err != nil && !os.IsExist(err) {
		return err
	}

	// Find and bind to UDC
	matches, err := filepath.Glob("/sys/class/udc/*")
	if err != nil || len(matches) == 0 {
		utils.LogWarn("No UDC found, USB gadget not bound")
		return nil
	}

	udc := filepath.Base(matches[0])
	if err := g.writeFile(filepath.Join(g.gadgetDir, "UDC"), udc); err != nil {
		return err
	}

	g.enabled = true
	utils.LogInfo("USB gadget enabled as HID mouse: udc=%s", udc)
	return nil
}

// writeFile writes content to a file
func (g *USBGadget) writeFile(path, content string) error {
	return os.WriteFile(path, []byte(content), 0644)
}

// Teardown disables and removes the USB gadget
func (g *USBGadget) Teardown() {
	if !g.enabled {
		return
	}

	// Disable gadget
	_ = g.writeFile(filepath.Join(g.gadgetDir, "UDC"), "")
	time.Sleep(100 * time.Millisecond)

	// Remove symlink
	configDir := filepath.Join(g.gadgetDir, fmt.Sprintf("configs/c.%d", g.configIndex))
	_ = os.Remove(filepath.Join(configDir, "hid.usb0"))
	_ = os.RemoveAll(filepath.Join(configDir, "strings"))

	// Remove function
	_ = os.RemoveAll(filepath.Join(g.gadgetDir, "functions/hid.usb0"))
	_ = os.RemoveAll(configDir)
	_ = os.RemoveAll(filepath.Join(g.gadgetDir, "strings"))

	// Remove gadget directory
	_ = os.RemoveAll(g.gadgetDir)

	g.enabled = false
	utils.LogInfo("USB gadget torn down")
}

// IsEnabled returns whether the gadget is enabled
func (g *USBGadget) IsEnabled() bool {
	return g.enabled
}

// SendMouseReport sends a mouse HID report
func (g *USBGadget) SendMouseReport(dx, dy int16, buttons byte, wheel int8) error {
	if !g.enabled {
		return fmt.Errorf("gadget not enabled")
	}

	report := []byte{
		buttons,                // Button state
		byte(dx & 0xFF),        // X low byte
		byte((dx >> 8) & 0xFF), // X high byte
		byte(dy & 0xFF),        // Y low byte
		byte((dy >> 8) & 0xFF), // Y high byte
		byte(wheel),            // Wheel delta
		0x00,                   // Padding
		0x00,                   // Padding
	}

	hidDev := filepath.Join(g.gadgetDir, "functions/hid.usb0/dev")
	return os.WriteFile(hidDev, report, 0644)
}

// GetUDC returns the current UDC
func (g *USBGadget) GetUDC() string {
	udcPath := filepath.Join(g.gadgetDir, "UDC")
	data, err := os.ReadFile(udcPath)
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(data))
}

// GetStatus returns gadget status
func (g *USBGadget) GetStatus() map[string]interface{} {
	return map[string]interface{}{
		"enabled":      g.enabled,
		"gadget_dir":   g.gadgetDir,
		"vendor_id":    g.vendorID,
		"product_id":   g.productID,
		"manufacturer": g.manufacturer,
		"product":      g.product,
		"udc":          g.GetUDC(),
	}
}
