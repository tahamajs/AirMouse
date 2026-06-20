// ============================================================
// Air Mouse Pro - Web UI Controller
// ============================================================

const API_BASE = '';

// State
const state = {
    serverRunning: false,
    devices: [],
    logs: [],
    stats: { clicks: 0, scrolls: 0, devices: 0 },
    gestureTemplates: [],
    charts: {},
    pollingInterval: null,
    logPollingInterval: null,
    isDark: true,
    qrData: null
};

// ============================================================
// DOM References
// ============================================================

const $ = (id) => document.getElementById(id);
const $$ = (sel) => document.querySelectorAll(sel);

const elements = {
    // Navigation
    navItems: $$('.nav-item'),
    pages: $$('.page'),
    pageTitle: $('pageTitle'),
    menuToggle: $('menuToggle'),
    sidebar: $('sidebar'),

    // Stats
    statClicks: $('statClicks'),
    statScrolls: $('statScrolls'),
    statDevices: $('statDevices'),
    statUptime: $('statUptime'),

    // Server
    startBtn: $('startServerBtn'),
    stopBtn: $('stopServerBtn'),
    restartBtn: $('restartServerBtn'),
    statusIndicator: $('serverStatusIndicator'),
    statusText: $('serverStatusText'),

    // QR
    qrContainer: $('qrContainer'),
    refreshQrBtn: $('refreshQrBtn'),

    // Devices
    devicesGrid: $('devicesGrid'),
    refreshDevicesBtn: $('refreshDevicesBtn'),

    // Network
    serverIp: $('serverIp'),
    tcpPort: $('tcpPort'),
    wsPort: $('wsPort'),
    udpPort: $('udpPort'),
    copyIpBtn: $('copyIpBtn'),
    saveNetworkBtn: $('saveNetworkBtn'),

    // Gestures
    gesturesList: $('gesturesList'),
    addGestureBtn: $('addGestureBtn'),

    // Proximity
    proximityEnabled: $('proximityEnabled'),
    nearThreshold: $('nearThreshold'),
    nearThresholdValue: $('nearThresholdValue'),
    farThreshold: $('farThreshold'),
    farThresholdValue: $('farThresholdValue'),
    currentDistance: $('currentDistance'),
    lockStatus: $('lockStatus'),

    // Settings
    serverName: $('serverName'),
    themeSelect: $('themeSelect'),
    languageSelect: $('languageSelect'),
    sensitivity: $('sensitivity'),
    sensitivityValue: $('sensitivityValue'),
    smoothingEnabled: $('smoothingEnabled'),
    accelerationEnabled: $('accelerationEnabled'),
    aiSmoothing: $('aiSmoothing'),
    predictiveMovement: $('predictiveMovement'),
    blendFactor: $('blendFactor'),
    blendFactorValue: $('blendFactorValue'),
    authEnabled: $('authEnabled'),
    authToken: $('authToken'),
    generateTokenBtn: $('generateTokenBtn'),
    saveSettingsBtn: $('saveSettingsBtn'),
    settingsTabs: $$('.tab-btn'),
    tabContents: $$('.tab-content'),

    // Logs
    logContainer: $('logContainer'),
    clearLogsBtn: $('clearLogsBtn'),
    exportLogsBtn: $('exportLogsBtn'),
    logLevelFilter: $('logLevelFilter'),

    // Analytics
    resetStatsBtn: $('resetStatsBtn'),
    totalMovements: $('totalMovements'),
    avgSpeed: $('avgSpeed'),
    totalClicks: $('totalClicks'),
    totalScrolls: $('totalScrolls'),
    sessionDuration: $('sessionDuration'),

    // Charts
    speedChart: $('speedChart'),
    pingChart: $('pingChart'),
    movementChart: $('movementChart'),
    usageChart: $('usageChart'),
    refreshSpeedChart: $('refreshSpeedChart'),

    // Toast
    toast: $('toast'),
    notificationBell: $('notificationBell')
};

// ============================================================
// Navigation
// ============================================================

