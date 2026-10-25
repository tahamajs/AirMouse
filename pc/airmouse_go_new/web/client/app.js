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

    const CLIENT_VERSION = '1.0.1';

    // State Variables
    let ws = null;
    let seq = 0;
    let messageCounter = 0;
    let rateInterval = null;
    let isConnected = false;

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

    // Web Audio Synthesizer Context & Pooled Buffers (iOS AudioContext unlocked on touch)
    let audioCtx = null;
    let audioBuffers = {
        light: null,
        medium: null,
        heavy: null
    };
    let isAudioUnlocked = false;

    function initAudio() {
        if (!audioCtx) {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                audioCtx = new AudioContextClass();
                generatePooledBuffers(audioCtx);
            }
        }
        if (audioCtx && audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
    }

    // Warm-up and unlock AudioContext immediately on touch to avoid clipping & delay
    function unlockAudioOnTouch() {
        if (!audioCtx) {
            initAudio();
        } else if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }

        if (audioCtx && !isAudioUnlocked) {
            try {
                // Play a silent 1-sample buffer to warm up the iOS WebKit audio hardware pipeline
                const silentBuf = audioCtx.createBuffer(1, 1, 22050);
                const src = audioCtx.createBufferSource();
                src.buffer = silentBuf;
                src.connect(audioCtx.destination);
                src.start(0);
                isAudioUnlocked = true;
            } catch (e) {}
        }
    }

    // Pre-synthesize clean tactile click PCM impulses into reusable AudioBuffers
    function generatePooledBuffers(ctx) {
        const sampleRate = ctx.sampleRate;

        function createImpulse(durationSec, fStart, fEnd, peakGain) {
            const numSamples = Math.max(1, Math.floor(sampleRate * durationSec));
            const buf = ctx.createBuffer(1, numSamples, sampleRate);
            const chan = buf.getChannelData(0);

            let phase = 0;
            for (let i = 0; i < numSamples; i++) {
                const t = i / numSamples;
                const freq = fEnd + (fStart - fEnd) * Math.cos(t * Math.PI * 0.5);
                phase += (2 * Math.PI * freq) / sampleRate;

                // Attack envelope (first 4%), smooth exponential decay, zero fade-out at end
                let envelope;
                if (t < 0.04) {
                    envelope = t / 0.04;
                } else {
                    envelope = Math.exp(-7.5 * (t - 0.04));
                }
                if (t > 0.88) {
                    envelope *= (1 - (t - 0.88) / 0.12);
                }

                chan[i] = Math.sin(phase) * envelope * peakGain;
            }
            return buf;
        }

        // Pooled buffers:
        // Light: 14ms crisp tap for 1-finger click
        audioBuffers.light = createImpulse(0.014, 950, 220, 0.4);
        // Medium: 24ms firm click for 2-finger tap & middle click
        audioBuffers.medium = createImpulse(0.024, 750, 160, 0.5);
        // Heavy: 38ms deeper click for long-press / drag actions
        audioBuffers.heavy = createImpulse(0.038, 520, 110, 0.6);
    }

    function playTactileFeedback(type = 'light') {
        if (!feedbackEnabled) return;

        // Try standard navigator.vibrate if available (Android Chrome)
        if (navigator.vibrate) {
            try {
                if (type === 'light') navigator.vibrate(10);
                else if (type === 'medium') navigator.vibrate(25);
                else if (type === 'heavy') navigator.vibrate(45);
            } catch (e) {}
        }

        // Pooled Web Audio tactile synthesis for iOS Safari (WebKit blocks navigator.vibrate)
        try {
            if (!audioCtx) initAudio();
            if (audioCtx) {
                if (audioCtx.state === 'suspended') {
                    audioCtx.resume();
                }
                const buffer = audioBuffers[type] || audioBuffers.light;
                if (buffer) {
                    const source = audioCtx.createBufferSource();
                    source.buffer = buffer;
                    source.connect(audioCtx.destination);
                    source.start(0);
                }
            }
        } catch (e) {
            console.warn('Tactile feedback synthesis error:', e);
        }
    }

    // Screen Wake Lock Implementation with Video Fallback for older iOS Safari (< 16.4)
    let wakeLock = null;
    let wakeLockVideo = null;
    let wakeLockMode = 'none'; // 'native' | 'video' | 'none'
    let wakeLockRequested = true;

    // Ultralight silent looping MP4 data URI for WebKit fallback
    const FALLBACK_VIDEO_MP4 = "data:video/mp4;base64,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";

    function ensureFallbackVideo() {
        if (!wakeLockVideo) {
            wakeLockVideo = document.createElement('video');
            wakeLockVideo.setAttribute('playsinline', '');
            wakeLockVideo.setAttribute('webkit-playsinline', '');
            wakeLockVideo.setAttribute('muted', '');
            wakeLockVideo.setAttribute('loop', '');
            wakeLockVideo.muted = true;
            wakeLockVideo.loop = true;
            wakeLockVideo.playsInline = true;
            wakeLockVideo.style.position = 'fixed';
            wakeLockVideo.style.top = '-9999px';
            wakeLockVideo.style.left = '-9999px';
            wakeLockVideo.style.width = '1px';
            wakeLockVideo.style.height = '1px';
            wakeLockVideo.style.opacity = '0.001';
            wakeLockVideo.style.pointerEvents = 'none';
            wakeLockVideo.src = FALLBACK_VIDEO_MP4;
            document.body.appendChild(wakeLockVideo);
        }
        return wakeLockVideo;
    }

    async function requestWakeLock() {
        if (!wakeLockRequested) return;

        // 1. Try native Screen Wake Lock API (iOS 16.4+, Chrome, modern mobile browsers)
        if ('wakeLock' in navigator && navigator.wakeLock && typeof navigator.wakeLock.request === 'function') {
            try {
                wakeLock = await navigator.wakeLock.request('screen');
                wakeLockMode = 'native';
                updateWakeLockStatus(true, 'Active');
                wakeLock.addEventListener('release', () => {
                    if (wakeLockMode === 'native') {
                        updateWakeLockStatus(false);
                    }
                });
                return;
            } catch (err) {
                console.warn('Native WakeLock error, trying video fallback:', err.name, err.message);
            }
        }

        // 2. Fallback for older iOS Safari engines (< 16.4) via silent looping video
        try {
            const vid = ensureFallbackVideo();
            const playPromise = vid.play();
            if (playPromise !== undefined) {
                await playPromise;
            }
            wakeLockMode = 'video';
            updateWakeLockStatus(true, 'Active (Video)');
        } catch (vidErr) {
            console.warn('Video wake lock fallback failed:', vidErr);
            wakeLockMode = 'none';
            updateWakeLockStatus(false, 'Unavailable');
        }
    }

    async function releaseWakeLock() {
        if (wakeLock) {
            try {
                await wakeLock.release();
            } catch (e) {}
            wakeLock = null;
        }
        if (wakeLockVideo) {
            try {
                wakeLockVideo.pause();
            } catch (e) {}
        }
        wakeLockMode = 'none';
        updateWakeLockStatus(false);
    }

    function updateWakeLockStatus(active, customText) {
        if (!statWakeLock) return;
        if (customText) {
            statWakeLock.textContent = customText;
            statWakeLock.style.color = active ? 'var(--success)' : 'var(--text-secondary)';
        } else if (active) {
            statWakeLock.textContent = wakeLockMode === 'video' ? 'Active (Video)' : 'Active';
            statWakeLock.style.color = 'var(--success)';
        } else {
            statWakeLock.textContent = 'Idle';
            statWakeLock.style.color = 'var(--text-secondary)';
        }
    }

    // Re-acquire Wake Lock when tab/page regains visibility; pause video fallback when hidden
    document.addEventListener('visibilitychange', async () => {
        if (wakeLockRequested && isConnected && document.visibilityState === 'visible') {
            await requestWakeLock();
        } else if (document.visibilityState === 'hidden') {
            if (wakeLockVideo && wakeLockMode === 'video') {
                try {
                    wakeLockVideo.pause();
                } catch (e) {}
            }
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
    const motionPermissionModal = document.getElementById('motionPermissionModal');
    const btnMotionGrant = document.getElementById('btnMotionGrant');
    const btnMotionCancel = document.getElementById('btnMotionCancel');

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

        // Server Host Resolution: default to matching host with WS port 8081
        const defaultHost = (window.location.hostname && window.location.hostname !== '') 
            ? `${window.location.hostname}:8081` 
            : '127.0.0.1:8081';
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

        // Frictionless One-Touch / QR Auto-Connect
        if (urlParams.get('autoconnect') === '1' || urlParams.get('auto') === 'true') {
            setTimeout(() => {
                if (!isConnected && btnConnect) {
                    btnConnect.click();
                }
            }, 300);
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

        // Unlock audio context immediately within user gesture
        unlockAudioOnTouch();

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
        // Unlock / resume AudioContext on touchstart to guarantee zero click delay & no clipping
        unlockAudioOnTouch();

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

    // iOS DeviceOrientation / DeviceMotionEvent API & Gyroscope Battery Saver Handling
    let motionPermissionGranted = false;

    function isMotionPermissionRequired() {
        return (
            (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') ||
            (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function')
        );
    }

    function showMotionPermissionModal() {
        if (motionPermissionModal) {
            motionPermissionModal.classList.remove('hidden');
        }
    }

    function hideMotionPermissionModal() {
        if (motionPermissionModal) {
            motionPermissionModal.classList.add('hidden');
        }
    }

    // Direct user-gesture execution for iOS 13+ sensor permissions (DeviceMotionEvent.requestPermission)
    async function requestMotionPermissions() {
        unlockAudioOnTouch();
        let granted = true;

        // iOS 13+ DeviceMotionEvent permission
        if (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') {
            try {
                const res = await DeviceMotionEvent.requestPermission();
                if (res !== 'granted') {
                    granted = false;
                }
            } catch (err) {
                console.warn('DeviceMotionEvent.requestPermission error:', err);
                granted = false;
            }
        }

        // iOS 13+ DeviceOrientationEvent permission
        if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
            try {
                const res = await DeviceOrientationEvent.requestPermission();
                if (res !== 'granted') {
                    granted = false;
                }
            } catch (err) {
                console.warn('DeviceOrientationEvent.requestPermission error:', err);
                granted = false;
            }
        }

        return granted;
    }

    function requestDeviceOrientationPermission() {
        unlockAudioOnTouch();

        if (isMotionPermissionRequired()) {
            if (motionPermissionGranted) {
                startGyroscope();
                gyroToggle.checked = true;
            } else {
                showMotionPermissionModal();
            }
        } else {
            motionPermissionGranted = true;
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

    function getScreenOrientationAngle() {
        // Modern standard ScreenOrientation API
        if (window.screen && window.screen.orientation && typeof window.screen.orientation.angle === 'number') {
            return window.screen.orientation.angle;
        }
        // iOS Safari / older mobile engines fallback
        if (typeof window.orientation === 'number') {
            return window.orientation;
        }
        return 0;
    }

    function resetOrientationTracking() {
        lastBeta = null;
        lastGamma = null;
        lastGyroTime = null;
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

        // Screen orientation coordinate transformation:
        // Converts device-body coordinates to current viewport coordinates (portrait vs landscape)
        const orientationAngle = getScreenOrientationAngle();
        const angleRad = (orientationAngle * Math.PI) / 180;
        const cosA = Math.cos(angleRad);
        const sinA = Math.sin(angleRad);

        // 2D Rotation matrix:
        // angle=0 (portrait): rotDx = deltaGamma, rotDy = deltaBeta
        // angle=90 (landscape left): rotDx = deltaBeta, rotDy = -deltaGamma
        // angle=180 (portrait inverted): rotDx = -deltaGamma, rotDy = -deltaBeta
        // angle=270/-90 (landscape right): rotDx = -deltaBeta, rotDy = deltaGamma
        const rotDx = deltaGamma * cosA + deltaBeta * sinA;
        const rotDy = -deltaGamma * sinA + deltaBeta * cosA;

        // Map tilt deltas to cursor coordinates
        const dx = rotDx * gyroSensitivity * 3.5;
        const dy = rotDy * gyroSensitivity * 3.5;

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
            unlockAudioOnTouch();
            if (isConnected) disconnect();
            else connect();
        });

        btnGyroPermission.addEventListener('click', requestDeviceOrientationPermission);

        btnSettingsToggle.addEventListener('click', () => {
            unlockAudioOnTouch();
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

        // Motion Permission Modal Actions
        if (btnMotionGrant) {
            btnMotionGrant.addEventListener('click', async () => {
                hideMotionPermissionModal();
                const granted = await requestMotionPermissions();
                if (granted) {
                    motionPermissionGranted = true;
                    btnGyroPermission.textContent = 'Gyro Allowed ✓';
                    btnGyroPermission.disabled = true;
                    gyroToggle.checked = true;
                    startGyroscope();
                    playTactileFeedback('medium');
                } else {
                    gyroToggle.checked = false;
                    statGyro.textContent = 'Denied';
                    statGyro.style.color = 'var(--text-secondary)';
                    alert('Motion sensor permission was denied in iOS settings.');
                }
            });
        }

        if (btnMotionCancel) {
            btnMotionCancel.addEventListener('click', () => {
                hideMotionPermissionModal();
                gyroToggle.checked = false;
                stopGyroscope();
            });
        }

        // Gyro Battery Saver Toggle
        gyroToggle.addEventListener('change', (e) => {
            unlockAudioOnTouch();
            if (e.target.checked) {
                if (isMotionPermissionRequired() && !motionPermissionGranted) {
                    e.target.checked = false;
                    showMotionPermissionModal();
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

        // Screen orientation tracking reset
        window.addEventListener('orientationchange', resetOrientationTracking);
        if (window.screen && window.screen.orientation) {
            window.screen.orientation.addEventListener('change', resetOrientationTracking);
        }

        // Universal touch/click audio wake-up (resumes AudioContext on first touch)
        ['touchstart', 'touchend', 'pointerdown', 'mousedown'].forEach(evt => {
            window.addEventListener(evt, unlockAudioOnTouch, { passive: true, capture: true });
        });
    }

    // Initialize on DOM ready
    window.addEventListener('DOMContentLoaded', () => {
        initSettings();
        setupTouchpad();
        setupEventListeners();
    });
})();
