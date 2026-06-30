document.addEventListener('DOMContentLoaded', () => {
    // Navigation Logic
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');
    const pageTitle = document.getElementById('pageTitle');

    const titles = {
        'dashboard': 'Dashboard',
        'devices': 'Connected Devices',
        'settings': 'Server Settings'
    };

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const pageId = item.dataset.page;
            
            // Update Active Nav
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            // Update Active Page
            pages.forEach(p => p.classList.remove('active'));
            document.getElementById(pageId + 'Page').classList.add('active');

            // Update Title
            pageTitle.textContent = titles[pageId];

            if(pageId === 'devices') fetchDevices();
        });
    });

    // Stats Fetching
    const statClicks = document.getElementById('statClicks');
    const statScrolls = document.getElementById('statScrolls');
    const statDevices = document.getElementById('statDevices');
    const statUptime = document.getElementById('statUptime');
    const devicesGrid = document.getElementById('devicesGrid');
    const refreshDevicesBtn = document.getElementById('refreshDevicesBtn');

    function formatDuration(seconds) {
        const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
        const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
        const s = String(Math.floor(seconds % 60)).padStart(2, '0');
        return `${h}:${m}:${s}`;
    }

    async function fetchStats() {
        try {
            const res = await fetch('/api/stats');
            const data = await res.json();
            if(data) {
                statClicks.textContent = data.clicks || 0;
                statScrolls.textContent = data.scrolls || 0;
                statDevices.textContent = data.devices || 0;
                statUptime.textContent = formatDuration(data.uptime || 0);
            }
        } catch(e) {
            console.warn("Failed to fetch stats");
        }
    }

    async function fetchDevices() {
        try {
            const res = await fetch('/api/devices');
            const devices = await res.json();
            if(devices && devices.length > 0) {
                devicesGrid.innerHTML = devices.map(d => `
                    <div class="device-card">
                        <div class="device-icon-large">
                            <span class="material-icons-round">smartphone</span>
                        </div>
                        <div class="device-info-inner">
                            <h4>${d.name || 'Unknown Device'}</h4>
                            <p>${d.id.substring(0, 8)}... • Connected</p>
                        </div>
                    </div>
                `).join('');
            } else {
                devicesGrid.innerHTML = `
                    <div class="loading-state">
                        <span class="material-icons-round">phonelink_erase</span>
                        <span>No devices currently connected</span>
                    </div>
                `;
            }
        } catch (e) {
            devicesGrid.innerHTML = `
                <div class="loading-state">
                    <span class="material-icons-round">error</span>
                    <span>Failed to load devices</span>
                </div>
            `;
        }
    }

    if(refreshDevicesBtn) {
        refreshDevicesBtn.addEventListener('click', () => {
            devicesGrid.innerHTML = `
                <div class="loading-state">
                    <div class="spinner"></div>
                    <span>Refreshing...</span>
                </div>
            `;
            setTimeout(fetchDevices, 500);
        });
    }

    // Dynamic Aesthetic Chart Animation
    const bars = document.querySelectorAll('.bar');
    setInterval(() => {
        bars.forEach(bar => {
            const randomHeight = Math.floor(Math.random() * 60) + 40; // 40 to 100
            bar.style.height = `${randomHeight}%`;
        });
    }, 2000);

    // Initial Fetch & Polling
    fetchStats();
    setInterval(fetchStats, 2000);
});