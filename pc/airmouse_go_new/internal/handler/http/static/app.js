// Air Mouse Pro Web Interface
// Full-featured dashboard with real-time updates

// ============================================
// Global Variables
// ============================================
let speedChart = null;
let pingChart = null;
let movementChart = null;
let usageChart = null;
let updateInterval = null;
let wsConnection = null;
let reconnectAttempts = 0;
let maxReconnectAttempts = 10;
let notificationCount = 0;

// DOM Elements
const elements = {};

// ============================================
// Initialization
// ============================================
document.addEventListener('DOMContentLoaded', () => {
    initializeElements();
    initializeCharts();
    initializeEventListeners();
    loadInitialData();
    startAutoRefresh();
    connectWebSocket();
    checkServerStatus();
});

function initializeElements() {
    const ids = [
        'dashboardPage', 'devicesPage', 'networkPage', 'gesturesPage',
        'proximityPage', 'analyticsPage', 'settingsPage', 'logsPage',
        'statClicks', 'statScrolls', 'statDevices', 'statUptime',
        'serverStatusIndicator', 'serverStatusText', 'pageTitle',
        'startServerBtn', 'stopServerBtn', 'restartServerBtn',
        'refreshDevicesBtn', 'refreshQrBtn', 'copyIpBtn', 'saveNetworkBtn',
        'addGestureBtn', 'saveSettingsBtn', 'clearLogsBtn', 'exportLogsBtn',
        'resetStatsBtn', 'generateTokenBtn', 'refreshSpeedChart',
        'serverIp', 'tcpPort', 'wsPort', 'udpPort', 'serverName',
        'themeSelect', 'languageSelect', 'sensitivity', 'sensitivityValue',
        'smoothingEnabled', 'accelerationEnabled', 'aiSmoothing',
        'predictiveMovement', 'blendFactor', 'blendFactorValue',
        'authEnabled', 'authToken', 'proximityEnabled',
        'nearThreshold', 'nearThresholdValue', 'farThreshold', 'farThresholdValue',
        'currentDistance', 'lockStatus', 'qualityIndicator', 'qualityLatency', 'qualitySignal',
        'totalMovements', 'avgSpeed', 'totalClicks', 'totalScrolls', 'sessionDuration',
        'logLevelFilter', 'logContainer', 'qrContainer', 'devicesGrid', 'gesturesList',
        'notificationBell'
    ];
    
    ids.forEach(id => {
        elements[id] = document.getElementById(id);
    });
}

