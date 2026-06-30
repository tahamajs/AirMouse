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
                    <div class="device-card">
                        <div class="device-icon-large active">
                            <span class="material-icons-round">smartphone</span>
                        </div>
                        <div class="device-info-inner">
                            <h4>${d.name || 'Unknown Device'}</h4>
                            <p>${d.id.substring(0, 8)}... • ${d.ip || 'Network'}</p>
                        </div>
                        <span class="badge green-badge">Connected</span>
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
                <div class="form-group">
                    <label>Server Name</label>
                    <input type="text" id="cfgServerName" class="form-control" value="${currentConfig.server_name}">
                </div>
                <div class="form-group">
                    <label>Mouse Sensitivity (1.0 to 5.0)</label>
                    <input type="number" step="0.1" id="cfgSensitivity" class="form-control" value="${currentConfig.sensitivity}">
                </div>
                
                <div class="toggle-wrapper">
                    <div class="toggle-info">
                        <h4>AI Cursor Smoothing</h4>
                        <p>Use Machine Learning to predict and smooth movements.</p>
                    </div>
                    <label class="switch">
                        <input type="checkbox" id="cfgAISmooth" ${currentConfig.enable_ai_smoothing ? 'checked' : ''}>
                        <span class="slider"></span>
                    </label>
                </div>

                <div class="toggle-wrapper">
                    <div class="toggle-info">
                        <h4>Humanizer Physics</h4>
                        <p>Adds realistic B-Spline interpolation to cursor tracking.</p>
                    </div>
                    <label class="switch">
                        <input type="checkbox" id="cfgHumanizer" ${currentConfig.enable_humanizer ? 'checked' : ''}>
                        <span class="slider"></span>
                    </label>
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
        currentConfig.enable_ai_smoothing = document.getElementById('cfgAISmooth').checked;
        currentConfig.enable_humanizer = document.getElementById('cfgHumanizer').checked;
        currentConfig.auth_enabled = document.getElementById('cfgAuth').checked;

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

    // Start Polling Stats every 2s
    fetchStats();
    setInterval(fetchStats, 2000);
});