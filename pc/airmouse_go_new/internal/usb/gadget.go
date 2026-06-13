package usb

import (
    "fmt"
    "os"
    "path/filepath"
    "strings"
    "time"

    "airmouse-go/internal/infra/logger"
)

type Gadget struct {
    enabled      bool
    gadgetDir    string
    vendorID     string
    productID    string
    manufacturer string
    product      string
    serial       string
}

type GadgetConfig struct {
    VendorID     string
    ProductID    string
    Manufacturer string
    Product      string
    Serial       string
}

func NewGadget(cfg GadgetConfig) *Gadget {
    return &Gadget{
        gadgetDir:    "/sys/kernel/config/usb_gadget/airmouse",
        vendorID:     cfg.VendorID,
        productID:    cfg.ProductID,
        manufacturer: cfg.Manufacturer,
        product:      cfg.Product,
        serial:       cfg.Serial,
    }
}

// Setup configures the USB gadget as a HID mouse
func (g *Gadget) Setup() error {
    // Check if configfs is available
    if _, err := os.Stat("/sys/kernel/config/usb_gadget"); os.IsNotExist(err) {
        logger.Warn("USB gadget not supported (kernel configfs missing)")
        return nil
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
    if err := g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/manufacturer"), g.manufacturer); err != nil {
        // Create strings directory if needed
        os.MkdirAll(filepath.Join(g.gadgetDir, "strings/0x409"), 0755)
        g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/manufacturer"), g.manufacturer)
    }
    if err := g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/product"), g.product); err != nil {
        g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/product"), g.product)
    }
    if err := g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/serialnumber"), g.serial); err != nil {
        g.writeFile(filepath.Join(g.gadgetDir, "strings/0x409/serialnumber"), g.serial)
    }

    // Create configuration
    configDir := filepath.Join(g.gadgetDir, "configs/c.1")
    if err := os.MkdirAll(configDir, 0755); err != nil {
        return err
    }

    // Set configuration strings
    stringsDir := filepath.Join(configDir, "strings/0x409")
    os.MkdirAll(stringsDir, 0755)
    g.writeFile(filepath.Join(stringsDir, "configuration"), "HID Mouse")

    // Set max power (100 mA)
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
        0xC0,       //   End Collection
        0xC0,       // End Collection
    }

    if err := g.writeFile(filepath.Join(hidDir, "report_desc"), string(reportDesc)); err != nil {
        return err
    }

    // Symlink function to configuration
    if err := os.Symlink(hidDir, filepath.Join(configDir, "hid.usb0")); err != nil && !os.IsExist(err) {
        return err
    }

    // Find and bind to UDC
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
    logger.Info("USB gadget enabled as HID mouse (UDC: %s)", udc)
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
    time.Sleep(100 * time.Millisecond)

    // Remove symlinks
    configDir := filepath.Join(g.gadgetDir, "configs/c.1")
    _ = os.Remove(filepath.Join(configDir, "hid.usb0"))
    _ = os.RemoveAll(filepath.Join(configDir, "strings"))

    // Remove functions
    _ = os.RemoveAll(filepath.Join(g.gadgetDir, "functions/hid.usb0"))
    _ = os.RemoveAll(configDir)
    _ = os.RemoveAll(filepath.Join(g.gadgetDir, "strings"))

    // Remove gadget directory
    _ = os.RemoveAll(g.gadgetDir)

    g.enabled = false
    logger.Info("USB gadget torn down")
}

func (g *Gadget) IsEnabled() bool {
    return g.enabled
}

func (g *Gadget) SendMouseReport(dx, dy int16, buttons byte, wheel int8) error {
    if !g.enabled {
        return fmt.Errorf("gadget not enabled")
    }

    // Report format: buttons (1 byte) + X (2 bytes) + Y (2 bytes) + wheel (1 byte) + padding (2 bytes)
    report := []byte{
        buttons,           // Button state
        byte(dx & 0xFF),   // X low byte
        byte((dx >> 8) & 0xFF), // X high byte
        byte(dy & 0xFF),   // Y low byte
        byte((dy >> 8) & 0xFF), // Y high byte
        byte(wheel),       // Wheel delta
        0x00,              // Padding
        0x00,              // Padding
    }

    hidDev := filepath.Join(g.gadgetDir, "functions/hid.usb0/dev")
    return os.WriteFile(hidDev, report, 0644)
}