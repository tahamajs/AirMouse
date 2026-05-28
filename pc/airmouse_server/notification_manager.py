import plyer

def send_notification(title: str, message: str):
    try:
        plyer.notification.notify(
            title=title,
            message=message,
            app_name="Air Mouse Server",
            timeout=3
        )
    except Exception:
        pass