elements.navItems.forEach(item => {
    item.addEventListener('click', () => {
        const page = item.dataset.page;
        navigateTo(page);
    });
});

elements.menuToggle.addEventListener('click', () => {
    elements.sidebar.classList.toggle('open');
});

function navigateTo(page) {
    // Update nav
    elements.navItems.forEach(n => n.classList.remove('active'));
    const navItem = document.querySelector(`.nav-item[data-page="${page}"]`);
    if (navItem) navItem.classList.add('active');

    // Update pages
    elements.pages.forEach(p => p.classList.remove('active'));
    const pageEl = document.getElementById(`${page}Page`);
    if (pageEl) pageEl.classList.add('active');

    // Update title
    const titles = {
        dashboard: 'Dashboard',
        devices: 'Devices',
        network: 'Network',
        gestures: 'Gestures',
        proximity: 'Proximity',
        analytics: 'Analytics',
        settings: 'Settings',
        logs: 'Logs'
    };
    elements.pageTitle.textContent = titles[page] || page;

    // Close sidebar on mobile
    elements.sidebar.classList.remove('open');

    // Trigger page-specific load
    switch(page) {
        case 'devices': loadDevices(); break;
        case 'gestures': loadGestures(); break;
        case 'logs': loadLogs(); break;
        case 'analytics': loadAnalytics(); break;
        case 'network': loadNetworkSettings(); break;
        case 'settings': loadSettings(); break;
        case 'proximity': loadProximitySettings(); break;
    }
}

// ============================================================
// Server Controls
// ============================================================

elements.startBtn.addEventListener('click', startServer);
elements.stopBtn.addEventListener('click', stopServer);
elements.restartBtn.addEventListener('click', restartServer);

async function startServer() {
    try {
        const resp = await fetch('/api/start', { method: 'POST' });
        const data = await resp.json();
        if (data.success) {
            state.serverRunning = true;
            updateServerStatus();
            showToast('Server started successfully', 'success');
        } else {
            showToast('Failed to start server: ' + data.error, 'error');
        }
    } catch (e) {
        showToast('Error starting server', 'error');
    }
}

async function stopServer() {
    try {
        const resp = await fetch('/api/stop', { method: 'POST' });
        const data = await resp.json();
        if (data.success) {
            state.serverRunning = false;
            updateServerStatus();
            showToast('Server stopped', 'info');
        } else {
            showToast('Failed to stop server', 'error');
        }
    } catch (e) {
        showToast('Error stopping server', 'error');
    }
}

async function restartServer() {
    await stopServer();
    setTimeout(() => startServer(), 1000);
}

function updateServerStatus() {
    if (state.serverRunning) {
        elements.statusIndicator.className = 'status-indicator online';
        elements.statusText.textContent = 'Running';
        elements.startBtn.disabled = true;
        elements.stopBtn.disabled = false;
    } else {
        elements.statusIndicator.className = 'status-indicator offline';
        elements.statusText.textContent = 'Offline';
        elements.startBtn.disabled = false;
        elements.stopBtn.disabled = true;
    }
}

// ============================================================
// Dashboard Stats & Charts
// ============================================================

