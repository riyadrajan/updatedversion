"""
Adaptive Attention Scorer with User Calibration
Learns user's baseline behavior and adapts thresholds accordingly
"""

import numpy as np
from typing import Optional, Dict, List
import json
from pathlib import Path


class AdaptiveAttentionScorer:
    """
    Enhanced attention scorer that adapts to individual user behavior.
    Learns baseline metrics during calibration and adjusts thresholds.
    """
    
    def __init__(
        self,
        user_id: Optional[str] = None,
        calibration_file: Optional[str] = None
    ):
        """
        Initialize adaptive scorer.
        
        Parameters
        ----------
        user_id : str, optional
            User identifier for personalized calibration
        calibration_file : str, optional
            Path to save/load calibration data
        """
        self.user_id = user_id or "default"
        self.calibration_file = calibration_file or f"calibration_{self.user_id}.json"
        
        # Default thresholds (will be adapted)
        self.ear_thresh = 0.15
        self.gaze_thresh = 0.2
        self.roll_thresh = 60
        self.pitch_thresh = 20
        self.yaw_thresh = 30
        
        # Calibration data
        self.is_calibrated = False
        self.calibration_data = {
            'ear_baseline': [],
            'gaze_baseline': [],
            'pitch_baseline': [],
            'yaw_baseline': [],
            'roll_baseline': [],
        }
        
        # Metrics history for pattern analysis
        self.ear_history = []
        self.gaze_history = []
        self.pitch_history = []
        
        # Load existing calibration if available
        self._load_calibration()
        
    def add_calibration_sample(
        self,
        ear: Optional[float],
        gaze: Optional[float],
        pitch: Optional[float],
        yaw: Optional[float],
        roll: Optional[float]
    ):
        """
        Add a sample during calibration phase.
        User should be in normal studying posture.
        """
        if ear is not None:
            self.calibration_data['ear_baseline'].append(ear)
        if gaze is not None:
            self.calibration_data['gaze_baseline'].append(gaze)
        if pitch is not None:
            self.calibration_data['pitch_baseline'].append(pitch)
        if yaw is not None:
            self.calibration_data['yaw_baseline'].append(yaw)
        if roll is not None:
            self.calibration_data['roll_baseline'].append(roll)
    
    def finalize_calibration(self, min_samples: int = 50):
        """
        Compute adaptive thresholds from calibration data.
        
        Parameters
        ----------
        min_samples : int
            Minimum samples required for calibration
        """
        if len(self.calibration_data['ear_baseline']) < min_samples:
            print(f"Warning: Insufficient calibration samples ({len(self.calibration_data['ear_baseline'])} < {min_samples})")
            return False
        
        # EAR threshold: 25th percentile (lower = eyes more closed)
        if self.calibration_data['ear_baseline']:
            ear_baseline = np.array(self.calibration_data['ear_baseline'])
            self.ear_thresh = np.percentile(ear_baseline, 25) * 0.9
            print(f"Calibrated EAR threshold: {self.ear_thresh:.3f}")
        
        # Gaze threshold: 75th percentile (higher = more off-center)
        if self.calibration_data['gaze_baseline']:
            gaze_baseline = np.array(self.calibration_data['gaze_baseline'])
            self.gaze_thresh = np.percentile(gaze_baseline, 75) * 1.2
            print(f"Calibrated Gaze threshold: {self.gaze_thresh:.3f}")
        
        # Pitch threshold: mean ± 1.5 std
        if self.calibration_data['pitch_baseline']:
            pitch_baseline = np.array(self.calibration_data['pitch_baseline'])
            pitch_mean = np.mean(pitch_baseline)
            pitch_std = np.std(pitch_baseline)
            self.pitch_thresh = abs(pitch_mean) + 1.5 * pitch_std
            print(f"Calibrated Pitch threshold: ±{self.pitch_thresh:.1f}°")
        
        # Yaw threshold: mean ± 1.5 std
        if self.calibration_data['yaw_baseline']:
            yaw_baseline = np.array(self.calibration_data['yaw_baseline'])
            yaw_mean = np.mean(yaw_baseline)
            yaw_std = np.std(yaw_baseline)
            self.yaw_thresh = abs(yaw_mean) + 1.5 * yaw_std
            print(f"Calibrated Yaw threshold: ±{self.yaw_thresh:.1f}°")
        
        # Roll threshold: mean ± 2 std (more tolerance)
        if self.calibration_data['roll_baseline']:
            roll_baseline = np.array(self.calibration_data['roll_baseline'])
            roll_mean = np.mean(roll_baseline)
            roll_std = np.std(roll_baseline)
            self.roll_thresh = abs(roll_mean) + 2.0 * roll_std
            print(f"Calibrated Roll threshold: ±{self.roll_thresh:.1f}°")
        
        self.is_calibrated = True
        self._save_calibration()
        return True
    
    def detect_anomaly(
        self,
        ear: Optional[float],
        gaze: Optional[float],
        pitch: Optional[float]
    ) -> bool:
        """
        Detect if current metrics are suspiciously consistent (possible spoofing).
        
        Returns
        -------
        bool
            True if metrics show unnatural consistency
        """
        # Add to history
        if ear is not None:
            self.ear_history.append(ear)
        if gaze is not None:
            self.gaze_history.append(gaze)
        if pitch is not None:
            self.pitch_history.append(pitch)
        
        # Keep only recent history
        max_history = 100
        self.ear_history = self.ear_history[-max_history:]
        self.gaze_history = self.gaze_history[-max_history:]
        self.pitch_history = self.pitch_history[-max_history:]
        
        # Need sufficient history
        if len(self.ear_history) < 30:
            return False
        
        # Calculate variance
        ear_var = np.var(self.ear_history[-30:]) if self.ear_history else 1.0
        gaze_var = np.var(self.gaze_history[-30:]) if self.gaze_history else 1.0
        pitch_var = np.var(self.pitch_history[-30:]) if self.pitch_history else 1.0
        
        # Real humans have variance; too low = suspicious
        EAR_MIN_VARIANCE = 0.0001
        GAZE_MIN_VARIANCE = 0.001
        PITCH_MIN_VARIANCE = 0.5
        
        suspicious = (
            ear_var < EAR_MIN_VARIANCE or
            gaze_var < GAZE_MIN_VARIANCE or
            pitch_var < PITCH_MIN_VARIANCE
        )
        
        if suspicious:
            print(f"Warning: Suspiciously low variance detected (ear={ear_var:.6f}, gaze={gaze_var:.6f}, pitch={pitch_var:.2f})")
        
        return suspicious
    
    def get_thresholds(self) -> Dict[str, float]:
        """Get current threshold values."""
        return {
            'ear_thresh': self.ear_thresh,
            'gaze_thresh': self.gaze_thresh,
            'roll_thresh': self.roll_thresh,
            'pitch_thresh': self.pitch_thresh,
            'yaw_thresh': self.yaw_thresh,
        }
    
    def _save_calibration(self):
        """Save calibration data to file."""
        try:
            data = {
                'user_id': self.user_id,
                'is_calibrated': self.is_calibrated,
                'thresholds': self.get_thresholds(),
                'calibration_stats': {
                    'ear_samples': len(self.calibration_data['ear_baseline']),
                    'gaze_samples': len(self.calibration_data['gaze_baseline']),
                    'pitch_samples': len(self.calibration_data['pitch_baseline']),
                }
            }
            
            with open(self.calibration_file, 'w') as f:
                json.dump(data, f, indent=2)
            
            print(f"Calibration saved to {self.calibration_file}")
        except Exception as e:
            print(f"Failed to save calibration: {e}")
    
    def _load_calibration(self):
        """Load calibration data from file."""
        try:
            if Path(self.calibration_file).exists():
                with open(self.calibration_file, 'r') as f:
                    data = json.load(f)
                
                if data.get('user_id') == self.user_id:
                    thresholds = data.get('thresholds', {})
                    self.ear_thresh = thresholds.get('ear_thresh', self.ear_thresh)
                    self.gaze_thresh = thresholds.get('gaze_thresh', self.gaze_thresh)
                    self.roll_thresh = thresholds.get('roll_thresh', self.roll_thresh)
                    self.pitch_thresh = thresholds.get('pitch_thresh', self.pitch_thresh)
                    self.yaw_thresh = thresholds.get('yaw_thresh', self.yaw_thresh)
                    self.is_calibrated = data.get('is_calibrated', False)
                    
                    print(f"Calibration loaded from {self.calibration_file}")
                    return True
        except Exception as e:
            print(f"Failed to load calibration: {e}")
        
        return False
