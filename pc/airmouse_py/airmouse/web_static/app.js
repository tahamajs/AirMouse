/**
 * AirMouse Mobile Web & iOS Client (Protocol v2)
 * Features:
 * - Protocol v2 compliant wire envelope (v: 2, seq, ts, id, nested payload)
 * - WebSocket / WSS transport with automatic URL discovery and pairing token query param
 * - HTML5 DeviceOrientation API with iOS Safari 13+ permission request
 * - Touchpad gestures:
 *    - 1-finger drag -> move
 *    - 1-finger tap -> left click
 *    - 2-finger tap -> right click
 *    - 2-finger drag -> scroll
 * - Device fingerprinting & persistent settings
 */

(function () {
    'use strict';

    // State Variables
    let ws = null;
    let seq = 0;
    let messageCounter = 0;
    let rateInterval = null;
    let isConnected = false;
    let lastActiveTimestamp = 0;

    // Gyroscope State
    let gyroActive = false;
    let lastAlpha = null;
    let lastBeta = null;
    let lastGamma = null;
    let lastGyroTime = 0;

    // Touch State
    let activeTouches = new Map();
    let touchStartTime = 0;
    let initialTouchCount = 0;
    let maxTouchCount = 0;
    let isDragging = false;
    let lastTouchX = 0;
    let lastTouchY = 0;
    let lastScrollY = 0;
    let totalMoveDistance = 0;

    // Config & Settings
    const TAP_THRESHOLD_MS = 250;
    const TAP_DISTANCE_THRESHOLD = 12; // px
    let touchSensitivity = 1.5;
    let gyroSensitivity = 1.8;
    let hapticEnabled = true;

    // DOM Elements
    const serverHostInput = document.getElementById('serverHost');
    const pairingTokenInput = document.getElementById('pairingToken');
    const btnConnect = document.getElementById('btnConnect');
    const btnGyroPermission = document.getElementById('btnGyroPermission');
    const btnFullscreen = document.getElementById('btnFullscreen');
    const btnSettingsToggle = document.getElementById('btnSettingsToggle');
    const settingsPanel = document.getElementById('settingsPanel');
    const touchSensitivitySlider = document.getElementById('touchSensitivity');
    const valTouchSens = document.getElementById('valTouchSens');
    const gyroSensitivitySlider = document.getElementById('gyroSensitivity');
    const valGyroSens = document.getElementById('valGyroSens');
    const gyroToggle = document.getElementById('gyroToggle');
    const hapticToggle = document.getElementById('hapticToggle');
    const connectionStatus = document.getElementById('connectionStatus');
    const statusText = document.getElementById('statusText');
    const touchpad = document.getElementById('touchpad');
    const touchFeedback = document.getElementById('touchFeedback');
    const statSeq = document.getElementById('statSeq');
    const statRate = document.getElementById('statRate');
    const statGyro = document.getElementById('statGyro');
    const btnLeftClick = document.getElementById('btnLeftClick');
    const btnMiddleClick = document.getElementById('btnMiddleClick');
    const btnRightClick = document.getElementById('btnRightClick');

    // Generate or retrieve persistent device fingerprint
    function getDeviceID() {
        let id = localStorage.getItem('airmouse_device_id');
        if (!id) {
            id = 'web-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now().toString(36);
            localStorage.setItem('airmouse_device_id', id);
        }
        return id;
    }

    // UUID Generator for Protocol v2 id field
    function generateUUID() {
        if (crypto && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = (Math.random() * 16) | 0;
            const v = c === 'x' ? r : (r & 0x3) | 0x8;
            return v.toString(16);
        });
    }

    // Load Settings
    function initSettings() {
        const urlParams = new URLSearchParams(window.location.search);
        
        // Host resolution: URL param -> localStorage -> default to current host:port
        const defaultHost = window.location.host || '127.0.0.1:8081';
        const savedHost = urlParams.get('host') || urlParams.get('ip') || localStorage.getItem('airmouse_host') || defaultHost;
        serverHostInput.value = savedHost;

        // Token resolution: URL param ?token=... -> localStorage
        const paramToken = urlParams.get('token');
        if (paramToken) {
            pairingTokenInput.value = paramToken;
            localStorage.setItem('airmouse_token', paramToken);
        } else {
            const savedToken = localStorage.getItem('airmouse_token') || '';
            pairingTokenInput.value = savedToken;
        }

        // Sensitivity
        const savedTouchSens = localStorage.getItem('airmouse_touch_sens');
        if (savedTouchSens) {
            touchSensitivity = parseFloat(savedTouchSens);
            touchSensitivitySlider.value = touchSensitivity;
            valTouchSens.textContent = touchSensitivity.toFixed(1);
        }

        const savedGyroSens = localStorage.getItem('airmouse_gyro_sens');
        if (savedGyroSens) {
            gyroSensitivity = parseFloat(savedGyroSens);
            gyroSensitivitySlider.value = gyroSensitivity;
            valGyroSens.textContent = gyroSensitivity.toFixed(1);
        }

        const savedHaptic = localStorage.getItem('airmouse_haptic');
        if (savedHaptic !== null) {
            hapticEnabled = savedHaptic === 'true';
            hapticToggle.checked = hapticEnabled;
        }

        // Message rate statistics timer
        rateInterval = setInterval(() => {
            statRate.textContent = messageCounter + ' msg/s';
            messageCounter = 0;
        }, 1000);
    }

    function triggerHaptic(type = 'light') {
        if (!hapticEnabled) return;
        if (navigator.vibrate) {
            if (type === 'light') navigator.vibrate(10);
            else if (type === 'medium') navigator.vibrate(25);
            else if (type === 'heavy') navigator.vibrate(45);
        }
    }

    // Protocol v2 Envelope Sender
    function sendProtocolV2(type, payload, correlationId) {
        if (!ws || ws.readyState !== WebSocket.OPEN) {
            return false;
        }

        seq += 1;
        statSeq.textContent = seq;
        messageCounter += 1;

        const envelope = {
            v: 2,
            type: type,
            seq: seq,
            ts: Date.now(),
            id: correlationId || generateUUID(),
            payload: payload || {}
        };

        try {
            ws.send(JSON.stringify(envelope));
            return true;
        } catch (e) {
            console.error('Failed to send envelope:', e);
            return false;
        }
    }

    // Connect to WebSocket / WSS
    function connect() {
        if (ws) {
            disconnect();
            return;
        }

        const host = serverHostInput.value.trim();
        const token = pairingTokenInput.value.trim();

        if (!host) {
            alert('Please specify the server address (e.g. 192.168.1.50:8081)');
            return;
        }

        localStorage.setItem('airmouse_host', host);
        if (token) localStorage.setItem('airmouse_token', token);

        // Determine protocol: WSS if page is HTTPS, or default to wss://
        let protocol = 'wss://';
        if (window.location.protocol === 'http:' && !host.includes('https')) {
            // allow fallback for local non-tls testing if explicitly on http:
            protocol = 'ws://';
        }

        let cleanHost = host.replace(/^wss?:\/\//, '').replace(/\/ws\/?$/, '');
        let targetUrl = `${protocol}${cleanHost}/ws`;
        if (token) {
            targetUrl += `?token=${encodeURIComponent(token)}`;
        }

        updateStatus('connecting', 'Connecting...');
        btnConnect.textContent = 'Connecting...';
        btnConnect.disabled = true;

        try {
            ws = new WebSocket(targetUrl);
        } catch (err) {
            console.error('WebSocket creation error:', err);
            updateStatus('disconnected', 'Connection Error');
            btnConnect.textContent = 'Connect (wss://)';
            btnConnect.disabled = false;
            return;
        }

        ws.onopen = function () {
            isConnected = true;
            updateStatus('connected', 'Connected');
            btnConnect.textContent = 'Disconnect';
            btnConnect.classList.add('connected');
            btnConnect.disabled = false;
            triggerHaptic('medium');

            // Send Protocol V2 Hello
            const userAgent = navigator.userAgent || 'WebClient';
            const isIOS = /iPad|iPhone|iPod/.test(userAgent);
            const clientName = isIOS ? 'iOS Safari AirMouse' : 'Mobile Web AirMouse';

            sendProtocolV2('hello', {
                device_id: getDeviceID(),
                name: clientName,
                device_name: clientName,
                model: isIOS ? 'Apple iOS' : 'Mobile Browser',
                brand: isIOS ? 'Apple' : 'Generic',
                version: '2.0.0',
                protocol: 'WEBSOCKET',
                transport: 'websocket',
                token: token
            });
        };

        ws.onmessage = function (event) {
            try {
                const data = JSON.parse(event.data);
                if (data.type === 'welcome') {
                    console.log('AirMouse Welcome received:', data);
                } else if (data.type === 'ack') {
                    // ACK received for click/scroll
                } else if (data.type === 'error') {
                    console.warn('AirMouse Server Error:', data.payload || data.error);
                }
            } catch (e) {
                // non-JSON message
            }
        };

        ws.onerror = function (e) {
            console.error('WebSocket Error:', e);
            updateStatus('disconnected', 'WS Error');
        };

        ws.onclose = function (e) {
            console.log('WebSocket closed code:', e.code, e.reason);
            disconnect();
        };
    }

    function disconnect() {
        if (ws) {
            try {
                ws.close();
            } catch (e) {}
            ws = null;
        }
        isConnected = false;
        updateStatus('disconnected', 'Disconnected');
        btnConnect.textContent = 'Connect (wss://)';
        btnConnect.classList.remove('connected');
        btnConnect.disabled = false;
    }

    function updateStatus(state, text) {
        connectionStatus.className = 'status-indicator ' + state;
        statusText.textContent = text;
    }

    // Touchpad Gestures
    function setupTouchpad() {
        // Prevent browser gestures (pinch-zoom, scrolling)
        touchpad.addEventListener('touchstart', onTouchStart, { passive: false });
        touchpad.addEventListener('touchmove', onTouchMove, { passive: false });
        touchpad.addEventListener('touchend', onTouchEnd, { passive: false });
        touchpad.addEventListener('touchcancel', onTouchEnd, { passive: false });

        // Fallback Mouse events for desktop browser testing
        let mouseIsDown = false;
        touchpad.addEventListener('mousedown', (e) => {
            mouseIsDown = true;
            lastTouchX = e.clientX;
            lastTouchY = e.clientY;
            touchpad.classList.add('active');
        });

        window.addEventListener('mousemove', (e) => {
            if (!mouseIsDown) return;
            const dx = (e.clientX - lastTouchX) * touchSensitivity;
            const dy = (e.clientY - lastTouchY) * touchSensitivity;
            lastTouchX = e.clientX;
            lastTouchY = e.clientY;
            sendProtocolV2('move', { dx: dx, dy: dy, dt: 0.016 });
        });

        window.addEventListener('mouseup', () => {
            if (mouseIsDown) {
                mouseIsDown = false;
                touchpad.classList.remove('active');
            }
        });
    }

    function onTouchStart(e) {
        e.preventDefault();
        touchStartTime = Date.now();
        initialTouchCount = e.touches.length;
        maxTouchCount = e.touches.length;
        totalMoveDistance = 0;
        isDragging = false;
        touchpad.classList.add('active');

        if (e.touches.length === 1) {
            lastTouchX = e.touches[0].clientX;
            lastTouchY = e.touches[0].clientY;
            showTouchIndicator(e.touches[0].clientX, e.touches[0].clientY);
        } else if (e.touches.length === 2) {
            lastScrollY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
            removeTouchIndicator();
        }
    }

    function onTouchMove(e) {
        e.preventDefault();
        if (e.touches.length > maxTouchCount) {
            maxTouchCount = e.touches.length;
        }

        if (e.touches.length === 1) {
            // 1-Finger Drag -> Mouse Move
            const curX = e.touches[0].clientX;
            const curY = e.touches[0].clientY;
            const dx = (curX - lastTouchX) * touchSensitivity;
            const dy = (curY - lastTouchY) * touchSensitivity;

            const dist = Math.hypot(curX - lastTouchX, curY - lastTouchY);
            totalMoveDistance += dist;

            lastTouchX = curX;
            lastTouchY = curY;

            updateTouchIndicator(curX, curY);

            // Bounds check for Protocol V2: |dx| <= 500, |dy| <= 500
            const clampedDx = Math.max(-500, Math.min(500, dx));
            const clampedDy = Math.max(-500, Math.min(500, dy));

            if (Math.abs(clampedDx) > 0.01 || Math.abs(clampedDy) > 0.01) {
                isDragging = true;
                sendProtocolV2('move', {
                    dx: parseFloat(clampedDx.toFixed(2)),
                    dy: parseFloat(clampedDy.toFixed(2)),
                    dt: 0.016
                });
            }
        } else if (e.touches.length === 2) {
            // 2-Finger Drag -> Scroll
            const curScrollY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
            const deltaY = (curScrollY - lastScrollY) * 0.25 * touchSensitivity;
            const dist = Math.abs(curScrollY - lastScrollY);
            totalMoveDistance += dist;
            lastScrollY = curScrollY;

            if (dist > 1.0) {
                isDragging = true;
                // Positive delta scrolls up, negative scrolls down
                const clampedDelta = Math.max(-10.0, Math.min(10.0, deltaY));
                sendProtocolV2('scroll', {
                    delta: parseFloat(clampedDelta.toFixed(2))
                });
            }
        }
    }

    function onTouchEnd(e) {
        e.preventDefault();
        touchpad.classList.remove('active');
        removeTouchIndicator();

        const elapsed = Date.now() - touchStartTime;

        // Check for Tap gesture (short duration, minimal move distance)
        if (elapsed <= TAP_THRESHOLD_MS && totalMoveDistance < TAP_DISTANCE_THRESHOLD) {
            if (maxTouchCount === 1) {
                // 1-Finger Tap -> Left Click
                triggerHaptic('light');
                sendProtocolV2('click', { button: 'left' });
            } else if (maxTouchCount === 2) {
                // 2-Finger Tap -> Right Click
                triggerHaptic('medium');
                sendProtocolV2('click', { button: 'right' });
            }
        }
    }

    function showTouchIndicator(x, y) {
        touchFeedback.innerHTML = '';
        const rect = touchpad.getBoundingClientRect();
        const pointer = document.createElement('div');
        pointer.className = 'touch-pointer';
        pointer.id = 'activeTouchPoint';
        pointer.style.left = `${x - rect.left}px`;
        pointer.style.top = `${y - rect.top}px`;
        touchFeedback.appendChild(pointer);
    }

    function updateTouchIndicator(x, y) {
        const pointer = document.getElementById('activeTouchPoint');
        if (pointer) {
            const rect = touchpad.getBoundingClientRect();
            pointer.style.left = `${x - rect.left}px`;
            pointer.style.top = `${y - rect.top}px`;
        }
    }

    function removeTouchIndicator() {
        touchFeedback.innerHTML = '';
    }

    // iOS DeviceOrientation API & Gyroscope Motion
    async function requestDeviceOrientationPermission() {
        if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
            try {
                const response = await DeviceOrientationEvent.requestPermission();
                if (response === 'granted') {
                    startGyroscope();
                    gyroToggle.checked = true;
                    btnGyroPermission.textContent = 'Gyro Allowed ✓';
                    btnGyroPermission.disabled = true;
                } else {
                    alert('Motion sensor permission was denied.');
                }
            } catch (err) {
                console.error('Permission error:', err);
                alert('DeviceOrientation error: ' + err.message);
            }
        } else {
            // Non-iOS or older iOS versions
            startGyroscope();
            gyroToggle.checked = true;
            btnGyroPermission.textContent = 'Gyro Active ✓';
            btnGyroPermission.disabled = true;
        }
    }

    function startGyroscope() {
        if (gyroActive) return;
        window.addEventListener('deviceorientation', handleOrientation, true);
        gyroActive = true;
        statGyro.textContent = 'Active';
        statGyro.style.color = 'var(--success)';
    }

    function stopGyroscope() {
        if (!gyroActive) return;
        window.removeEventListener('deviceorientation', handleOrientation, true);
        gyroActive = false;
        lastAlpha = null;
        lastBeta = null;
        lastGamma = null;
        statGyro.textContent = 'Disabled';
        statGyro.style.color = 'var(--text-secondary)';
    }

    function handleOrientation(event) {
        if (!gyroActive || !isConnected) return;

        const now = performance.now();
        const dt = lastGyroTime ? (now - lastGyroTime) / 1000 : 0.016;
        lastGyroTime = now;

        const gamma = event.gamma; // Left to right tilt [-90, 90]
        const beta = event.beta;   // Front to back tilt [-180, 180]

        if (gamma === null || beta === null) return;

        if (lastGamma === null || lastBeta === null) {
            lastGamma = gamma;
            lastBeta = beta;
            return;
        }

        let deltaGamma = gamma - lastGamma;
        let deltaBeta = beta - lastBeta;

        // Threshold small noise / jitter
        if (Math.abs(deltaGamma) < 0.15) deltaGamma = 0;
        if (Math.abs(deltaBeta) < 0.15) deltaBeta = 0;

        lastGamma = gamma;
        lastBeta = beta;

        if (deltaGamma === 0 && deltaBeta === 0) return;

        // Map tilt deltas to cursor delta (gamma -> dx, beta -> dy)
        const dx = deltaGamma * gyroSensitivity * 3.5;
        const dy = deltaBeta * gyroSensitivity * 3.5;

        const clampedDx = Math.max(-500, Math.min(500, dx));
        const clampedDy = Math.max(-500, Math.min(500, dy));

        sendProtocolV2('move', {
            dx: parseFloat(clampedDx.toFixed(2)),
            dy: parseFloat(clampedDy.toFixed(2)),
            dt: parseFloat(dt.toFixed(3))
        });
    }

    // UI Event Listeners
    function setupEventListeners() {
        btnConnect.addEventListener('click', () => {
            if (isConnected) disconnect();
            else connect();
        });

        btnGyroPermission.addEventListener('click', requestDeviceOrientationPermission);

        btnSettingsToggle.addEventListener('click', () => {
            settingsPanel.classList.toggle('collapsed');
        });

        touchSensitivitySlider.addEventListener('input', (e) => {
            touchSensitivity = parseFloat(e.target.value);
            valTouchSens.textContent = touchSensitivity.toFixed(1);
            localStorage.setItem('airmouse_touch_sens', touchSensitivity);
        });

        gyroSensitivitySlider.addEventListener('input', (e) => {
            gyroSensitivity = parseFloat(e.target.value);
            valGyroSens.textContent = gyroSensitivity.toFixed(1);
            localStorage.setItem('airmouse_gyro_sens', gyroSensitivity);
        });

        gyroToggle.addEventListener('change', (e) => {
            if (e.target.checked) {
                if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
                    requestDeviceOrientationPermission();
                } else {
                    startGyroscope();
                }
            } else {
                stopGyroscope();
            }
        });

        hapticToggle.addEventListener('change', (e) => {
            hapticEnabled = e.target.checked;
            localStorage.setItem('airmouse_haptic', hapticEnabled);
        });

        // Fullscreen Toggle
        if (btnFullscreen) {
            btnFullscreen.addEventListener('click', () => {
                if (!document.fullscreenElement) {
                    if (document.documentElement.requestFullscreen) {
                        document.documentElement.requestFullscreen().catch(() => {});
                    } else if (document.documentElement.webkitRequestFullscreen) {
                        document.documentElement.webkitRequestFullscreen();
                    }
                    btnFullscreen.textContent = '✖ Exit Fullscreen';
                } else {
                    if (document.exitFullscreen) {
                        document.exitFullscreen().catch(() => {});
                    } else if (document.webkitExitFullscreen) {
                        document.webkitExitFullscreen();
                    }
                    btnFullscreen.textContent = '⛶ Fullscreen';
                }
            });
        }

        // Virtual Mouse Buttons
        btnLeftClick.addEventListener('click', () => {
            triggerHaptic('light');
            sendProtocolV2('click', { button: 'left' });
        });

        btnMiddleClick.addEventListener('click', () => {
            triggerHaptic('medium');
            sendProtocolV2('click', { button: 'middle' });
        });

        btnRightClick.addEventListener('click', () => {
            triggerHaptic('medium');
            sendProtocolV2('click', { button: 'right' });
        });
    }

    // Initialization
    window.addEventListener('DOMContentLoaded', () => {
        initSettings();
        setupTouchpad();
        setupEventListeners();
    });
})();
