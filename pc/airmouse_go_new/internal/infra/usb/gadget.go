package usb

import (
	"fmt"
	"os"
	"path/filepath"

	"airmouse-go/internal/infra/logger"
)

type Gadget struct {
	enabled   bool
	gadgetDir string
}

func NewGadget() *Gadget {
	return &Gadget{
		gadgetDir: "/sys/kernel/config/usb_gadget/airmouse",
	}
}

// Setup configures the USB gadget as a HID mouse.
func (g *Gadget) Setup() error {
	if _, err := os.Stat("/sys/kernel/config/usb_gadget"); os.IsNotExist(err) {
		logger.Warn("USB gadget not supported (kernel configfs missing)")
		return nil
	}
	// Create gadget directory
	if err := os.MkdirAll(g.gadgetDir, 0755); err != nil {
		return fmt.Errorf("failed to create gadget dir: %w", err)
	}
	// Set USB IDs (example: generic mouse)
	if err := g.writeFile("idVendor", "0x1d6b"); err != nil {
		return err
	}
	if err := g.writeFile("idProduct", "0x0104"); err != nil {
		return err
	}
	// Create configuration
	configDir := filepath.Join(g.gadgetDir, "configs/c.1")
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(configDir, "MaxPower"), "100"); err != nil {
		return err
	}
	// Create HID function
	hidDir := filepath.Join(g.gadgetDir, "functions/hid.usb0")
	if err := os.MkdirAll(hidDir, 0755); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(hidDir, "protocol"), "2"); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(hidDir, "subclass"), "1"); err != nil {
		return err
	}
	if err := g.writeFile(filepath.Join(hidDir, "report_length"), "8"); err != nil {
		return err
	}
	// Symlink function to configuration
	if err := os.Symlink(hidDir, filepath.Join(configDir, "hid.usb0")); err != nil && !os.IsExist(err) {
		return err
	}
	// Bind to UDC (USB Device Controller)
	udcFile := "/sys/class/udc/*"
	matches, err := filepath.Glob(udcFile)
	if err != nil || len(matches) == 0 {
		logger.Warn("No UDC found, USB gadget not bound")
		return nil
	}
	udc := filepath.Base(matches[0])
	if err := g.writeFile(filepath.Join(g.gadgetDir, "UDC"), udc); err != nil {
		return err
	}
	g.enabled = true
	logger.Info("USB gadget enabled as HID mouse")
	return nil
}

func (g *Gadget) writeFile(path, content string) error {
	return os.WriteFile(path, []byte(content), 0644)
}

func (g *Gadget) Teardown() {
	if !g.enabled {
		return
	}
	// Disable gadget
	_ = g.writeFile(filepath.Join(g.gadgetDir, "UDC"), "")
	// Remove symlinks and directories
	configDir := filepath.Join(g.gadgetDir, "configs/c.1")
	_ = os.Remove(filepath.Join(configDir, "hid.usb0"))
	_ = os.RemoveAll(filepath.Join(g.gadgetDir, "functions/hid.usb0"))
	_ = os.RemoveAll(configDir)
	_ = os.RemoveAll(g.gadgetDir)
	logger.Info("USB gadget torn down")
}
