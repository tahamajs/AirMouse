# scripts/preinstall.sh
#!/bin/sh
echo "Preparing to install Air Mouse Pro Server..."

# scripts/postinstall.sh
#!/bin/sh
echo "Air Mouse Pro Server installed successfully!"
systemctl daemon-reload 2>/dev/null || true

# scripts/preremove.sh
#!/bin/sh
echo "Removing Air Mouse Pro Server..."

# scripts/postremove.sh
#!/bin/sh
echo "Air Mouse Pro Server removed completely."