function initCharts() {
    // Speed Chart
    const ctx1 = elements.speedChart.getContext('2d');
    state.charts.speed = new Chart(ctx1, {
        type: 'line',
        data: {
            labels: Array.from({length: 20}, (_, i) => i),
            datasets: [{
                label: 'Speed (px/s)',
                data: Array(20).fill(0),
                borderColor: '#6366f1',
                backgroundColor: 'rgba(99,102,241,0.1)',
                fill: true,
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { display: false },
                y: { display: true, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });

    // Ping Chart
    const ctx2 = elements.pingChart.getContext('2d');
    state.charts.ping = new Chart(ctx2, {
        type: 'line',
        data: {
            labels: Array.from({length: 30}, (_, i) => i),
            datasets: [{
                label: 'Latency (ms)',
                data: Array(30).fill(0),
                borderColor: '#10b981',
                backgroundColor: 'rgba(16,185,129,0.1)',
                fill: true,
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { display: false },
                y: { display: true, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });

    // Movement Chart
    const ctx3 = elements.movementChart.getContext('2d');
    state.charts.movement = new Chart(ctx3, {
        type: 'scatter',
        data: {
            datasets: [{
                label: 'Movement',
                data: [],
                backgroundColor: '#6366f1',
                pointRadius: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });

    // Usage Chart
    const ctx4 = elements.usageChart.getContext('2d');
    state.charts.usage = new Chart(ctx4, {
        type: 'bar',
        data: {
            labels: ['0-6', '6-12', '12-18', '18-24'],
            datasets: [{
                label: 'Usage',
                data: [0, 0, 0, 0],
                backgroundColor: ['#6366f1', '#8b5cf6', '#a78bfa', '#c4b5fd']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false } },
                y: { grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
}

async function updateDashboard() {
    try {
        const resp = await fetch('/api/stats');
        const data = await resp.json();
        if (data) {
            elements.statClicks.textContent = data.clicks || 0;
            elements.statScrolls.textContent = data.scrolls || 0;
            elements.statDevices.textContent = data.devices || 0;
            if (data.uptime) {
                elements.statUptime.textContent = formatDuration(data.uptime);
            }
            // Update charts with new data
            if (state.charts.speed && data.speedHistory) {
                state.charts.speed.data.datasets[0].data = data.speedHistory;
                state.charts.speed.update();
            }
            if (state.charts.ping && data.pingHistory) {
                state.charts.ping.data.datasets[0].data = data.pingHistory;
                state.charts.ping.update();
            }
            // Update connection quality
            updateConnectionQuality(data.quality);
        }
    } catch (e) {
        // Silently fail
    }
}

function updateConnectionQuality(quality) {
    const indicator = document.querySelector('.quality-indicator');
    if (!indicator) return;
    const latency = document.getElementById('qualityLatency');
    const signal = document.getElementById('qualitySignal');

    if (quality) {
        const level = quality.level || 'good';
        indicator.className = `quality-indicator ${level}`;
        indicator.textContent = quality.text || 'Good';
        if (latency) latency.textContent = `Latency: ${quality.latency || 0} ms`;
        if (signal) signal.textContent = `Signal: ${quality.signal || 'Good'}`;
    } else {
        indicator.className = 'quality-indicator poor';
        indicator.textContent = 'N/A';
    }
}

function formatDuration(seconds) {
    const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
    const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
    const s = String(Math.floor(seconds % 60)).padStart(2, '0');
    return `${h}:${m}:${s}`;
}

// ============================================================
// QR Code
// ============================================================

elements.refreshQrBtn.addEventListener('click', loadQRCode);

async function loadQRCode() {
    try {
        const resp = await fetch('/api/qrcode');
        if (resp.ok) {
            const data = await resp.text();
            elements.qrContainer.innerHTML = `<img src="${data}" alt="QR Code" style="max-width:200px">`;
        } else {
            elements.qrContainer.innerHTML = '<div class="qr-placeholder">QR unavailable</div>';
        }
    } catch (e) {
        elements.qrContainer.innerHTML = '<div class="qr-placeholder">QR unavailable</div>';
    }
}

// ============================================================
// Devices
// ============================================================

elements.refreshDevicesBtn.addEventListener('click', loadDevices);

async function loadDevices() {
    try {
        const resp = await fetch('/api/devices');
        const devices = await resp.json();
        if (devices && devices.length > 0) {
            elements.devicesGrid.innerHTML = devices.map(d => `
                <div class="device-card">
                    <div class="device-icon">${d.type === 'mobile' ? '📱' : '💻'}</div>
                    <div class="device-info">
                        <div class="device-name">${d.name || 'Unknown Device'}</div>
                        <div class="device-type">${d.type || 'Unknown'} • ${d.ip || 'N/A'}</div>
                    </div>
                    <div class="device-status ${d.connected ? 'online' : 'offline'}">${d.connected ? '● Online' : '○ Offline'}</div>
                </div>
            `).join('');
        } else {
            elements.devicesGrid.innerHTML = '<div class="loading">No devices connected</div>';
        }
    } catch (e) {
        elements.devicesGrid.innerHTML = '<div class="loading">Failed to load devices</div>';
    }
}

// ============================================================
// Network Settings
// ============================================================

elements.copyIpBtn.addEventListener('click', () => {
    const ip = elements.serverIp.value;
    if (ip) {
        navigator.clipboard?.writeText(ip);
        showToast('IP copied to clipboard', 'success');
    }
});

elements.saveNetworkBtn.addEventListener('click', saveNetworkSettings);

async function loadNetworkSettings() {
    try {
        const resp = await fetch('/api/config/network');
        const data = await resp.json();
        if (data) {
            elements.serverIp.value = data.ip || '127.0.0.1';
            elements.tcpPort.value = data.tcp_port || 8080;
            elements.wsPort.value = data.ws_port || 8081;
            elements.udpPort.value = data.udp_port || 8082;
        }
    } catch (e) {
        // Use defaults
    }
}

async function saveNetworkSettings() {
    const data = {
        tcp_port: parseInt(elements.tcpPort.value) || 8080,
        ws_port: parseInt(elements.wsPort.value) || 8081,
        udp_port: parseInt(elements.udpPort.value) || 8082
    };
    try {
        const resp = await fetch('/api/config/network', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (resp.ok) {
            showToast('Network settings saved', 'success');
        } else {
            showToast('Failed to save network settings', 'error');
        }
    } catch (e) {
        showToast('Error saving network settings', 'error');
    }
}

// ============================================================
// Gestures
// ============================================================

elements.addGestureBtn.addEventListener('click', showAddGestureDialog);

async function loadGestures() {
    try {
        const resp = await fetch('/api/gestures');
        const gestures = await resp.json();
        if (gestures && gestures.length > 0) {
            elements.gesturesList.innerHTML = gestures.map(g => `
                <div class="gesture-item">
                    <span class="gesture-name">${g.name || 'Unknown'}</span>
                    <span class="gesture-action">${g.action || 'No action'}</span>
                    <span class="gesture-confidence">${(g.confidence || 0) * 100}%</span>
                </div>
            `).join('');
        } else {
            elements.gesturesList.innerHTML = '<div class="loading">No gestures configured</div>';
        }
    } catch (e) {
        elements.gesturesList.innerHTML = '<div class="loading">Failed to load gestures</div>';
    }
}

function showAddGestureDialog() {
    // Simple prompt - can be enhanced with a proper modal
    const name = prompt('Enter gesture name:');
    if (name) {
        const action = prompt('Enter action (e.g., Play/Pause):');
        if (action) {
            fetch('/api/gestures', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, action })
            }).then(() => {
                showToast('Gesture added', 'success');
                loadGestures();
            }).catch(() => {
                showToast('Failed to add gesture', 'error');
            });
        }
    }
}

// ============================================================
// Proximity
// ============================================================

elements.nearThreshold.addEventListener('input', () => {
    const val = parseFloat(elements.nearThreshold.value);
    elements.nearThresholdValue.textContent = val + ' m';
});

elements.farThreshold.addEventListener('input', () => {
    const val = parseFloat(elements.farThreshold.value);
    elements.farThresholdValue.textContent = val + ' m';
});

async function loadProximitySettings() {
    try {
        const resp = await fetch('/api/config/proximity');
        const data = await resp.json();
        if (data) {
            elements.proximityEnabled.checked = data.enabled || false;
            elements.nearThreshold.value = data.near_threshold || 1.5;
            elements.nearThresholdValue.textContent = (data.near_threshold || 1.5) + ' m';
            elements.farThreshold.value = data.far_threshold || 3.0;
            elements.farThresholdValue.textContent = (data.far_threshold || 3.0) + ' m';
            if (data.distance) {
                elements.currentDistance.textContent = data.distance + ' m';
                elements.lockStatus.textContent = data.locked ? '🔒 Locked' : '🔓 Unlocked';
                elements.lockStatus.className = `lock-status ${data.locked ? 'locked' : 'unlocked'}`;
            }
        }
    } catch (e) {
        // Use defaults
    }
}

// ============================================================
// Settings
// ============================================================

elements.sensitivity.addEventListener('input', () => {
    elements.sensitivityValue.textContent = parseFloat(elements.sensitivity.value).toFixed(2);
});

elements.blendFactor.addEventListener('input', () => {
    elements.blendFactorValue.textContent = parseFloat(elements.blendFactor.value).toFixed(2);
});

elements.settingsTabs.forEach(tab => {
    tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;
        elements.settingsTabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        elements.tabContents.forEach(c => c.classList.remove('active'));
        const content = document.getElementById(tabName + 'Tab');
        if (content) content.classList.add('active');
    });
});

elements.saveSettingsBtn.addEventListener('click', saveSettings);
elements.generateTokenBtn.addEventListener('click', generateToken);

async function loadSettings() {
    try {
        const resp = await fetch('/api/config');
        const data = await resp.json();
        if (data) {
            elements.serverName.value = data.server_name || '';
            elements.themeSelect.value = data.theme || 'dark';
            elements.languageSelect.value = data.language || 'en';
            elements.sensitivity.value = data.sensitivity || 1.0;
            elements.sensitivityValue.textContent = (data.sensitivity || 1.0).toFixed(2);
            elements.smoothingEnabled.checked = data.smoothing || false;
            elements.accelerationEnabled.checked = data.acceleration || false;
            elements.aiSmoothing.checked = data.ai_smoothing || false;
            elements.predictiveMovement.checked = data.predictive || false;
            elements.blendFactor.value = data.blend_factor || 0.6;
            elements.blendFactorValue.textContent = (data.blend_factor || 0.6).toFixed(2);
            elements.authEnabled.checked = data.auth_enabled || false;
            if (data.auth_token) {
                elements.authToken.value = data.auth_token;
            }
        }
    } catch (e) {
        // Use defaults
    }
}

async function saveSettings() {
    const data = {
        server_name: elements.serverName.value,
        theme: elements.themeSelect.value,
        language: elements.languageSelect.value,
        sensitivity: parseFloat(elements.sensitivity.value),
        smoothing: elements.smoothingEnabled.checked,
        acceleration: elements.accelerationEnabled.checked,
        ai_smoothing: elements.aiSmoothing.checked,
        predictive: elements.predictiveMovement.checked,
        blend_factor: parseFloat(elements.blendFactor.value),
        auth_enabled: elements.authEnabled.checked,
        auth_token: elements.authToken.value
    };
    try {
        const resp = await fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (resp.ok) {
            showToast('Settings saved', 'success');
        } else {
            showToast('Failed to save settings', 'error');
        }
    } catch (e) {
        showToast('Error saving settings', 'error');
    }
}

async function generateToken() {
    try {
        const resp = await fetch('/api/auth/token', { method: 'POST' });
        const data = await resp.json();
        if (data.token) {
            elements.authToken.value = data.token;
            showToast('New token generated', 'success');
        }
    } catch (e) {
        showToast('Failed to generate token', 'error');
    }
}

// ============================================================
// Logs
// ============================================================

elements.clearLogsBtn.addEventListener('click', clearLogs);
elements.exportLogsBtn.addEventListener('click', exportLogs);
elements.logLevelFilter.addEventListener('change', loadLogs);

async function loadLogs() {
    const level = elements.logLevelFilter.value;
    try {
        const resp = await fetch(`/api/logs?level=${level}`);
        const logs = await resp.json();
        if (logs && logs.length > 0) {
            elements.logContainer.innerHTML = logs.map(log => `
                <div class="log-entry ${log.level || 'info'}">
                    [${log.time || '--:--:--'}] ${log.message || 'No message'}
                </div>
            `).join('');
        } else {
            elements.logContainer.innerHTML = '<div class="log-entry system">No logs available</div>';
        }
    } catch (e) {
        elements.logContainer.innerHTML = '<div class="log-entry error">Failed to load logs</div>';
    }
}

async function clearLogs() {
    try {
        await fetch('/api/logs', { method: 'DELETE' });
        showToast('Logs cleared', 'info');
        loadLogs();
    } catch (e) {
        showToast('Failed to clear logs', 'error');
    }
}

async function exportLogs() {
    try {
        const resp = await fetch('/api/logs/export');
        const blob = await resp.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `airmouse_logs_${new Date().toISOString().slice(0,10)}.txt`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('Logs exported', 'success');
    } catch (e) {
        showToast('Failed to export logs', 'error');
    }
}

// ============================================================
// Analytics
// ============================================================

elements.resetStatsBtn.addEventListener('click', resetStatistics);

async function loadAnalytics() {
    try {
        const resp = await fetch('/api/stats/detailed');
        const data = await resp.json();
        if (data) {
            elements.totalMovements.textContent = data.total_movements || 0;
            elements.avgSpeed.textContent = (data.avg_speed || 0) + ' px/s';
            elements.totalClicks.textContent = data.total_clicks || 0;
            elements.totalScrolls.textContent = data.total_scrolls || 0;
            elements.sessionDuration.textContent = formatDuration(data.session_duration || 0);
            // Update movement chart
            if (state.charts.movement && data.movement_points) {
                state.charts.movement.data.datasets[0].data = data.movement_points;
                state.charts.movement.update();
            }
            // Update usage chart
            if (state.charts.usage && data.usage_by_hour) {
                state.charts.usage.data.datasets[0].data = data.usage_by_hour;
                state.charts.usage.update();
            }
        }
    } catch (e) {
        // Silently fail
    }
}

async function resetStatistics() {
    if (confirm('Reset all statistics?')) {
        try {
            await fetch('/api/stats/reset', { method: 'POST' });
            showToast('Statistics reset', 'info');
            loadAnalytics();
        } catch (e) {
            showToast('Failed to reset statistics', 'error');
        }
    }
}

// ============================================================
// Toast Notifications
// ============================================================

function showToast(message, type = 'info') {
    const toast = elements.toast;
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.add('show');
    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// ============================================================
// Polling
// ============================================================

function startPolling() {
    // Dashboard stats - every 2 seconds
    if (state.pollingInterval) clearInterval(state.pollingInterval);
    state.pollingInterval = setInterval(() => {
        updateDashboard();
    }, 2000);

    // Logs - every 3 seconds if logs page is active
    if (state.logPollingInterval) clearInterval(state.logPollingInterval);
    state.logPollingInterval = setInterval(() => {
        const logsPage = document.getElementById('logsPage');
        if (logsPage && logsPage.classList.contains('active')) {
            loadLogs();
        }
    }, 3000);
}

// ============================================================
// Keyboard Shortcuts
// ============================================================

document.addEventListener('keydown', (e) => {
    // Ctrl+1-8 for navigation
    if (e.ctrlKey && e.key >= '1' && e.key <= '8') {
        e.preventDefault();
        const pages = ['dashboard', 'devices', 'network', 'gestures', 'proximity', 'analytics', 'settings', 'logs'];
        const idx = parseInt(e.key) - 1;
        if (idx < pages.length) {
            navigateTo(pages[idx]);
        }
    }
    // Escape to close sidebar
    if (e.key === 'Escape') {
        elements.sidebar.classList.remove('open');
    }
});

// ============================================================
// Init
// ============================================================

function init() {
    // Set initial state
    updateServerStatus();

    // Load initial data
    loadQRCode();
    loadDevices();
    loadNetworkSettings();
    loadGestures();
    loadProximitySettings();
    loadSettings();
    loadLogs();
    loadAnalytics();

    // Init charts
    initCharts();

    // Start polling
    startPolling();

    // Check server status
    fetch('/api/status')
        .then(r => r.json())
        .then(data => {
            state.serverRunning = data.running || false;
            updateServerStatus();
        })
        .catch(() => {});

    console.log('🎯 Air Mouse Pro Web UI initialized');
}

// Run on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}