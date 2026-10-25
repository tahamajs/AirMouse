# Linux Setup & Permissions Guide (uinput)

AirMouse creates a virtual synthetic mouse and keyboard device using the Linux kernel's `/dev/uinput` subsystem. Modern Linux systems (including Wayland on Ubuntu 22.04+, Fedora, and Arch) restrict `/dev/uinput` to root by default.

To run AirMouse as a normal desktop user without `sudo`:

### 1. Install the udev rule
```bash
sudo cp packaging/udev/99-airmouse-uinput.rules /etc/udev/rules.d/
```

### 2. Ensure your user is in the `input` group
```bash
sudo usermod -aG input $USER
```

### 3. Reload udev rules
```bash
sudo udevadm control --reload-rules && sudo udevadm trigger
```

### 4. Verification
Verify that `/dev/uinput` is accessible:
```bash
ls -l /dev/uinput
# Expected output:
# crw-rw----+ 1 root input 10, 223 ... /dev/uinput
```

Now you can run the AirMouse desktop daemon (`airmouse-server` or `airmouse-headless`) without root privileges under both X11 and Wayland sessions.
