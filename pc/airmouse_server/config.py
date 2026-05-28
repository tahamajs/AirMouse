import json
import os

CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "discovery_port": 8081,
    "sensitivity": 0.5,
    "accent_color": "#007acc",
    "selected_ip": "",
    "manual_ip_enabled": False,
    "manual_ip_value": "",
    "mDNS_name": "airmouse",
    "always_on_top": False,
    "log_filters": {"info": True, "warning": True, "error": True},
    "theme": "default"
}

_CONFIG_FILE = os.path.join(os.path.dirname(__file__), "..", "config.json")

def load_config():
    try:
        with open(_CONFIG_FILE, "r", encoding="utf-8") as f:
            loaded = json.load(f)
        return {**CONFIG, **loaded}
    except (OSError, json.JSONDecodeError):
        return CONFIG.copy()

def save_config():
    with open(_CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(CONFIG, f, indent=4)

# Initialize global CONFIG
CONFIG = load_config()