function initializeCharts() {
    // Speed Chart
    const speedCtx = document.getElementById('speedChart')?.getContext('2d');
    if (speedCtx) {
        speedChart = new Chart(speedCtx, {
            type: 'line',
            data: {
                labels: Array(30).fill(''),
                datasets: [{
                    label: 'Speed (px/s)',
                    data: Array(30).fill(0),
                    borderColor: '#6366f1',
                    backgroundColor: 'rgba(99, 102, 241, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointRadius: 0,
                    pointHoverRadius: 5
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { position: 'top', labels: { color: '#cbd5e1' } },
                    tooltip: { mode: 'index', intersect: false }
                },
                scales: {
                    y: { grid: { color: '#334155' }, ticks: { color: '#94a3b8' } },
                    x: { grid: { display: false }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }
    
    // Ping Chart
    const pingCtx = document.getElementById('pingChart')?.getContext('2d');
    if (pingCtx) {
        pingChart = new Chart(pingCtx, {
            type: 'line',
            data: {
                labels: Array(20).fill(''),
                datasets: [{
                    label: 'Latency (ms)',
                    data: Array(20).fill(0),
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    tension: 0.3,
                    fill: true,
                    pointRadius: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { position: 'top', labels: { color: '#cbd5e1' } }
                },
                scales: {
                    y: { grid: { color: '#334155' }, ticks: { color: '#94a3b8' } },
                    x: { grid: { display: false }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }
    
    // Movement Chart
    const movementCtx = document.getElementById('movementChart')?.getContext('2d');
    if (movementCtx) {
        movementChart = new Chart(movementCtx, {
            type: 'doughnut',
            data: {
                labels: ['Clicks', 'Scrolls', 'Movements'],
                datasets: [{
                    data: [0, 0, 0],
                    backgroundColor: ['#6366f1', '#10b981', '#f59e0b'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { position: 'bottom', labels: { color: '#cbd5e1' } }
                }
            }
        });
    }
    
    // Usage Chart
    const usageCtx = document.getElementById('usageChart')?.getContext('2d');
    if (usageCtx) {
        usageChart = new Chart(usageCtx, {
            type: 'bar',
            data: {
                labels: Array(24).fill('').map((_, i) => `${i}:00`),
                datasets: [{
                    label: 'Usage',
                    data: Array(24).fill(0),
                    backgroundColor: '#6366f1',
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: { grid: { color: '#334155' }, ticks: { color: '#94a3b8' } },
                    x: { grid: { display: false }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }
}

function initializeEventListeners() {
    // Navigation
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const page = item.dataset.page;
            switchPage(page);
        });
    });
    
    // Buttons
    if (elements.startServerBtn) elements.startServerBtn.addEventListener('click', startServer);
    if (elements.stopServerBtn) elements.stopServerBtn.addEventListener('click', stopServer);
    if (elements.restartServerBtn) elements.restartServerBtn.addEventListener('click', restartServer);
    if (elements.refreshDevicesBtn) elements.refreshDevicesBtn.addEventListener('click', loadDevices);
    if (elements.refreshQrBtn) elements.refreshQrBtn.addEventListener('click', loadQRCode);
    if (elements.copyIpBtn) elements.copyIpBtn.addEventListener('click', copyServerIp);
    if (elements.saveNetworkBtn) elements.saveNetworkBtn.addEventListener('click', saveNetworkSettings);
    if (elements.addGestureBtn) elements.addGestureBtn.addEventListener('click', showAddGestureDialog);
    if (elements.saveSettingsBtn) elements.saveSettingsBtn.addEventListener('click', saveAllSettings);
    if (elements.clearLogsBtn) elements.clearLogsBtn.addEventListener('click', clearLogs);
    if (elements.exportLogsBtn) elements.exportLogsBtn.addEventListener('click', exportLogs);
    if (elements.resetStatsBtn) elements.resetStatsBtn.addEventListener('click', resetStatistics);
    if (elements.generateTokenBtn) elements.generateTokenBtn.addEventListener('click', generateAuthToken);
    if (elements.refreshSpeedChart) elements.refreshSpeedChart.addEventListener('click', () => loadSpeedData());
    
    // Menu Toggle
    const menuToggle = document.getElementById('menuToggle');
    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            document.querySelector('.sidebar')?.classList.toggle('open');
        });
    }
    
    // Settings Tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            switchSettingsTab(tab);
        });
    });
    
    // Range Inputs
    if (elements.sensitivity) {
        elements.sensitivity.addEventListener('input', (e) => {
            if (elements.sensitivityValue) elements.sensitivityValue.textContent = parseFloat(e.target.value).toFixed(2);
        });
    }
    if (elements.blendFactor) {
        elements.blendFactor.addEventListener('input', (e) => {
            if (elements.blendFactorValue) elements.blendFactorValue.textContent = parseFloat(e.target.value).toFixed(2);
        });
    }
    if (elements.nearThreshold) {
        elements.nearThreshold.addEventListener('input', (e) => {
            if (elements.nearThresholdValue) elements.nearThresholdValue.textContent = parseFloat(e.target.value).toFixed(1) + ' m';
        });
    }
    if (elements.farThreshold) {
        elements.farThreshold.addEventListener('input', (e) => {
            if (elements.farThresholdValue) elements.farThresholdValue.textContent = parseFloat(e.target.value).toFixed(1) + ' m';
        });
    }
    
    // Log Level Filter
    if (elements.logLevelFilter) {
        elements.logLevelFilter.addEventListener('change', () => filterLogs());
    }
}

// ============================================
// Page Navigation
// ============================================
function switchPage(page) {
    // Update active nav item
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === page) item.classList.add('active');
    });
    
    // Update visible page
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    const targetPage = document.getElementById(`${page}Page`);
    if (targetPage) targetPage.classList.add('active');
    
    // Update page title
    const pageNames = {
        dashboard: 'Dashboard',
        devices: 'Devices',
        network: 'Network',
        gestures: 'Gestures',
        proximity: 'Proximity',
        analytics: 'Analytics',
        settings: 'Settings',
        logs: 'Logs'
    };
    if (elements.pageTitle) elements.pageTitle.textContent = pageNames[page] || 'Dashboard';
    
    // Load page-specific data
    switch(page) {
        case 'devices': loadDevices(); break;
        case 'network': loadNetworkSettings(); break;
        case 'gestures': loadGestures(); break;
        case 'proximity': loadProximitySettings(); break;
        case 'analytics': loadAnalytics(); break;
        case 'settings': loadSettings(); break;
        case 'logs': loadLogs(); break;
    }
}

function switchSettingsTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.tab === tab) btn.classList.add('active');
    });
    
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    const targetTab = document.getElementById(`${tab}Tab`);
    if (targetTab) targetTab.classList.add('active');
}

// ============================================
// Auto Refresh
// ============================================
function startAutoRefresh() {
    if (updateInterval) clearInterval(updateInterval);
    updateInterval = setInterval(() => {
        loadStats();
        loadServerStatus();
    }, 2000);
}

// ============================================
// Data Loading
// ============================================
async function loadInitialData() {
    await loadStats();
    await loadServerStatus();
    await loadDevices();
    await loadQRCode();
}

async function loadStats() {
    try {
        const response = await fetch('/api/stats');
        const data = await response.json();
        
        if (elements.statClicks) elements.statClicks.textContent = data.clicks || 0;
        if (elements.statScrolls) elements.statScrolls.textContent = data.scrolls || 0;
        if (elements.statDevices) elements.statDevices.textContent = data.devices || 0;
        
        // Update charts
        if (speedChart && data.speedHistory) {
            speedChart.data.datasets[0].data = data.speedHistory;
            speedChart.update();
        }
        
        if (movementChart) {
            movementChart.data.datasets[0].data = [data.clicks || 0, data.scrolls || 0, data.movements || 0];
            movementChart.update();
        }
        
        if (elements.totalMovements) elements.totalMovements.textContent = data.movements || 0;
        if (elements.avgSpeed) elements.avgSpeed.textContent = (data.avgSpeed || 0).toFixed(1);
        if (elements.totalClicks) elements.totalClicks.textContent = (data.clicks || 0);
        if (elements.totalScrolls) elements.totalScrolls.textContent = (data.scrolls || 0);
    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

async function loadServerStatus() {
    try {
        const response = await fetch('/api/status');
        const data = await response.json();
        
        const isRunning = data.running;
        const indicator = elements.serverStatusIndicator;
        const statusText = elements.serverStatusText;
        
        if (indicator) {
            if (isRunning) {
                indicator.classList.add('online');
                if (statusText) statusText.textContent = 'Online';
            } else {
                indicator.classList.remove('online');
                if (statusText) statusText.textContent = 'Offline';
            }
        }
        
        if (elements.statUptime && data.uptime) {
            elements.statUptime.textContent = formatUptime(data.uptime);
        }
        
        if (elements.qualityLatency && data.latency) {
            elements.qualityLatency.textContent = `Latency: ${data.latency} ms`;
            updateQualityIndicator(data.latency);
        }
        
        if (pingChart && data.pingHistory) {
            pingChart.data.datasets[0].data = data.pingHistory;
            pingChart.update();
        }
    } catch (error) {
        console.error('Failed to load server status:', error);
    }
}

async function loadDevices() {
    try {
        const response = await fetch('/api/devices');
        const devices = await response.json();
        
        const container = elements.devicesGrid;
        if (!container) return;
        
        if (!devices.length) {
            container.innerHTML = '<div class="loading">No devices connected</div>';
            return;
        }
        
        container.innerHTML = devices.map(device => `
            <div class="device-card">
                <div class="device-icon">📱</div>
                <div class="device-info">
                    <div class="device-name">${escapeHtml(device.name || 'Unknown')}</div>
                    <div class="device-details">${device.type || 'Unknown'} • ${device.ip || 'N/A'}</div>
                    <div class="device-details">Connected: ${formatTime(device.connected_at)}</div>
                </div>
                <div class="device-status ${device.active ? 'active' : ''}"></div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load devices:', error);
        if (elements.devicesGrid) elements.devicesGrid.innerHTML = '<div class="loading">Failed to load devices</div>';
    }
}

async function loadQRCode() {
    try {
        const response = await fetch('/api/qrcode');
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        
        const container = elements.qrContainer;
        if (container) {
            container.innerHTML = `<img src="${url}" alt="QR Code">`;
        }
    } catch (error) {
        console.error('Failed to load QR code:', error);
        if (elements.qrContainer) {
            elements.qrContainer.innerHTML = '<div class="qr-placeholder">Failed to load QR</div>';
        }
    }
}

async function loadNetworkSettings() {
    try {
        const response = await fetch('/api/config/network');
        const config = await response.json();
        
        if (elements.serverIp) elements.serverIp.value = config.ip || await getLocalIP();
        if (elements.tcpPort) elements.tcpPort.value = config.tcp_port || 8080;
        if (elements.wsPort) elements.wsPort.value = config.ws_port || 8081;
        if (elements.udpPort) elements.udpPort.value = config.udp_port || 8082;
    } catch (error) {
        console.error('Failed to load network settings:', error);
    }
}

async function loadGestures() {
    try {
        const response = await fetch('/api/gestures');
        const gestures = await response.json();
        
        const container = elements.gesturesList;
        if (!container) return;
        
        if (!gestures.length) {
            container.innerHTML = '<div class="loading">No gestures configured</div>';
            return;
        }
        
        container.innerHTML = gestures.map(gesture => `
            <div class="gesture-card">
                <div class="gesture-icon">${getGestureIcon(gesture.name)}</div>
                <div class="gesture-info">
                    <div class="gesture-name">${escapeHtml(gesture.name)}</div>
                    <div class="gesture-action">→ ${escapeHtml(gesture.action || 'No action')}</div>
                </div>
                <div class="gesture-confidence">${Math.round((gesture.confidence || 0) * 100)}%</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load gestures:', error);
    }
}

async function loadProximitySettings() {
    try {
        const response = await fetch('/api/proximity');
        const data = await response.json();
        
        if (elements.proximityEnabled) elements.proximityEnabled.checked = data.enabled;
        if (elements.nearThreshold) elements.nearThreshold.value = data.near_threshold || 1.5;
        if (elements.farThreshold) elements.farThreshold.value = data.far_threshold || 3.0;
        if (elements.currentDistance) elements.currentDistance.textContent = (data.distance || 0).toFixed(2) + ' m';
        
        if (elements.nearThresholdValue) {
            elements.nearThresholdValue.textContent = (data.near_threshold || 1.5).toFixed(1) + ' m';
        }
        if (elements.farThresholdValue) {
            elements.farThresholdValue.textContent = (data.far_threshold || 3.0).toFixed(1) + ' m';
        }
        
        if (elements.lockStatus) {
            const isLocked = data.is_locked;
            elements.lockStatus.textContent = isLocked ? '🔒 Locked' : '🔓 Unlocked';
            elements.lockStatus.className = `lock-status ${isLocked ? 'locked' : 'unlocked'}`;
        }
    } catch (error) {
        console.error('Failed to load proximity settings:', error);
    }
}

async function loadAnalytics() {
    try {
        const response = await fetch('/api/analytics');
        const data = await response.json();
        
        if (elements.sessionDuration && data.session_duration) {
            elements.sessionDuration.textContent = formatDuration(data.session_duration);
        }
        
        if (usageChart && data.usage_by_hour) {
            usageChart.data.datasets[0].data = data.usage_by_hour;
            usageChart.update();
        }
    } catch (error) {
        console.error('Failed to load analytics:', error);
    }
}

async function loadSettings() {
    try {
        const response = await fetch('/api/config');
        const config = await response.json();
        
        if (elements.serverName) elements.serverName.value = config.server_name || 'Air Mouse Pro';
        if (elements.themeSelect) elements.themeSelect.value = config.theme || 'dark';
        if (elements.languageSelect) elements.languageSelect.value = config.language || 'en';
        if (elements.sensitivity) elements.sensitivity.value = config.sensitivity || 1.0;
        if (elements.sensitivityValue) elements.sensitivityValue.textContent = (config.sensitivity || 1.0).toFixed(2);
        if (elements.smoothingEnabled) elements.smoothingEnabled.checked = config.smoothing_enabled !== false;
        if (elements.accelerationEnabled) elements.accelerationEnabled.checked = config.acceleration_enabled || false;
        if (elements.aiSmoothing) elements.aiSmoothing.checked = config.ai_smoothing || false;
        if (elements.predictiveMovement) elements.predictiveMovement.checked = config.predictive_enabled !== false;
        if (elements.blendFactor) elements.blendFactor.value = config.blend_factor || 0.6;
        if (elements.blendFactorValue) elements.blendFactorValue.textContent = (config.blend_factor || 0.6).toFixed(2);
        if (elements.authEnabled) elements.authEnabled.checked = config.auth_enabled || false;
        
        // Apply theme
        if (config.theme === 'light') {
            document.body.classList.add('light-theme');
        } else {
            document.body.classList.remove('light-theme');
        }
    } catch (error) {
        console.error('Failed to load settings:', error);
    }
}

async function loadLogs() {
    try {
        const response = await fetch('/api/logs');
        const logs = await response.json();
        
        const container = elements.logContainer;
        if (!container) return;
        
        const filter = elements.logLevelFilter?.value || 'all';
        
        container.innerHTML = logs.filter(log => filter === 'all' || log.level === filter)
            .map(log => `
                <div class="log-entry ${log.level}">
                    [${log.time}] [${log.level.toUpperCase()}] ${escapeHtml(log.message)}
                </div>
            `).join('');
        
        // Auto-scroll to bottom
        container.scrollTop = container.scrollHeight;
    } catch (error) {
        console.error('Failed to load logs:', error);
    }
}

async function loadSpeedData() {
    try {
        const response = await fetch('/api/speed');
        const data = await response.json();
        
        if (speedChart && data.history) {
            speedChart.data.datasets[0].data = data.history;
            speedChart.update();
        }
    } catch (error) {
        console.error('Failed to load speed data:', error);
    }
}

// ============================================
// Server Actions
// ============================================
async function startServer() {
    try {
        const response = await fetch('/api/start', { method: 'POST' });
        if (response.ok) {
            showToast('Server started successfully', 'success');
            loadServerStatus();
        } else {
            showToast('Failed to start server', 'error');
        }
    } catch (error) {
        showToast('Error starting server', 'error');
    }
}

async function stopServer() {
    try {
        const response = await fetch('/api/stop', { method: 'POST' });
        if (response.ok) {
            showToast('Server stopped', 'success');
            loadServerStatus();
        } else {
            showToast('Failed to stop server', 'error');
        }
    } catch (error) {
        showToast('Error stopping server', 'error');
    }
}

async function restartServer() {
    await stopServer();
    setTimeout(() => startServer(), 1000);
}

async function saveNetworkSettings() {
    const settings = {
        tcp_port: parseInt(elements.tcpPort?.value) || 8080,
        ws_port: parseInt(elements.wsPort?.value) || 8081,
        udp_port: parseInt(elements.udpPort?.value) || 8082
    };
    
    try {
        const response = await fetch('/api/config/network', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(settings)
        });
        
        if (response.ok) {
            showToast('Network settings saved', 'success');
        } else {
            showToast('Failed to save settings', 'error');
        }
    } catch (error) {
        showToast('Error saving settings', 'error');
    }
}

async function saveAllSettings() {
    const settings = {
        server_name: elements.serverName?.value || 'Air Mouse Pro',
        theme: elements.themeSelect?.value || 'dark',
        language: elements.languageSelect?.value || 'en',
        sensitivity: parseFloat(elements.sensitivity?.value) || 1.0,
        smoothing_enabled: elements.smoothingEnabled?.checked || false,
        acceleration_enabled: elements.accelerationEnabled?.checked || false,
        ai_smoothing: elements.aiSmoothing?.checked || false,
        predictive_enabled: elements.predictiveMovement?.checked || false,
        blend_factor: parseFloat(elements.blendFactor?.value) || 0.6,
        auth_enabled: elements.authEnabled?.checked || false,
        auth_token: elements.authToken?.value || ''
    };
    
    try {
        const response = await fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(settings)
        });
        
        if (response.ok) {
            showToast('Settings saved successfully', 'success');
            applyTheme(settings.theme);
        } else {
            showToast('Failed to save settings', 'error');
        }
    } catch (error) {
        showToast('Error saving settings', 'error');
    }
}

async function resetStatistics() {
    if (!confirm('Are you sure you want to reset all statistics?')) return;
    
    try {
        const response = await fetch('/api/stats/reset', { method: 'POST' });
        if (response.ok) {
            showToast('Statistics reset', 'success');
            loadStats();
            loadAnalytics();
        }
    } catch (error) {
        showToast('Error resetting statistics', 'error');
    }
}

async function generateAuthToken() {
    try {
        const response = await fetch('/api/auth/token', { method: 'POST' });
        const data = await response.json();
        if (elements.authToken) elements.authToken.value = data.token;
        showToast('New token generated', 'success');
    } catch (error) {
        showToast('Failed to generate token', 'error');
    }
}

function clearLogs() {
    if (confirm('Clear all logs?')) {
        fetch('/api/logs/clear', { method: 'POST' })
            .then(() => {
                showToast('Logs cleared', 'success');
                if (elements.logContainer) elements.logContainer.innerHTML = '';
            })
            .catch(() => showToast('Failed to clear logs', 'error'));
    }
}

function exportLogs() {
    window.open('/api/logs/export', '_blank');
}

function copyServerIp() {
    const ip = elements.serverIp?.value;
    if (ip) {
        navigator.clipboard.writeText(ip);
        showToast('IP copied to clipboard', 'success');
    }
}

// ============================================
// WebSocket Connection
// ============================================
function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;
    
    wsConnection = new WebSocket(wsUrl);
    
    wsConnection.onopen = () => {
        console.log('WebSocket connected');
        reconnectAttempts = 0;
        addNotification('Connected to server');
    };
    
    wsConnection.onmessage = (event) => {
        handleWebSocketMessage(event.data);
    };
    
    wsConnection.onclose = () => {
        console.log('WebSocket disconnected');
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            setTimeout(() => connectWebSocket(), 3000 * reconnectAttempts);
        }
    };
    
    wsConnection.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
}

