try:
    import plyer
except Exception:
    plyer = None


def send_notification(title: str, message: str):
    if plyer is None:
        # fallback: no-op
        return
    try:
        plyer.notification.notify(
            title=title,
            message=message,
            app_name="Air Mouse Server",
            timeout=3
        )
    except Exception:
        pass