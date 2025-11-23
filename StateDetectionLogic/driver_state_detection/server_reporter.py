"""
Optional server reporter - sends detection events to existing server
Safe to use - fails silently if server unavailable
"""

import requests
import time
from typing import Dict, Optional

class ServerReporter:
    """Reports detection events to server without blocking"""
    
    def __init__(self, server_url: str = "http://10.0.2.2:3000", enabled: bool = True):
        self.server_url = server_url
        self.enabled = enabled
        self.last_report_time = 0
        self.report_interval = 2.0  # Report every 2 seconds
        
    def report_detection(
        self,
        activity: str,
        is_distracted: bool,
        severity: float,
        detected_objects: Dict[str, bool],
        ear: Optional[float] = None,
        gaze: Optional[float] = None,
        head_pose: Optional[Dict] = None
    ):
        """
        Send detection event to server (non-blocking, fails silently)
        """
        if not self.enabled:
            return
        
        # Throttle reports
        now = time.time()
        if now - self.last_report_time < self.report_interval:
            return
        
        try:
            # Calculate focus score from activity
            focus_score = self._calculate_focus_score(activity, severity)
            
            data = {
                'timestamp': now,
                'activity': activity,
                'distracted': is_distracted,
                'severity': severity,
                'focus_score': focus_score,
                'objects': detected_objects,
                'ear': ear,
                'gaze': gaze,
                'head_pose': head_pose or {}
            }
            
            # Send to server (1 second timeout, don't block)
            requests.post(
                f"{self.server_url}/detection/event",
                json=data,
                timeout=1
            )
            
            self.last_report_time = now
            
        except Exception as e:
            # Fail silently - don't break detection if server down
            pass
    
    def _calculate_focus_score(self, activity: str, severity: float) -> float:
        """Convert activity to focus score (0-100)"""
        
        ACTIVITY_SCORES = {
            'focused_studying': 100,
            'reading_book': 95,
            'taking_notes': 90,
            'typing': 85,
            'drinking_water': 80,
            'thinking': 70,
            'looking_away': 40,
            'phone_distraction': 10,
            'face_missing': 0,
            'unknown': 50
        }
        
        base_score = ACTIVITY_SCORES.get(activity, 50)
        
        # Reduce by severity
        if severity > 0:
            base_score *= (1 - severity * 0.5)
        
        return max(0, min(100, base_score))
