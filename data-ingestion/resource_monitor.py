import logging
import threading
import time

import psutil

logger = logging.getLogger(__name__)

class ResourceMonitor(threading.Thread):
    """
    Logs CPU and RAM usage at a fixed interval.
    """
    def __init__(self, interval: float = 1.0):
        super().__init__(daemon=True)
        self.interval = interval
        self._stop = threading.Event()

    def run(self):
        while not self._stop.is_set():
            cpu = psutil.cpu_percent(interval=None)
            mem = psutil.virtual_memory().percent
            logger.info(f"CPU {cpu:.1f}% | RAM {mem:.1f}%")
            time.sleep(self.interval)

    def stop(self):
        self._stop.set()
