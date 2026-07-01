document.addEventListener('DOMContentLoaded', () => {
    // ---- Toast Notification System ----
    const toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);

    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        const icon = type === 'success' ? 'check_circle' : 'error';
        toast.innerHTML = `<span class="material-icons-round">${icon}</span> <span>${message}</span>`;
        toastContainer.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    // ---- Navigation Logic ----
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');
    const pageTitle = document.getElementById('pageTitle');
    const pendingNavBadge = document.getElementById('pendingNavBadge');

    const titles = {
        'dashboard': 'Dashboard',
        'devices': 'Device Management',
        'settings': 'Server Configuration'
    };

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const pageId = item.dataset.page;
            
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            pages.forEach(p => p.classList.remove('active'));
            document.getElementById(pageId + 'Page').classList.add('active');

            pageTitle.textContent = titles[pageId];
            if(pageId === 'devices') fetchDevices();
            if(pageId === 'settings') loadSettings();
        });
    });

    // ---- Dashboard Live Stats ----
    const statElements = {
        clicks: document.getElementById('statClicks'),
        scrolls: document.getElementById('statScrolls'),
        devices: document.getElementById('statDevices'),
        uptime: document.getElementById('statUptime')
    };

    function formatUptime(s) {
        const h = String(Math.floor(s / 3600)).padStart(2, '0');
        const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
        const s_left = String(Math.floor(s % 60)).padStart(2, '0');
        return `${h}:${m}:${s_left}`;
    }

    // Activity Monitor Fake Chart for Aesthetics (Since real packet frequency isn't tracked in API yet)
    const monitor = document.querySelector('.live-activity-monitor');
    for (let i = 0; i < 20; i++) {
        const bar = document.createElement('div');
        bar.className = 'activity-bar';
        bar.style.height = '10%';
        monitor.appendChild(bar);
    }

    async function fetchStats() {
        try {
            const res = await fetch('/api/stats');
            const data = await res.json();
            if(data) {
                statElements.clicks.textContent = data.clicks || 0;
                statElements.scrolls.textContent = data.scrolls || 0;
                statElements.devices.textContent = data.devices || 0;
                statElements.uptime.textContent = formatUptime(data.uptime || 0);

                if (data.pending > 0) {
                    pendingNavBadge.textContent = data.pending;
                    pendingNavBadge.classList.remove('hidden');
                } else {
                    pendingNavBadge.classList.add('hidden');
                }

                // Render Server Connection Info
                const serverIPsEl = document.getElementById('serverIPs');
                const serverWSPortEl = document.getElementById('serverWSPort');
                const serverOtherPortsEl = document.getElementById('serverOtherPorts');
                if (serverIPsEl && data.ips) {
                    serverIPsEl.textContent = data.ips.join(' | ');
                }
                if (serverWSPortEl && data.ports) {
                    serverWSPortEl.textContent = data.ports.websocket || '-';
                }
                if (serverOtherPortsEl && data.ports) {
                    serverOtherPortsEl.textContent = `${data.ports.tcp || '-'} TCP / ${data.ports.udp || '-'} UDP`;
                }

                // Update activity chart based on clicks+scrolls delta to make it feel alive
                const bars = document.querySelectorAll('.activity-bar');
                const activityLevel = data.devices > 0 ? (Math.random() * 60 + 20) : (Math.random() * 10 + 5);
                bars.forEach((bar, idx) => {
                    if (idx < bars.length - 1) {
                        bar.style.height = bars[idx + 1].style.height; // Shift left
                    } else {
                        bar.style.height = `${activityLevel}%`;
                    }
                });
            }
        } catch(e) {}
    }

    // ---- Device Management ----
    const devicesGrid = document.getElementById('devicesGrid');
    const pendingDevicesGrid = document.getElementById('pendingDevicesGrid');
    const pendingDevicesPanel = document.getElementById('pendingDevicesPanel');
    const pendingCount = document.getElementById('pendingCount');
    
    document.getElementById('refreshDevicesBtn').addEventListener('click', () => {
        showToast('Refreshing devices...', 'success');
        fetchDevices();
    });

    async function fetchDevices() {
        try {
            const res = await fetch('/api/devices');
            const devices = await res.json();
            
            const active = devices.filter(d => d.status === 'connected' || d.status === 'active');
            const pending = devices.filter(d => d.status === 'pending_approval');

            // Render Active
            if(active.length > 0) {
                devicesGrid.innerHTML = active.map(d => `
                    <div class="device-card-wrapper" style="width: 100%; grid-column: 1 / -1;">
                        <div class="device-card" onclick="toggleDeviceDetails('${d.id}')" style="cursor: pointer; display: flex; width: 100%; align-items: center; justify-content: space-between;">
                            <div style="display: flex; align-items: center; gap: 16px;">
                                <div class="device-icon-large active">
                                    <span class="material-icons-round">smartphone</span>
                                </div>
                                <div class="device-info-inner" style="text-align: left;">
                                    <h4 style="margin: 0; font-size: 16px;">${d.name || 'Unknown Device'}</h4>
                                    <p style="margin: 4px 0 0 0; font-size: 13px; color: var(--text-secondary);">${d.id.substring(0, 8)}... • ${d.ip || 'Network'}</p>
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 12px;">
                                <span class="badge green-badge">Connected</span>
                                <span class="material-icons-round" style="color: var(--text-secondary);">expand_more</span>
                            </div>
                        </div>
                        <div class="device-details hidden" id="details-${d.id}" style="background: rgba(0,0,0,0.2); border: 1px solid var(--glass-border); border-top: none; padding: 20px; border-radius: 0 0 16px 16px; margin-top: -8px;">
                            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px;">
                                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary);">
                                    <h5 style="color: var(--text-primary); font-size: 14px; margin-bottom: 4px; font-weight: 600;">Hardware Specs</h5>
                                    <span><strong>Model:</strong> ${d.brand || ''} ${d.model || '-'}</span>
                                    <span><strong>Manufacturer:</strong> ${d.manufacturer || '-'}</span>
                                    <span><strong>Android Version:</strong> Android ${d.android_version || '-'} (SDK ${d.sdk || '-'})</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary);">
                                    <h5 style="color: var(--text-primary); font-size: 14px; margin-bottom: 4px; font-weight: 600;">Connection Details</h5>
                                    <span><strong>IP Address:</strong> ${d.ip || '-'}</span>
                                    <span><strong>MAC Address:</strong> ${d.mac || '-'}</span>
                                    <span><strong>Signal Strength (RSSI):</strong> ${d.rssi ? d.rssi + ' dBm' : 'Strong'}</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary);">
                                    <h5 style="color: var(--text-primary); font-size: 14px; margin-bottom: 4px; font-weight: 600;">Data Statistics</h5>
                                    <span><strong>Traffic Flow:</strong> Sent: ${formatBytes(d.bytes_sent)} | Recv: ${formatBytes(d.bytes_recv)}</span>
                                    <span><strong>Messages Count:</strong> Sent: ${d.msg_sent} | Recv: ${d.msg_recv}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `).join('');
            } else {
                devicesGrid.innerHTML = `<div class="loading-state"><span class="material-icons-round">phonelink_erase</span><span>No active devices</span></div>`;
            }

            // Render Pending
            if(pending.length > 0) {
                pendingDevicesPanel.classList.remove('hidden');
                pendingCount.textContent = `${pending.length} Pending`;
                pendingNavBadge.textContent = pending.length;
                pendingNavBadge.classList.remove('hidden');

                pendingDevicesGrid.innerHTML = pending.map(d => `
                    <div class="device-card">
                        <div class="device-icon-large pending">
                            <span class="material-icons-round">security</span>
                        </div>
                        <div class="device-info-inner">
                            <h4>${d.name || 'New Device Request'}</h4>
                            <p>${d.id.substring(0, 12)}...</p>
                        </div>
                        <div class="device-actions">
                            <button class="btn-approve" onclick="approveDevice('${d.id}')">Approve</button>
                            <button class="btn-reject" onclick="rejectDevice('${d.id}')">Reject</button>
                        </div>
                    </div>
                `).join('');
            } else {
                pendingDevicesPanel.classList.add('hidden');
                pendingNavBadge.classList.add('hidden');
            }
        } catch (e) {
            devicesGrid.innerHTML = `<div class="loading-state"><span>Failed to load devices</span></div>`;
        }
    }

    window.approveDevice = async (id) => {
        try {
            const res = await fetch('/api/devices/approve', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id })
            });
            if(res.ok) {
                showToast('Device Approved successfully!');
                fetchDevices();
                fetchStats();
            } else {
                showToast('Failed to approve device', 'error');
            }
        } catch(e) {
            showToast('Error approving device', 'error');
        }
    };

    window.rejectDevice = async (id) => {
        try {
            const res = await fetch('/api/devices/reject', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id })
            });
            if(res.ok) {
                showToast('Device Rejected');
                fetchDevices();
            }
        } catch(e) {}
    };

    // ---- Settings Management ----
    const settingsForm = document.getElementById('settingsForm');
    const saveSettingsBtn = document.getElementById('saveSettingsBtn');
    let currentConfig = null;

    async function loadSettings() {
        if(currentConfig) return; // already loaded
        try {
            const res = await fetch('/api/config');
            currentConfig = await res.json();
            
            settingsForm.innerHTML = `
                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-blue);">settings</span> General Settings</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Server Name</label>
                            <input type="text" id="cfgServerName" class="form-control" value="${currentConfig.server_name}">
                        </div>
                        <div class="form-group">
                            <label>Mouse Sensitivity</label>
                            <input type="number" step="0.1" id="cfgSensitivity" class="form-control" value="${currentConfig.sensitivity}">
                        </div>
                    </div>
                    <div class="toggle-wrapper">
                        <div class="toggle-info">
                            <h4>Strict Authentication</h4>
                            <p>Require devices to pair before connecting.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgAuth" ${currentConfig.auth_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-purple);">mouse</span> Mouse & Physics</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>Smoothing Enabled</h4>
                            <p>Smooth out hand tremors with exponential moving average.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgSmoothing" ${currentConfig.smoothing_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>Acceleration Enabled</h4>
                            <p>Increase speed dynamically based on swipe velocity.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgAccelEnabled" ${currentConfig.acceleration_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Acceleration Factor</label>
                            <input type="number" step="0.1" id="cfgAccelFactor" class="form-control" value="${currentConfig.acceleration_factor}">
                        </div>
                        <div class="form-group">
                            <label>Max Acceleration Limit</label>
                            <input type="number" step="0.1" id="cfgMaxAccel" class="form-control" value="${currentConfig.max_acceleration}">
                        </div>
                    </div>
                    <div class="toggle-wrapper" style="margin-top: 16px;">
                        <div class="toggle-info">
                            <h4>Haptic Feedback</h4>
                            <p>Vibrate phone slightly when mouse events occur.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgHaptic" ${currentConfig.haptic_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-green);">psychology</span> AI & Predictive Tracking</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>AI Cursor Smoothing</h4>
                            <p>Use Machine Learning (ONNX) to predict and smooth movements.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgAISmooth" ${currentConfig.enable_ai_smoothing ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>Predictive Tracking</h4>
                            <p>Extrapolate motion vectors for ultra-low latency tracking.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgPredictive" ${currentConfig.enable_predictive ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Prediction Blend Factor (0.0 to 1.0)</label>
                            <input type="number" step="0.05" id="cfgPredictiveBlend" class="form-control" value="${currentConfig.predictive_blend_factor}">
                        </div>
                        <div class="form-group">
                            <label>Jitter Blend Factor (0.0 to 1.0)</label>
                            <input type="number" step="0.05" id="cfgJitterBlend" class="form-control" value="${currentConfig.jitter_blend_factor}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-orange);">gesture</span> Click & Gesture Thresholds</h4>
                    <div class="form-grid" style="margin-bottom: 16px;">
                        <div class="form-group">
                            <label>Click Force Threshold</label>
                            <input type="number" step="0.1" id="cfgClickThreshold" class="form-control" value="${currentConfig.click_threshold}">
                        </div>
                        <div class="form-group">
                            <label>Scroll Velocity Threshold</label>
                            <input type="number" step="0.1" id="cfgScrollThreshold" class="form-control" value="${currentConfig.scroll_threshold}">
                        </div>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Double Click Interval (ms)</label>
                            <input type="number" id="cfgDoubleClickInterval" class="form-control" value="${currentConfig.double_click_interval}">
                        </div>
                        <div class="form-group">
                            <label>Right Click Tilt Angle</label>
                            <input type="number" step="0.1" id="cfgRightClickTilt" class="form-control" value="${currentConfig.right_click_tilt}">
                        </div>
                    </div>
                </div>

                <div class="settings-section" style="border-bottom: none; padding-bottom: 0;">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-red);">settings_remote</span> Proximity & BLE</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>Proximity Autoclose</h4>
                            <p>Automatically lock pointer when phone is far from screen.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgProximity" ${currentConfig.proximity_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 16px;">
                        <div class="toggle-info">
                            <h4>BLE Discovery</h4>
                            <p>Broadcast server identity using BLE advertising.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgBLE" ${currentConfig.ble_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper">
                        <div class="toggle-info">
                            <h4>HID Proxy Mode</h4>
                            <p>Proxy client commands to hardware mouse emulation.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgHIDProxy" ${currentConfig.hid_proxy_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                </div>
            `;
        } catch(e) {
            settingsForm.innerHTML = `<div class="loading-state"><span>Error loading configuration</span></div>`;
        }
    }

    saveSettingsBtn.addEventListener('click', async () => {
        if(!currentConfig) return;
        
        saveSettingsBtn.textContent = 'Saving...';
        
        currentConfig.server_name = document.getElementById('cfgServerName').value;
        currentConfig.sensitivity = parseFloat(document.getElementById('cfgSensitivity').value);
        currentConfig.auth_enabled = document.getElementById('cfgAuth').checked;

        currentConfig.smoothing_enabled = document.getElementById('cfgSmoothing').checked;
        currentConfig.acceleration_enabled = document.getElementById('cfgAccelEnabled').checked;
        currentConfig.acceleration_factor = parseFloat(document.getElementById('cfgAccelFactor').value);
        currentConfig.max_acceleration = parseFloat(document.getElementById('cfgMaxAccel').value);
        currentConfig.haptic_enabled = document.getElementById('cfgHaptic').checked;

        currentConfig.enable_ai_smoothing = document.getElementById('cfgAISmooth').checked;
        currentConfig.enable_predictive = document.getElementById('cfgPredictive').checked;
        currentConfig.predictive_blend_factor = parseFloat(document.getElementById('cfgPredictiveBlend').value);
        currentConfig.jitter_blend_factor = parseFloat(document.getElementById('cfgJitterBlend').value);

        currentConfig.click_threshold = parseFloat(document.getElementById('cfgClickThreshold').value);
        currentConfig.scroll_threshold = parseFloat(document.getElementById('cfgScrollThreshold').value);
        currentConfig.double_click_interval = parseInt(document.getElementById('cfgDoubleClickInterval').value);
        currentConfig.right_click_tilt = parseFloat(document.getElementById('cfgRightClickTilt').value);

        currentConfig.proximity_enabled = document.getElementById('cfgProximity').checked;
        currentConfig.ble_enabled = document.getElementById('cfgBLE').checked;
        currentConfig.hid_proxy_enabled = document.getElementById('cfgHIDProxy').checked;

        try {
            const res = await fetch('/api/config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(currentConfig)
            });
            if(res.ok) {
                showToast('Settings saved successfully!');
            } else {
                showToast('Failed to save settings', 'error');
            }
        } catch(e) {
            showToast('Error saving settings', 'error');
        }
        saveSettingsBtn.textContent = 'Save Config';
    });

    // ---- Helper Functions ----
    window.toggleDeviceDetails = (id) => {
        const el = document.getElementById(`details-${id}`);
        if(el) {
            el.classList.toggle('hidden');
        }
    };

    function formatBytes(bytes) {
        if (!bytes) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    // Start Polling Stats every 2s
    fetchStats();
    setInterval(fetchStats, 2000);
});