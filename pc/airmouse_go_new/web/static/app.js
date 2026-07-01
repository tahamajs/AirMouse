document.addEventListener('DOMContentLoaded', () => {
    // ---- Toast Notification System ----
    const toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
    let serverIsPaused = false;

    let lastClicks = null;
    let lastScrolls = null;
    let lastDevices = null;
    let lastPaused = null;

    function addConsoleLog(message, level = 'INFO') {
        const consoleLogs = document.getElementById('consoleLogs');
        if (!consoleLogs) return;
        const time = new Date().toLocaleTimeString();
        const log = document.createElement('div');
        let color = '#34d399'; // green for info
        if (level === 'DEBUG') color = '#9ca3af'; // gray
        if (level === 'WARN') color = '#fbbf24'; // orange
        if (level === 'ERROR') color = '#f87171'; // red
        log.innerHTML = `<span style="color: var(--text-secondary); margin-right: 8px;">[${time}]</span><span style="color: ${color}; font-weight: 600; margin-right: 8px;">[${level}]</span><span style="color: var(--text-primary);">${message}</span>`;
        consoleLogs.appendChild(log);
        consoleLogs.scrollTop = consoleLogs.scrollHeight;
    }

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
        'mirror': 'Screen Mirroring',
        'presentation': 'Presenter & Media Controller',
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

                // Update Last Connected Device & Android Mode
                const lastDeviceEl = document.getElementById('statLastDevice');
                if (lastDeviceEl && data.last_device) {
                    lastDeviceEl.textContent = data.last_device_time !== '-' ? `${data.last_device} (${data.last_device_time})` : data.last_device;
                }
                const activeModeEl = document.getElementById('statActiveMode');
                if (activeModeEl && data.active_mode) {
                    activeModeEl.textContent = data.active_mode;
                }

                // Live Logging comparisons
                if (lastClicks !== null && data.clicks > lastClicks) {
                    addConsoleLog(`Mouse clicks updated (+${data.clicks - lastClicks}) - Total: ${data.clicks}`);
                }
                if (lastScrolls !== null && data.scrolls > lastScrolls) {
                    addConsoleLog(`Mouse scrolls updated (+${data.scrolls - lastScrolls}) - Total: ${data.scrolls}`);
                }
                if (lastDevices !== null && data.devices !== lastDevices) {
                    addConsoleLog(`Active client count changed from ${lastDevices} to ${data.devices}`, data.devices > lastDevices ? 'INFO' : 'WARN');
                }
                if (lastPaused !== null && data.paused !== lastPaused) {
                    addConsoleLog(data.paused ? 'Global motion capture paused' : 'Global motion capture resumed', 'WARN');
                }

                lastClicks = data.clicks || 0;
                lastScrolls = data.scrolls || 0;
                lastDevices = data.devices || 0;
                lastPaused = data.paused || false;

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

                // Update active listeners indicators
                if (data.protocols) {
                    updateListenerBadge('listenerWS', data.protocols.websocket);
                    updateListenerBadge('listenerTCP', data.protocols.tcp);
                    updateListenerBadge('listenerUDP', data.protocols.udp);
                    updateListenerBadge('listenerBT', data.protocols.bluetooth);
                    updateListenerBadge('listenerSerial', data.protocols.serial);
                }

                // Update server paused/active status
                const indicator = document.getElementById('serverStatusIndicator');
                const text = document.getElementById('serverStatusText');
                const icon = document.getElementById('toggleServerIcon');
                if (indicator && text && icon) {
                    if (data.paused) {
                        indicator.className = 'status-indicator';
                        indicator.style.backgroundColor = '#ef4444';
                        indicator.style.boxShadow = '0 0 12px #ef4444';
                        text.textContent = 'Server Paused';
                        icon.textContent = 'play_arrow';
                        serverIsPaused = true;
                    } else {
                        indicator.className = 'status-indicator online';
                        indicator.style.backgroundColor = '';
                        indicator.style.boxShadow = '';
                        text.textContent = 'Server Active';
                        icon.textContent = 'pause';
                        serverIsPaused = false;
                    }
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
                                    <p style="margin: 4px 0 0 0; font-size: 13px; color: var(--text-secondary);">${d.id.substring(0, 8)}... • ${d.ip_address || d.ip || 'Network'}</p>
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
                                    <span><strong>Model:</strong> ${d.brand || ''} ${d.device_model || d.model || '-'}</span>
                                    <span><strong>Manufacturer:</strong> ${d.manufacturer || '-'}</span>
                                    <span><strong>Android Version:</strong> Android ${d.android_version || '-'} (SDK ${d.sdk_int || d.sdk || '-'})</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary);">
                                    <h5 style="color: var(--text-primary); font-size: 14px; margin-bottom: 4px; font-weight: 600;">Connection Details</h5>
                                    <span><strong>IP Address:</strong> ${d.ip_address || '-'}</span>
                                    <span><strong>MAC Address:</strong> ${d.mac_address || '-'}</span>
                                    <span><strong>Protocol:</strong> ${d.protocol || '-'} (${d.transport || 'WS'})</span>
                                    <span><strong>Signal Strength (RSSI):</strong> ${d.rssi ? d.rssi + ' dBm' : 'Strong'}</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary);">
                                    <h5 style="color: var(--text-primary); font-size: 14px; margin-bottom: 4px; font-weight: 600;">Data Statistics</h5>
                                    <span><strong>Traffic Flow:</strong> Sent: ${formatBytes(d.bytes_sent)} | Recv: ${formatBytes(d.bytes_recv)}</span>
                                    <span><strong>Messages Count:</strong> Sent: ${d.messages_sent || d.msg_sent || 0} | Recv: ${d.messages_recv || d.msg_recv || 0}</span>
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
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-blue);">settings</span> General & Server Info</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Server Name</label>
                            <input type="text" id="cfgServerName" class="form-control" value="${currentConfig.server_name}">
                        </div>
                        <div class="form-group">
                            <label>Accent Theme</label>
                            <select id="cfgTheme" class="form-control" style="background: rgba(0,0,0,0.5); border: 1px solid var(--glass-border); color: white;">
                                <option value="dark" ${currentConfig.theme === 'dark' ? 'selected' : ''}>Dark</option>
                                <option value="light" ${currentConfig.theme === 'light' ? 'selected' : ''}>Light</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>TCP Listen Port</label>
                            <input type="number" id="cfgPort" class="form-control" value="${currentConfig.port}">
                        </div>
                        <div class="form-group">
                            <label>WebSocket Port</label>
                            <input type="number" id="cfgWSPort" class="form-control" value="${currentConfig.websocket_port}">
                        </div>
                        <div class="form-group">
                            <label>UDP Port</label>
                            <input type="number" id="cfgUDPPort" class="form-control" value="${currentConfig.udp_port}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-green);">settings_ethernet</span> Connection Protocols</h4>
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px;">
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>TCP Server</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgEnableTCP" ${currentConfig.enable_tcp ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>WebSocket</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgEnableWS" ${currentConfig.enable_websocket ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>UDP Server</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgEnableUDP" ${currentConfig.enable_udp ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>Bluetooth</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgEnableBT" ${currentConfig.enable_bluetooth ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>Serial Port</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgEnableSerial" ${currentConfig.enable_serial ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-purple);">mouse</span> Mouse & Physics</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Smoothing Enabled</h4>
                            <p>Smooth out hand tremors with exponential moving average.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgSmoothing" ${currentConfig.smoothing_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
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
                            <label>Sensitivity Factor</label>
                            <input type="number" step="0.1" id="cfgSensitivity" class="form-control" value="${currentConfig.sensitivity}">
                        </div>
                        <div class="form-group">
                            <label>Acceleration Factor</label>
                            <input type="number" step="0.1" id="cfgAccelFactor" class="form-control" value="${currentConfig.acceleration_factor}">
                        </div>
                        <div class="form-group">
                            <label>Max Acceleration Limit</label>
                            <input type="number" step="0.1" id="cfgMaxAccel" class="form-control" value="${currentConfig.max_acceleration}">
                        </div>
                    </div>
                    <div class="toggle-wrapper" style="margin-top: 12px;">
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
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>AI Cursor Smoothing</h4>
                            <p>Use Machine Learning (ONNX) to predict and smooth movements.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgAISmooth" ${currentConfig.enable_ai_smoothing ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
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
                            <label>Prediction Blend Factor</label>
                            <input type="number" step="0.05" id="cfgPredictiveBlend" class="form-control" value="${currentConfig.predictive_blend_factor}">
                        </div>
                        <div class="form-group">
                            <label>Jitter Compensation</label>
                            <select id="cfgJitterComp" class="form-control" style="background: rgba(0,0,0,0.5); border: 1px solid var(--glass-border); color: white;">
                                <option value="true" ${currentConfig.enable_jitter_compensation ? 'selected' : ''}>Enabled</option>
                                <option value="false" ${!currentConfig.enable_jitter_compensation ? 'selected' : ''}>Disabled</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Jitter Blend Factor</label>
                            <input type="number" step="0.05" id="cfgJitterBlend" class="form-control" value="${currentConfig.jitter_blend_factor}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-purple);">model_training</span> ML Predictor (LSTM)</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Enable LSTM Prediction</h4>
                            <p>Run local sequence prediction to model human hand latency.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgMLPredict" ${currentConfig.enable_ml_prediction ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Model Path (.onnx)</label>
                            <input type="text" id="cfgMLModelPath" class="form-control" value="${currentConfig.ml_model_path || ''}">
                        </div>
                        <div class="form-group">
                            <label>Sequence Length</label>
                            <input type="number" id="cfgMLSeqLength" class="form-control" value="${currentConfig.ml_sequence_length}">
                        </div>
                        <div class="form-group">
                            <label>ML Blend Factor</label>
                            <input type="number" step="0.05" id="cfgMLBlend" class="form-control" value="${currentConfig.ml_blend_factor}">
                        </div>
                        <div class="form-group">
                            <label>Inference Interval (ms)</label>
                            <input type="number" id="cfgMLInterval" class="form-control" value="${currentConfig.ml_inference_interval_ms}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-orange);">waves</span> Humanizer Physics Engine</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Enable Humanizer</h4>
                            <p>Inject micro-tremors and B-Splines to humanize cursor movement.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgHumanizer" ${currentConfig.enable_humanizer ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Tremor Amplitude</label>
                            <input type="number" step="0.1" id="cfgHumanTremor" class="form-control" value="${currentConfig.humanizer_tremor_amplitude}">
                        </div>
                        <div class="form-group">
                            <label>B-Spline Segments</label>
                            <input type="number" id="cfgHumanSegments" class="form-control" value="${currentConfig.humanizer_bspline_segments}">
                        </div>
                        <div class="form-group">
                            <label>Noise Amplitude</label>
                            <input type="number" step="0.1" id="cfgHumanNoise" class="form-control" value="${currentConfig.humanizer_noise_amplitude}">
                        </div>
                        <div class="form-group">
                            <label>Velocity Peak Ratio</label>
                            <input type="number" step="0.05" id="cfgHumanPeak" class="form-control" value="${currentConfig.humanizer_velocity_peak_ratio}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-blue);">filter_alt</span> Particle Filter</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Enable Particle Filter</h4>
                            <p>Estimates absolute hand positioning using particle models.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgParticle" ${currentConfig.enable_particle_filter ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Number of Particles</label>
                            <input type="number" id="cfgParticleCount" class="form-control" value="${currentConfig.particle_filter_num_particles}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-orange);">gesture</span> Gesture & Click Thresholds</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Gesture Confidence (0.0 to 1.0)</label>
                            <input type="number" step="0.05" id="cfgGestureConfidence" class="form-control" value="${currentConfig.gesture_confidence_threshold}">
                        </div>
                        <div class="form-group">
                            <label>Gesture Cooldown (ms)</label>
                            <input type="number" id="cfgGestureCooldown" class="form-control" value="${currentConfig.gesture_cooldown_ms}">
                        </div>
                        <div class="form-group">
                            <label>Click Force Threshold</label>
                            <input type="number" step="0.1" id="cfgClickThreshold" class="form-control" value="${currentConfig.click_threshold}">
                        </div>
                        <div class="form-group">
                            <label>Scroll Velocity Threshold</label>
                            <input type="number" step="0.1" id="cfgScrollThreshold" class="form-control" value="${currentConfig.scroll_threshold}">
                        </div>
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

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-red);">settings_remote</span> Proximity & BLE</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Proximity Autoclose</h4>
                            <p>Automatically lock pointer when phone is far from screen.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgProximity" ${currentConfig.proximity_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid" style="margin-bottom: 12px;">
                        <div class="form-group">
                            <label>Near Threshold (m)</label>
                            <input type="number" step="0.1" id="cfgProxNear" class="form-control" value="${currentConfig.proximity_near_threshold}">
                        </div>
                        <div class="form-group">
                            <label>Far Threshold (m)</label>
                            <input type="number" step="0.1" id="cfgProxFar" class="form-control" value="${currentConfig.proximity_far_threshold}">
                        </div>
                        <div class="form-group">
                            <label>Scan Interval (ms)</label>
                            <input type="number" id="cfgProxInterval" class="form-control" value="${currentConfig.proximity_scan_interval_ms}">
                        </div>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
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

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-blue);">usb</span> USB Gadget Emulation</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>USB Gadget Mode</h4>
                            <p>Emulate hardware mouse over OTG USB connection.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgUSBEnabled" ${currentConfig.usb_gadget_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Vendor ID</label>
                            <input type="text" id="cfgUSBVendor" class="form-control" value="${currentConfig.usb_vendor_id || ''}">
                        </div>
                        <div class="form-group">
                            <label>Product ID</label>
                            <input type="text" id="cfgUSBProduct" class="form-control" value="${currentConfig.usb_product_id || ''}">
                        </div>
                        <div class="form-group">
                            <label>Manufacturer</label>
                            <input type="text" id="cfgUSBManufacturer" class="form-control" value="${currentConfig.usb_manufacturer || ''}">
                        </div>
                        <div class="form-group">
                            <label>Product Name</label>
                            <input type="text" id="cfgUSBProductName" class="form-control" value="${currentConfig.usb_product || ''}">
                        </div>
                    </div>
                </div>

                <div class="settings-section">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-red);">security</span> Security & Performance</h4>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Strict Authentication</h4>
                            <p>Require devices to pair before connecting.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgAuth" ${currentConfig.auth_enabled ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="toggle-wrapper" style="margin-bottom: 12px;">
                        <div class="toggle-info">
                            <h4>Desktop Pairing Notifications</h4>
                            <p>Display connection approval popup notifications on the desktop.</p>
                        </div>
                        <label class="switch">
                            <input type="checkbox" id="cfgPairingUI" ${currentConfig.enable_pairing_ui ? 'checked' : ''}>
                            <span class="slider"></span>
                        </label>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Token Time To Live (hours)</label>
                            <input type="number" id="cfgTokenTTL" class="form-control" value="${currentConfig.token_ttl_hours}">
                        </div>
                        <div class="form-group">
                            <label>Max Client Limit</label>
                            <input type="number" id="cfgMaxClients" class="form-control" value="${currentConfig.max_clients}">
                        </div>
                        <div class="form-group">
                            <label>Heartbeat Interval (s)</label>
                            <input type="number" id="cfgHeartbeat" class="form-control" value="${currentConfig.heartbeat_interval}">
                        </div>
                        <div class="form-group">
                            <label>Connection Timeout (s)</label>
                            <input type="number" id="cfgTimeout" class="form-control" value="${currentConfig.connection_timeout}">
                        </div>
                    </div>
                </div>

                <div class="settings-section" style="border-bottom: none; padding-bottom: 0;">
                    <h4 class="section-title"><span class="material-icons-round" style="color: var(--accent-blue);">terminal</span> Logs & Diagnostics</h4>
                    <div class="form-grid" style="margin-bottom: 12px;">
                        <div class="form-group">
                            <label>Log Level</label>
                            <select id="cfgLogLevel" class="form-control" style="background: rgba(0,0,0,0.5); border: 1px solid var(--glass-border); color: white;">
                                <option value="debug" ${currentConfig.log_level === 'debug' ? 'selected' : ''}>Debug</option>
                                <option value="info" ${currentConfig.log_level === 'info' ? 'selected' : ''}>Info</option>
                                <option value="warn" ${currentConfig.log_level === 'warn' ? 'selected' : ''}>Warning</option>
                                <option value="error" ${currentConfig.log_level === 'error' ? 'selected' : ''}>Error</option>
                            </select>
                        </div>
                    </div>
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px;">
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>Colored Logs</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgLogColor" ${currentConfig.log_color ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>Debug Mode</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgDebugMode" ${currentConfig.debug_mode ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
                        <div class="toggle-wrapper">
                            <div class="toggle-info">
                                <h4>Metrics Collection</h4>
                            </div>
                            <label class="switch">
                                <input type="checkbox" id="cfgMetricsEnabled" ${currentConfig.metrics_enabled ? 'checked' : ''}>
                                <span class="slider"></span>
                            </label>
                        </div>
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
        currentConfig.theme = document.getElementById('cfgTheme').value;
        currentConfig.port = parseInt(document.getElementById('cfgPort').value);
        currentConfig.websocket_port = parseInt(document.getElementById('cfgWSPort').value);
        currentConfig.udp_port = parseInt(document.getElementById('cfgUDPPort').value);

        currentConfig.enable_tcp = document.getElementById('cfgEnableTCP').checked;
        currentConfig.enable_websocket = document.getElementById('cfgEnableWS').checked;
        currentConfig.enable_udp = document.getElementById('cfgEnableUDP').checked;
        currentConfig.enable_bluetooth = document.getElementById('cfgEnableBT').checked;
        currentConfig.enable_serial = document.getElementById('cfgEnableSerial').checked;

        currentConfig.sensitivity = parseFloat(document.getElementById('cfgSensitivity').value);
        currentConfig.smoothing_enabled = document.getElementById('cfgSmoothing').checked;
        currentConfig.acceleration_enabled = document.getElementById('cfgAccelEnabled').checked;
        currentConfig.acceleration_factor = parseFloat(document.getElementById('cfgAccelFactor').value);
        currentConfig.max_acceleration = parseFloat(document.getElementById('cfgMaxAccel').value);
        currentConfig.haptic_enabled = document.getElementById('cfgHaptic').checked;

        currentConfig.enable_ai_smoothing = document.getElementById('cfgAISmooth').checked;
        currentConfig.enable_predictive = document.getElementById('cfgPredictive').checked;
        currentConfig.predictive_blend_factor = parseFloat(document.getElementById('cfgPredictiveBlend').value);
        currentConfig.enable_jitter_compensation = (document.getElementById('cfgJitterComp').value === 'true');
        currentConfig.jitter_blend_factor = parseFloat(document.getElementById('cfgJitterBlend').value);

        currentConfig.enable_ml_prediction = document.getElementById('cfgMLPredict').checked;
        currentConfig.ml_model_path = document.getElementById('cfgMLModelPath').value;
        currentConfig.ml_sequence_length = parseInt(document.getElementById('cfgMLSeqLength').value);
        currentConfig.ml_blend_factor = parseFloat(document.getElementById('cfgMLBlend').value);
        currentConfig.ml_inference_interval_ms = parseInt(document.getElementById('cfgMLInterval').value);

        currentConfig.enable_humanizer = document.getElementById('cfgHumanizer').checked;
        currentConfig.humanizer_tremor_amplitude = parseFloat(document.getElementById('cfgHumanTremor').value);
        currentConfig.humanizer_bspline_segments = parseInt(document.getElementById('cfgHumanSegments').value);
        currentConfig.humanizer_noise_amplitude = parseFloat(document.getElementById('cfgHumanNoise').value);
        currentConfig.humanizer_velocity_peak_ratio = parseFloat(document.getElementById('cfgHumanPeak').value);

        currentConfig.enable_particle_filter = document.getElementById('cfgParticle').checked;
        currentConfig.particle_filter_num_particles = parseInt(document.getElementById('cfgParticleCount').value);

        currentConfig.gesture_confidence_threshold = parseFloat(document.getElementById('cfgGestureConfidence').value);
        currentConfig.gesture_cooldown_ms = parseInt(document.getElementById('cfgGestureCooldown').value);
        currentConfig.click_threshold = parseFloat(document.getElementById('cfgClickThreshold').value);
        currentConfig.scroll_threshold = parseFloat(document.getElementById('cfgScrollThreshold').value);
        currentConfig.double_click_interval = parseInt(document.getElementById('cfgDoubleClickInterval').value);
        currentConfig.right_click_tilt = parseFloat(document.getElementById('cfgRightClickTilt').value);

        currentConfig.proximity_enabled = document.getElementById('cfgProximity').checked;
        currentConfig.proximity_near_threshold = parseFloat(document.getElementById('cfgProxNear').value);
        currentConfig.proximity_far_threshold = parseFloat(document.getElementById('cfgProxFar').value);
        currentConfig.proximity_scan_interval_ms = parseInt(document.getElementById('cfgProxInterval').value);
        currentConfig.ble_enabled = document.getElementById('cfgBLE').checked;
        currentConfig.hid_proxy_enabled = document.getElementById('cfgHIDProxy').checked;

        currentConfig.usb_gadget_enabled = document.getElementById('cfgUSBEnabled').checked;
        currentConfig.usb_vendor_id = document.getElementById('cfgUSBVendor').value;
        currentConfig.usb_product_id = document.getElementById('cfgUSBProduct').value;
        currentConfig.usb_manufacturer = document.getElementById('cfgUSBManufacturer').value;
        currentConfig.usb_product = document.getElementById('cfgUSBProductName').value;

        currentConfig.auth_enabled = document.getElementById('cfgAuth').checked;
        currentConfig.enable_pairing_ui = document.getElementById('cfgPairingUI').checked;
        currentConfig.token_ttl_hours = parseInt(document.getElementById('cfgTokenTTL').value);
        currentConfig.max_clients = parseInt(document.getElementById('cfgMaxClients').value);
        currentConfig.heartbeat_interval = parseInt(document.getElementById('cfgHeartbeat').value);
        currentConfig.connection_timeout = parseInt(document.getElementById('cfgTimeout').value);

        currentConfig.log_level = document.getElementById('cfgLogLevel').value;
        currentConfig.log_color = document.getElementById('cfgLogColor').checked;
        currentConfig.debug_mode = document.getElementById('cfgDebugMode').checked;
        currentConfig.metrics_enabled = document.getElementById('cfgMetricsEnabled').checked;

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

    // Clear logs button listener
    const clearLogsBtn = document.getElementById('clearLogsBtn');
    if (clearLogsBtn) {
        clearLogsBtn.addEventListener('click', () => {
            const consoleLogs = document.getElementById('consoleLogs');
            if (consoleLogs) {
                consoleLogs.innerHTML = `<div style="color: var(--text-secondary);">[INFO] Console cleared. Listening...</div>`;
                showToast('Console logs cleared');
            }
        });
    }

    // Start Polling Stats every 2s
    fetchStats();
    setInterval(fetchStats, 2000);

    // Toggle server movement status
    const toggleServerBtn = document.getElementById('toggleServerBtn');
    if (toggleServerBtn) {
        toggleServerBtn.addEventListener('click', async () => {
            try {
                const targetState = !serverIsPaused;
                const res = await fetch('/api/status/toggle', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ paused: targetState })
                });
                if(res.ok) {
                    const result = await res.json();
                    showToast(result.paused ? 'Motion Capture Paused' : 'Motion Capture Resumed');
                    fetchStats();
                } else {
                    showToast('Failed to toggle server state', 'error');
                }
            } catch(e) {
                showToast('Error toggling server state', 'error');
            }
        });
    }

    // Presentation commands listener
    const cmdButtons = document.querySelectorAll('.cmd-btn');
    cmdButtons.forEach(btn => {
        btn.addEventListener('click', async () => {
            const cmd = btn.dataset.command;
            if (!cmd) return;
            try {
                const res = await fetch('/api/command', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: cmd })
                });
                if (res.ok) {
                    showToast(`Command triggered: ${cmd}`);
                    addConsoleLog(`Triggered system command: ${cmd}`);
                } else {
                    const errData = await res.json();
                    showToast(`Command failed: ${errData.error || 'Unknown error'}`, 'error');
                    addConsoleLog(`Failed to trigger system command: ${cmd} (${errData.error || 'Unknown'})`, 'ERROR');
                }
            } catch(e) {
                showToast('Error sending command', 'error');
            }
        });
    });

    // Screen Mirroring WebSocket Client
    let mirrorSocket = null;
    const toggleMirrorBtn = document.getElementById('toggleMirrorBtn');
    const mirrorImage = document.getElementById('mirrorImage');
    const mirrorPlaceholder = document.getElementById('mirrorPlaceholder');
    const mirrorBtnIcon = document.getElementById('mirrorBtnIcon');
    const mirrorBtnText = document.getElementById('mirrorBtnText');

    if (toggleMirrorBtn) {
        toggleMirrorBtn.addEventListener('click', () => {
            if (mirrorSocket) {
                stopScreenMirror();
            } else {
                startScreenMirror();
            }
        });
    }

    function startScreenMirror() {
        if (mirrorSocket) return;
        
        const wsProto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const mirrorUrl = `${wsProto}//${window.location.host}/mirror`;
        
        mirrorSocket = new WebSocket(mirrorUrl);
        mirrorSocket.binaryType = 'blob';
        
        mirrorBtnIcon.textContent = 'stop';
        mirrorBtnText.textContent = 'Stop Stream';
        toggleMirrorBtn.style.background = 'linear-gradient(135deg, var(--accent-red), #dc2626)';
        
        mirrorPlaceholder.style.display = 'none';
        mirrorImage.style.display = 'block';
        
        addConsoleLog('Screen mirroring stream started');
        
        mirrorSocket.onmessage = (event) => {
            if (event.data instanceof Blob) {
                const url = URL.createObjectURL(event.data);
                const oldUrl = mirrorImage.src;
                mirrorImage.src = url;
                if (oldUrl && oldUrl.startsWith('blob:')) {
                    URL.revokeObjectURL(oldUrl);
                }
            }
        };
        
        mirrorSocket.onclose = () => {
            cleanupMirrorUI();
            addConsoleLog('Screen mirroring stream stopped', 'WARN');
        };
        
        mirrorSocket.onerror = (err) => {
            cleanupMirrorUI();
            addConsoleLog('Screen mirroring encountered an error', 'ERROR');
            showToast('Mirroring stream error', 'error');
        };
    }

    function stopScreenMirror() {
        if (mirrorSocket) {
            mirrorSocket.close();
            mirrorSocket = null;
        }
    }

    function cleanupMirrorUI() {
        mirrorSocket = null;
        mirrorBtnIcon.textContent = 'play_arrow';
        mirrorBtnText.textContent = 'Start Stream';
        toggleMirrorBtn.style.background = 'linear-gradient(135deg, var(--accent-blue), #2563eb)';
        mirrorImage.style.display = 'none';
        mirrorPlaceholder.style.display = 'flex';
    }

    function updateListenerBadge(elementId, enabled) {
        const el = document.getElementById(elementId);
        if (!el) return;
        if (enabled) {
            el.className = 'badge green-badge';
            el.textContent = 'Active';
        } else {
            el.className = 'badge yellow-badge';
            el.textContent = 'Disabled';
        }
    }
});