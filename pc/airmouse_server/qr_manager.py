import qrcode
from PIL import Image, ImageTk
from .config import CONFIG

class QRManager:
    def __init__(self, log_callback=None):
        self.log = log_callback or print
        self.last_pil_image = None
        self.tk_image = None

    def generate(self, ip: str, port: int) -> ImageTk.PhotoImage:
        qr_data = f"airmouse://{ip}:{port}"
        qr = qrcode.QRCode(
            version=None,
            error_correction=qrcode.constants.ERROR_CORRECT_H,
            box_size=8,
            border=2,
        )
        qr.add_data(qr_data)
        qr.make(fit=True)
        img = qr.make_image(fill_color="#111111", back_color="#ffffff").convert("RGB")
        img = img.resize((220, 220))
        self.last_pil_image = img
        self.tk_image = ImageTk.PhotoImage(img)
        return self.tk_image