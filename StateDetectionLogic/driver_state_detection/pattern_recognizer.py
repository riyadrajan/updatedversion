"""
Pattern Recognition Module
Detects temporal patterns in user behavior to distinguish activities
"""

import numpy as np
from typing import List, Optional, Tuple
from collections import deque


class PatternRecognizer:
    """
    Recognizes temporal patterns in eye movement, head pose, and gaze
    to distinguish between reading, thinking, phone usage, etc.
    """
    
    def __init__(self, window_size: int = 60):
        """
        Initialize pattern recognizer.
        
        Parameters
        ----------
        window_size : int
            Number of frames to analyze for patterns (default 60 = ~2 seconds at 30fps)
        """
        self.window_size = window_size
        
        # Sliding windows for metrics
        self.gaze_window = deque(maxlen=window_size)
        self.pitch_window = deque(maxlen=window_size)
        self.ear_window = deque(maxlen=window_size)
        
    def add_sample(
        self,
        gaze: Optional[float],
        pitch: Optional[float],
        ear: Optional[float]
    ):
        """Add new sample to sliding windows."""
        if gaze is not None:
            self.gaze_window.append(gaze)
        if pitch is not None:
            self.pitch_window.append(pitch)
        if ear is not None:
            self.ear_window.append(ear)
    
    def detect_reading_pattern(self) -> bool:
        """
        Detect reading pattern: systematic horizontal eye movement.
        
        Returns
        -------
        bool
            True if reading pattern detected
        """
        if len(self.gaze_window) < 20:
            return False
        
        gaze_array = np.array(list(self.gaze_window))
        
        # Reading shows periodic gaze movement (left-right scanning)
        # Look for oscillation in gaze score
        try:
            # Simple oscillation detection: count zero-crossings of derivative
            gaze_diff = np.diff(gaze_array)
            zero_crossings = np.sum(np.diff(np.sign(gaze_diff)) != 0)
            
            # Reading should have 3-8 crossings per window (scanning lines)
            is_reading = 3 <= zero_crossings <= 8
            
            # Also check that head is relatively stable (not moving much)
            if len(self.pitch_window) >= 20:
                pitch_stability = np.std(list(self.pitch_window)[-20:])
                is_reading = is_reading and pitch_stability < 5.0
            
            return is_reading
        except Exception:
            return False
    
    def detect_thinking_pattern(self) -> bool:
        """
        Detect thinking pattern: brief look away then return.
        
        Returns
        -------
        bool
            True if thinking pattern detected
        """
        if len(self.gaze_window) < 30:
            return False
        
        try:
            recent_gaze = list(self.gaze_window)[-30:]
            
            # Thinking: gaze increases (looking away) then decreases (returning)
            first_half = np.mean(recent_gaze[:15])
            second_half = np.mean(recent_gaze[15:])
            
            # Look away in first half, return in second half
            looked_away = first_half > 0.25
            returned = second_half < 0.2
            
            return looked_away and returned
        except Exception:
            return False
    
    def detect_phone_pattern(self) -> bool:
        """
        Detect phone usage pattern: sustained steep head angle + stable position.
        
        Returns
        -------
        bool
            True if phone usage pattern detected
        """
        if len(self.pitch_window) < 30:
            return False
        
        try:
            recent_pitch = np.array(list(self.pitch_window)[-30:])
            
            # Phone usage: steep angle (< -60°) sustained
            mean_pitch = np.mean(recent_pitch)
            pitch_stability = np.std(recent_pitch)
            
            # Steep angle + very stable (holding phone still)
            is_phone = mean_pitch < -60 and pitch_stability < 3.0
            
            return is_phone
        except Exception:
            return False
    
    def detect_blink_pattern(self) -> Tuple[bool, int]:
        """
        Detect natural blink pattern to verify liveness.
        
        Returns
        -------
        is_natural : bool
            True if blink pattern is natural
        blink_count : int
            Number of blinks detected
        """
        if len(self.ear_window) < 60:
            return True, 0  # Not enough data, assume natural
        
        try:
            ear_array = np.array(list(self.ear_window))
            
            # Detect blinks: EAR drops below threshold then recovers
            EAR_BLINK_THRESHOLD = 0.15
            blinks = 0
            in_blink = False
            
            for ear in ear_array:
                if ear < EAR_BLINK_THRESHOLD and not in_blink:
                    blinks += 1
                    in_blink = True
                elif ear > EAR_BLINK_THRESHOLD:
                    in_blink = False
            
            # Natural blink rate: 15-20 per minute
            # In 60 frames (~2 seconds at 30fps), expect 0-2 blinks
            is_natural = 0 <= blinks <= 3
            
            # Too few blinks over long period = suspicious (static image)
            if len(self.ear_window) == self.window_size and blinks == 0:
                is_natural = False
            
            return is_natural, blinks
        except Exception:
            return True, 0
    
    def detect_micro_movements(self) -> bool:
        """
        Detect natural micro-movements in head pose.
        Absence suggests static image/video.
        
        Returns
        -------
        bool
            True if natural micro-movements detected
        """
        if len(self.pitch_window) < 30:
            return True  # Not enough data
        
        try:
            pitch_array = np.array(list(self.pitch_window))
            
            # Calculate variance in recent window
            variance = np.var(pitch_array)
            
            # Natural micro-movements should have variance > 0.5
            # Too stable = suspicious
            has_movement = variance > 0.5
            
            return has_movement
        except Exception:
            return True
    
    def get_engagement_score(self) -> float:
        """
        Calculate engagement score based on patterns.
        
        Returns
        -------
        float
            Engagement score (0.0 = disengaged, 1.0 = highly engaged)
        """
        score = 0.5  # Baseline
        
        # Boost for reading pattern
        if self.detect_reading_pattern():
            score += 0.3
        
        # Slight boost for thinking (active learning)
        if self.detect_thinking_pattern():
            score += 0.1
        
        # Penalty for phone usage
        if self.detect_phone_pattern():
            score -= 0.5
        
        # Penalty for unnatural patterns
        is_natural_blink, _ = self.detect_blink_pattern()
        if not is_natural_blink:
            score -= 0.3
        
        if not self.detect_micro_movements():
            score -= 0.3
        
        # Clamp to [0, 1]
        return max(0.0, min(1.0, score))
    
    def get_pattern_summary(self) -> dict:
        """Get summary of detected patterns."""
        is_natural_blink, blink_count = self.detect_blink_pattern()
        
        return {
            'reading': self.detect_reading_pattern(),
            'thinking': self.detect_thinking_pattern(),
            'phone': self.detect_phone_pattern(),
            'natural_blinks': is_natural_blink,
            'blink_count': blink_count,
            'micro_movements': self.detect_micro_movements(),
            'engagement_score': self.get_engagement_score(),
        }
