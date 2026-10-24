/**
 * AirMouse Mobile Web & iOS Client (Protocol v2)
 * Production Hardened for Apple WebKit (iOS Safari 13 through 18+) & Modern Mobile Browsers
 * 
 * Features:
 * 1. Protocol v2 wire envelope (v: 2, seq, ts, id, nested payload)
 * 2. Screen Wake Lock API (keeps iPhone screen awake during touchpad use with automatic visibility change re-acquisition)
 * 3. Self-Signed Certificate Helper (guides iOS Safari users to trust ECDSA cert before wss:// initiation)
 * 4. Gyroscope Battery Saver (toggle window.removeEventListener to eliminate background sensor power draw)
 * 5. Web Audio API Click Synthesizer for tactile confirmation where navigator.vibrate is disabled by WebKit
 * 6. High-precision multi-touch gesture engine with bounds validation (|dx|, |dy| <= 500)
 */

(function () {
    'use strict';

    // State Variables
    let ws = null;
    let seq = 0;
    let messageCounter = 0;
    let rateInterval = null;
    let isConnected = false;

    // Screen Wake Lock API State
    let wakeLock = null;
    let wakeLockRequested = true;

    // Gyroscope State & Battery Saver
    let gyroActive = false;
    let lastBeta = null;
    let lastGamma = null;
    let lastGyroTime = 0;

    // Touch Tracking State (Map of identifier -> {x, y, startX, startY, startTime})
    const touchMap = new Map();
    let touchStartTime = 0;
    let maxTouchCount = 0;
    let isDragging = false;
    let lastSingleTouchX = 0;
    let lastSingleTouchY = 0;
    let lastTwoTouchCenterY = 0;
    let totalMoveDistance = 0;

    // Config & Settings
    const TAP_THRESHOLD_MS = 280;
    const TAP_DISTANCE_THRESHOLD = 15; // px
    let touchSensitivity = 1.5;
    let gyroSensitivity = 1.8;
    let feedbackEnabled = true;

    // Web Audio Synthesizer Context (iOS AudioContext unlocked on first user gesture)
    let audioCtx = null;

    function getAudioContext() {
        if (!audioCtx) {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                audioCtx = new AudioContextClass();
            }
        }
        if (audioCtx && audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
        return audioCtx;
    }

    function playTactileFeedback(type = 'light') {
        if (!feedbackEnabled) return;

        // Try standard navigator.vibrate if available
        if (navigator.vibrate) {
            try {
                if (type === 'light') navigator.vibrate(10);
                else if (type === 'medium') navigator.vibrate(25);
                else if (type === 'heavy') navigator.vibrate(45);
            } catch (e) {}
        }

        // On iOS Safari, navigator.vibrate is disabled in WebKit.
        // Synthesize an ultra-short subtle click impulse via Web Audio API.
        try {
            const ctx = getAudioContext();
            if (ctx && ctx.state === 'running') {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                const now = ctx.currentTime;

                const freq = type === 'light' ? 800 : (type === 'medium' ? 600 : 400);
                const duration = type === 'light' ? 0.015 : 0.03;

                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, now);
                osc.frequency.exponentialRampToValueAtTime(120, now + duration);

                gain.gain.setValueAtTime(0.08, now);
                gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

                osc.connect(gain);
                gain.connect(ctx.destination);

                osc.start(now);
                osc.stop(now + duration);
            }
        } catch (e) {}
    }

    // Screen Wake Lock Implementation
    async function requestWakeLock() {
        if (!wakeLockRequested) return;
        if ('wakeLock' in navigator) {
            try {
                wakeLock = await navigator.wakeLock.request('screen');
                updateWakeLockStatus(true);
                wakeLock.addEventListener('release', () => {
                    updateWakeLockStatus(false);
                });
            } catch (err) {
                console.warn('Screen WakeLock error:', err.name, err.message);
                updateWakeLockStatus(false);
            }
        } else {
            updateWakeLockStatus(false, 'Unsupported');
        }
    }

    async function releaseWakeLock() {
        if (wakeLock) {
            try {
                await wakeLock.release();
            } catch (e) {}
            wakeLock = null;
        }
        updateWakeLockStatus(false);
    }

    function updateWakeLockStatus(active, customText) {
        if (!statWakeLock) return;
        if (customText) {
            statWakeLock.textContent = customText;
            statWakeLock.style.color = 'var(--text-secondary)';
        } else if (active) {
            statWakeLock.textContent = 'Active';
            statWakeLock.style.color = 'var(--success)';
        } else {
            statWakeLock.textContent = 'Idle';
            statWakeLock.style.color = 'var(--text-secondary)';
        }
    }

    // Re-acquire Wake Lock when tab/page regains visibility
    document.addEventListener('visibilitychange', async () => {
        if (wakeLockRequested && isConnected && document.visibilityState === 'visible') {
            await requestWakeLock();
        }
    });

    // DOM Elements
    const serverHostInput = document.getElementById('serverHost');
    const pairingTokenInput = document.getElementById('pairingToken');
    const btnConnect = document.getElementById('btnConnect');
    const btnGyroPermission = document.getElementById('btnGyroPermission');
    const btnSettingsToggle = document.getElementById('btnSettingsToggle');
    const settingsPanel = document.getElementById('settingsPanel');
    const touchSensitivitySlider = document.getElementById('touchSensitivity');
    const valTouchSens = document.getElementById('valTouchSens');
    const gyroSensitivitySlider = document.getElementById('gyroSensitivity');
    const valGyroSens = document.getElementById('valGyroSens');
    const gyroToggle = document.getElementById('gyroToggle');
    const wakeLockToggle = document.getElementById('wakeLockToggle');
    const hapticToggle = document.getElementById('hapticToggle');
    const connectionStatus = document.getElementById('connectionStatus');
    const statusText = document.getElementById('statusText');
    const certBanner = document.getElementById('certBanner');
    const certLink = document.getElementById('certLink');
    const btnDismissCert = document.getElementById('btnDismissCert');
    const touchpad = document.getElementById('touchpad');
    const touchFeedback = document.getElementById('touchFeedback');
    const statSeq = document.getElementById('statSeq');
    const statRate = document.getElementById('statRate');
    const statGyro = document.getElementById('statGyro');
    const statWakeLock = document.getElementById('statWakeLock');
    const btnLeftClick = document.getElementById('btnLeftClick');
    const btnMiddleClick = document.getElementById('btnMiddleClick');
    const btnRightClick = document.getElementById('btnRightClick');

    // Persistent Device Identifier
    function getDeviceID() {
        let id = localStorage.getItem('airmouse_device_id');
        if (!id) {
            id = 'ios-web-' + Math.random().toString(36).substring(2, 12) + '-' + Date.now().toString(36);
            localStorage.setItem('airmouse_device_id', id);
        }
        return id;
    }

    // UUID Generator for Protocol v2 id field
    function generateUUID() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = (Math.random() * 16) | 0;
            const v = c === 'x' ? r : (r & 0x3) | 0x8;
            return v.toString(16);
        });
    }

    // Update Cert Link dynamically based on host
    function updateCertLink() {
        const host = serverHostInput.value.trim() || '127.0.0.1:8081';
        let clean = host.replace(/^wss?:\/\//, '').replace(/^https?:\/\//, '').replace(/\/.*$/, '');
        certLink.href = `https://${clean}/`;
    }

    // Settings Initialization
    function initSettings() {
        const urlParams = new URLSearchParams(window.location.search);

        // Server Host Resolution
        const defaultHost = window.location.host || '127.0.0.1:8081';
        const savedHost = urlParams.get('host') || urlParams.get('ip') || localStorage.getItem('airmouse_host') || defaultHost;
        serverHostInput.value = savedHost;
        updateCertLink();

        // Token Resolution (?token=... or stored)
        const paramToken = urlParams.get('token');
        if (paramToken) {
            pairingTokenInput.value = paramToken;
            localStorage.setItem('airmouse_token', paramToken);
        } else {
            const savedToken = localStorage.getItem('airmouse_token') || '';
            pairingTokenInput.value = savedToken;
        }

        // Sensitivities
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

        const savedFeedback = localStorage.getItem('airmouse_feedback');
        if (savedFeedback !== null) {
            feedbackEnabled = savedFeedback === 'true';
            hapticToggle.checked = feedbackEnabled;
        }

        const savedWakeLock = localStorage.getItem('airmouse_wakelock');
        if (savedWakeLock !== null) {
            wakeLockRequested = savedWakeLock === 'true';
            wakeLockToggle.checked = wakeLockRequested;
        }

        // Message rate statistics counter
        rateInterval = setInterval(() => {
            statRate.textContent = messageCounter + ' msg/s';
            messageCounter = 0;
        }, 1000);
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
            console.error('Failed to send Protocol V2 envelope:', e);
            return false;
        }
    }

    // WebSocket / WSS Connection
    function connect() {
        if (ws) {
            disconnect();
            return;
        }

        // Unlock audio context on user tap
        getAudioContext();

        const host = serverHostInput.value.trim();
        const token = pairingTokenInput.value.trim();

        if (!host) {
            alert('Please specify the server host:port');
            return;
        }

        localStorage.setItem('airmouse_host', host);
        if (token) localStorage.setItem('airmouse_token', token);
        updateCertLink();

        let protocol = 'wss://';
        if (window.location.protocol === 'http:' && !host.includes('https')) {
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
            console.error('WebSocket initialization error:', err);
            updateStatus('disconnected', 'WS Init Error');
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
            playTactileFeedback('medium');

            // Acquire Screen Wake Lock on connect
            if (wakeLockRequested) {
                requestWakeLock();
            }

            const ua = navigator.userAgent || '';
            const isIOS = /iPad|iPhone|iPod/.test(ua);
            const clientName = isIOS ? 'iOS Safari AirMouse' : 'Mobile Web AirMouse';

            // Send Protocol V2 Hello Handshake
            sendProtocolV2('hello', {
                device_id: getDeviceID(),
                name: clientName,
                device_name: clientName,
                model: isIOS ? 'Apple iPhone/iPad' : 'Mobile Browser',
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
                    console.log('AirMouse Welcome Handshake:', data);
                    updateStatus('connected', 'Ready');
                } else if (data.type === 'error') {
                    const msg = (data.payload && data.payload.message) || data.error || 'Server rejected connection';
                    alert('AirMouse Server Error: ' + msg);
                    disconnect();
                }
            } catch (e) {}
        };

        ws.onerror = function (e) {
            console.error('WebSocket Error:', e);
            updateStatus('disconnected', 'WS Error (Cert?)');
            // Show certificate helper banner if over wss
            if (targetUrl.startsWith('wss://')) {
                certBanner.classList.add('visible');
            }
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

        // Release WakeLock on disconnect to conserve battery
        releaseWakeLock();
    }

    function updateStatus(state, text) {
        connectionStatus.className = 'status-indicator ' + state;
        statusText.textContent = text;
    }

    // Touchpad Gesture Engine (Optimized for iOS WebKit)
    function setupTouchpad() {
        const opts = { passive: false };
        touchpad.addEventListener('touchstart', onTouchStart, opts);
        touchpad.addEventListener('touchmove', onTouchMove, opts);
        touchpad.addEventListener('touchend', onTouchEnd, opts);
        touchpad.addEventListener('touchcancel', onTouchEnd, opts);

        // Desktop mouse fallback
        let mouseIsDown = false;
        touchpad.addEventListener('mousedown', (e) => {
            getAudioContext();
            mouseIsDown = true;
            lastSingleTouchX = e.clientX;
            lastSingleTouchY = e.clientY;
            touchpad.classList.add('active');
        });

        window.addEventListener('mousemove', (e) => {
            if (!mouseIsDown) return;
            const dx = (e.clientX - lastSingleTouchX) * touchSensitivity;
            const dy = (e.clientY - lastSingleTouchY) * touchSensitivity;
            lastSingleTouchX = e.clientX;
            lastSingleTouchY = e.clientY;
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
        getAudioContext();

        const touches = e.touches;
        const now = Date.now();

        if (touches.length === 1 && touchMap.size === 0) {
            touchStartTime = now;
            totalMoveDistance = 0;
            maxTouchCount = 1;
            isDragging = false;
        }

        if (touches.length > maxTouchCount) {
            maxTouchCount = touches.length;
        }

        touchpad.classList.add('active');

        // Update active touches
        touchMap.clear();
        for (let i = 0; i < touches.length; i++) {
            const t = touches[i];
            touchMap.set(t.identifier, {
                x: t.clientX,
                y: t.clientY,
                startX: t.clientX,
                startY: t.clientY
            });
        }

        if (touches.length === 1) {
            lastSingleTouchX = touches[0].clientX;
            lastSingleTouchY = touches[0].clientY;
            updateTouchPointers(touches);
        } else if (touches.length === 2) {
            lastTwoTouchCenterY = (touches[0].clientY + touches[1].clientY) / 2;
            updateTouchPointers(touches);
        }
    }

    function onTouchMove(e) {
        e.preventDefault();
        const touches = e.touches;

        if (touches.length > maxTouchCount) {
            maxTouchCount = touches.length;
        }

        updateTouchPointers(touches);

        if (touches.length === 1) {
            // 1-Finger Drag -> Move Cursor
            const curX = touches[0].clientX;
            const curY = touches[0].clientY;

            const dx = (curX - lastSingleTouchX) * touchSensitivity;
            const dy = (curY - lastSingleTouchY) * touchSensitivity;

            const stepDist = Math.hypot(curX - lastSingleTouchX, curY - lastSingleTouchY);
            totalMoveDistance += stepDist;

            lastSingleTouchX = curX;
            lastSingleTouchY = curY;

            // Enforce Protocol V2 numeric bounds: |dx|, |dy| <= 500
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
        } else if (touches.length === 2) {
            // 2-Finger Drag -> Vertical Scroll
            const curCenterY = (touches[0].clientY + touches[1].clientY) / 2;
            const diffY = (curCenterY - lastTwoTouchCenterY) * 0.25 * touchSensitivity;
            const dist = Math.abs(curCenterY - lastTwoTouchCenterY);
            totalMoveDistance += dist;
            lastTwoTouchCenterY = curCenterY;

            if (dist > 1.0) {
                isDragging = true;
                // Enforce Protocol V2 numeric bounds: |delta| <= 10.0
                const clampedDelta = Math.max(-10.0, Math.min(10.0, diffY));
                sendProtocolV2('scroll', {
                    delta: parseFloat(clampedDelta.toFixed(2))
                });
            }
        }
    }

    function onTouchEnd(e) {
        e.preventDefault();

        if (e.touches.length === 0) {
            touchpad.classList.remove('active');
            clearTouchPointers();

            const elapsed = Date.now() - touchStartTime;

            // Check for Tap Gesture
            if (elapsed <= TAP_THRESHOLD_MS && totalMoveDistance < TAP_DISTANCE_THRESHOLD) {
                if (maxTouchCount === 1) {
                    // 1-Finger Tap -> Left Click
                    playTactileFeedback('light');
                    sendProtocolV2('click', { button: 'left' });
                } else if (maxTouchCount >= 2) {
                    // 2-Finger Tap -> Right Click
                    playTactileFeedback('medium');
                    sendProtocolV2('click', { button: 'right' });
                }
            }

            maxTouchCount = 0;
            touchMap.clear();
        } else {
            updateTouchPointers(e.touches);
            if (e.touches.length === 1) {
                lastSingleTouchX = e.touches[0].clientX;
                lastSingleTouchY = e.touches[0].clientY;
            } else if (e.touches.length === 2) {
                lastTwoTouchCenterY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
            }
        }
    }

    function updateTouchPointers(touches) {
        touchFeedback.innerHTML = '';
        const rect = touchpad.getBoundingClientRect();

        for (let i = 0; i < touches.length; i++) {
            const t = touches[i];
            const p = document.createElement('div');
            p.className = i === 0 ? 'touch-pointer' : 'touch-pointer-2';
            p.style.left = `${t.clientX - rect.left}px`;
            p.style.top = `${t.clientY - rect.top}px`;
            touchFeedback.appendChild(p);
        }
    }

    function clearTouchPointers() {
        touchFeedback.innerHTML = '';
    }

    // iOS DeviceOrientation API & Gyroscope Battery Saver Handling
    async function requestDeviceOrientationPermission() {
        getAudioContext();

        if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
            try {
                const response = await DeviceOrientationEvent.requestPermission();
                if (response === 'granted') {
                    startGyroscope();
                    gyroToggle.checked = true;
                    btnGyroPermission.textContent = 'Gyro Allowed ✓';
                    btnGyroPermission.disabled = true;
                    playTactileFeedback('medium');
                } else {
                    alert('Motion sensor permission was denied in iOS settings.');
                }
            } catch (err) {
                console.error('DeviceOrientation permission error:', err);
                alert('DeviceOrientation error: ' + err.message);
            }
        } else {
            startGyroscope();
            gyroToggle.checked = true;
            btnGyroPermission.textContent = 'Gyro Active ✓';
            btnGyroPermission.disabled = true;
            playTactileFeedback('light');
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
        // Battery Saver: Explicitly remove listener to prevent sensor hardware draw
        window.removeEventListener('deviceorientation', handleOrientation, true);
        gyroActive = false;
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

        const gamma = event.gamma; // [-90, 90]
        const beta = event.beta;   // [-180, 180]

        if (gamma === null || beta === null) return;

        if (lastGamma === null || lastBeta === null) {
            lastGamma = gamma;
            lastBeta = beta;
            return;
        }

        let deltaGamma = gamma - lastGamma;
        let deltaBeta = beta - lastBeta;

        // Deadzone threshold for small trembling
        if (Math.abs(deltaGamma) < 0.15) deltaGamma = 0;
        if (Math.abs(deltaBeta) < 0.15) deltaBeta = 0;

        lastGamma = gamma;
        lastBeta = beta;

        if (deltaGamma === 0 && deltaBeta === 0) return;

        // Map tilt deltas to cursor coordinates
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

    // UI Event Handlers
    function setupEventListeners() {
        btnConnect.addEventListener('click', () => {
            getAudioContext();
            if (isConnected) disconnect();
            else connect();
        });

        btnGyroPermission.addEventListener('click', requestDeviceOrientationPermission);

        btnSettingsToggle.addEventListener('click', () => {
            getAudioContext();
            settingsPanel.classList.toggle('collapsed');
        });

        serverHostInput.addEventListener('input', updateCertLink);

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

        // Gyro Battery Saver Toggle
        gyroToggle.addEventListener('change', (e) => {
            getAudioContext();
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

        // Screen Wake Lock Toggle
        wakeLockToggle.addEventListener('change', (e) => {
            wakeLockRequested = e.target.checked;
            localStorage.setItem('airmouse_wakelock', wakeLockRequested);
            if (wakeLockRequested && isConnected) {
                requestWakeLock();
            } else {
                releaseWakeLock();
            }
        });

        hapticToggle.addEventListener('change', (e) => {
            feedbackEnabled = e.target.checked;
            localStorage.setItem('airmouse_feedback', feedbackEnabled);
        });

        btnDismissCert.addEventListener('click', () => {
            certBanner.classList.remove('visible');
        });

        // Virtual Mouse Buttons
        btnLeftClick.addEventListener('click', () => {
            playTactileFeedback('light');
            sendProtocolV2('click', { button: 'left' });
        });

        btnMiddleClick.addEventListener('click', () => {
            playTactileFeedback('medium');
            sendProtocolV2('click', { button: 'middle' });
        });

        btnRightClick.addEventListener('click', () => {
            playTactileFeedback('medium');
            sendProtocolV2('click', { button: 'right' });
        });
    }

    // Initialize on DOM ready
    window.addEventListener('DOMContentLoaded', () => {
        initSettings();
        setupTouchpad();
        setupEventListeners();
    });
})();
