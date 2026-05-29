# Air Mouse PC — GUI Migration & Enhancement Plan

This document outlines options to modernize and optionally migrate the existing Tkinter GUI to a more feature-rich toolkit, plus an actionable scaffold for trying PyQt6.

Goals
- Provide a modern, polished desktop UI for the Air Mouse server.
- Improve accessibility and theming (light/dark/high-contrast/pure-black).
- Keep the server core (TCP/UDP handling and mouse control) reusable across UI options.
- Offer a clear migration path and quick prototype to evaluate options.

Options (pros/cons)

1) Keep Tkinter, use `ttkbootstrap` and `pystray` (minimal migration)
- Pros: Low effort; keeps current codebase; cross-platform; small binary size
- Cons: Limited modern widgets and animations
- Action: Use `ttkbootstrap` theme, refine widget layout, add icon assets and micro-transitions

2) Migrate to PyQt6 / Qt for Python
- Pros: Rich widgets, native look & feel, CSS-like styling, QML option, large ecosystem
- Cons: Larger binary and dependency size; license considerations for commercial use
- Action: Replace top-level windows, reuse backend server and event loop in a QThread or via asyncio+QEventLoop

3) Electron (Web UI) — wrap backend with local HTTP socket or WebSocket
- Pros: Unlimited UI freedom (HTML/CSS), great for designers; easy cross-platform packaging
- Cons: Very large runtime (Chromium), heavier memory usage
- Action: Create small local server exposing REST/WS for control; electron app connects and renders UI

4) Toga / BeeWare (native Python UI)
- Pros: Pure Python, cross-platform native widgets
- Cons: Less mature and fewer widgets; may require more effort

Recommended next step
- Prototype PyQt6 client to evaluate developer ergonomics and visual polish. If size becomes an issue, prototype with `ttkbootstrap` first.

PyQt6 scaffold
- File: `pyqt_scaffold/main.py` (included in this repo)
- Purpose: Minimal window with theme toggle, IP selector, and a live log area. Uses a separate thread to run the existing asyncio server if you decide to reuse it.

Packaging & build
- For PyQt6, use `pyinstaller --onefile --noconsole` for distributables; include Qt plugins and platform icons.
- For Tkinter, `pyinstaller` produces smaller executables.

Files added in this commit
- `pyqt_scaffold/main.py` — minimal PyQt6 app scaffold
- `PC_MIGRATION_PLAN.md` — plan and recommendations

How to try the PyQt6 scaffold
1. Create a virtualenv and install PyQt6: `python -m venv .venv && .venv/bin/pip install PyQt6`
2. Run the scaffold: `python pyqt_scaffold/main.py`
3. The scaffold does not run the TCP server by default; integrate the `AirMouseTCPServer` by creating a worker thread that starts the asyncio server and emits signals to the UI.

Notes
- Keep the server logic (TCP, UDP discovery, mouse controller) decoupled so GUI replacements only need to wire callbacks and control commands.
- If you want, I can extend the scaffold to include a small worker that runs the existing `AirMouseTCPServer` from `server.py` using `asyncio` + `QEventLoop` (requires `qasync`).