function handleWebSocketMessage(data) {
    try {
        const message = JSON.parse(data);
        
        switch (message.type) {
            case 'stats':
                updateStatsUI(message.data);
                break;
            case 'device_connected':
            case 'device_disconnected':
                loadDevices();
                addNotification(`Device ${message.type === 'device_connected' ? 'connected' : 'disconnected'}`);
                break;
            case 'gesture':
                showToast(`Gesture detected: ${message.gesture}`, 'info');
                addNotification(`Gesture: ${message.gesture}`);
                break;
            case 'proximity':
                if (elements.currentDistance) {
                    elements.currentDistance.textContent = (message.distance || 0).toFixed(2) + ' m';
                }
                break;
            case 'log':
                addLogEntry(message);
                break;
            case 'notification':
                addNotification(message.text);
                break;
        }
    } catch (e) {
        console.error('Failed to parse WebSocket message:', e);
    }
}

// ============================================
// Helper Functions
// ============================================
function updateStatsUI(stats) {
    if (elements.statClicks) elements.statClicks.textContent = stats.clicks || 0;
    if (elements.statScrolls) elements.statScrolls.textContent = stats.scrolls || 0;
    if (elements.statDevices) elements.statDevices.textContent = stats.devices || 0;
}

function addLogEntry(log) {
    const container = elements.logContainer;
    if (!container) return;
    
    const filter = elements.logLevelFilter?.value || 'all';
    if (filter !== 'all' && log.level !== filter) return;
    
    const entry = document.createElement('div');
    entry.className = `log-entry ${log.level}`;
    entry.innerHTML = `[${log.time}] [${log.level.toUpperCase()}] ${escapeHtml(log.message)}`;
    container.appendChild(entry);
    container.scrollTop = container.scrollHeight;
    
    // Limit log entries
    while (container.children.length > 500) {
        container.removeChild(container.firstChild);
    }
}

function filterLogs() {
    loadLogs();
}

function updateQualityIndicator(latency) {
    const indicator = elements.qualityIndicator;
    if (!indicator) return;
    
    if (latency < 30) {
        indicator.className = 'quality-indicator good';
        if (elements.qualitySignal) elements.qualitySignal.textContent = 'Signal: Excellent';
    } else if (latency < 60) {
        indicator.className = 'quality-indicator';
        if (elements.qualitySignal) elements.qualitySignal.textContent = 'Signal: Good';
    } else if (latency < 100) {
        indicator.className = 'quality-indicator';
        if (elements.qualitySignal) elements.qualitySignal.textContent = 'Signal: Fair';
    } else {
        indicator.className = 'quality-indicator poor';
        if (elements.qualitySignal) elements.qualitySignal.textContent = 'Signal: Poor';
    }
}

function applyTheme(theme) {
    if (theme === 'light') {
        document.body.classList.add('light-theme');
    } else {
        document.body.classList.remove('light-theme');
    }
}

function showAddGestureDialog() {
    const name = prompt('Enter gesture name:');
    if (!name) return;
    
    const action = prompt('Enter action (e.g., play_pause, volume_up):');
    if (!action) return;
    
    fetch('/api/gestures', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, action })
    }).then(() => {
        showToast('Gesture added', 'success');
        loadGestures();
    }).catch(() => showToast('Failed to add gesture', 'error'));
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function addNotification(message) {
    notificationCount++;
    if (elements.notificationBell) {
        elements.notificationBell.classList.add('has-notification');
    }
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
        notificationCount--;
        if (notificationCount <= 0 && elements.notificationBell) {
            elements.notificationBell.classList.remove('has-notification');
        }
    }, 5000);
}

async function checkServerStatus() {
    try {
        const response = await fetch('/health');
        if (response.ok) {
            console.log('Server is healthy');
        }
    } catch (error) {
        console.error('Server health check failed:', error);
    }
}

async function getLocalIP() {
    try {
        const response = await fetch('/api/ip');
        const data = await response.json();
        return data.ip || '127.0.0.1';
    } catch {
        return '127.0.0.1';
    }
}

function formatUptime(seconds) {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function formatDuration(seconds) {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) return `${hours}h ${minutes}m`;
    if (minutes > 0) return `${minutes}m ${secs}s`;
    return `${secs}s`;
}

function formatTime(timestamp) {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
}

function getGestureIcon(gesture) {
    const icons = {
        'ThumbsUp': '👍',
        'ThumbsDown': '👎',
        'LeftSwipe': '👈',
        'RightSwipe': '👉',
        'UpSwipe': '👆',
        'DownSwipe': '👇',
        'CircleCW': '🔄',
        'CircleCCW': '🔄',
        'ZoomIn': '🔍+',
        'ZoomOut': '🔍-',
        'DoubleTap': '👆👆',
        'LongPress': '👆⏸',
        'Peace': '✌️',
        'Fist': '✊'
    };
    return icons[gesture] || '✋';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}