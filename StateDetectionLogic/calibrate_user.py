"""
User Calibration Script
Run this for 1-2 minutes while user is in normal studying posture
to learn their baseline behavior and adapt thresholds
"""

import cv2
import mediapipe as mp
import numpy as np
import time
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent))

from driver_state_detection.eye_detector import EyeDetector
from driver_state_detection.pose_estimation import HeadPoseEstimator
from driver_state_detection.adaptive_scorer import AdaptiveAttentionScorer
from driver_state_detection.utils import get_landmarks


def calibrate_user(user_id: str = "default", duration_seconds: int = 120):
    """
    Calibrate detection system for a specific user.
    
    Parameters
    ----------
    user_id : str
        User identifier
    duration_seconds : int
        Calibration duration (default 120 seconds = 2 minutes)
    """
    print("=" * 60)
    print("STUDY FOCUS TRACKER - USER CALIBRATION")
    print("=" * 60)
    print(f"\nUser ID: {user_id}")
    print(f"Duration: {duration_seconds} seconds ({duration_seconds // 60} minutes)")
    print("\nINSTRUCTIONS:")
    print("1. Sit in your normal studying position")
    print("2. Look at your study materials (book, screen, notes)")
    print("3. Maintain natural posture - don't try to be perfect")
    print("4. Blink normally, move naturally")
    print("5. The system will learn YOUR normal studying behavior")
    print("\nPress ENTER to start calibration...")
    input()
    
    # Initialize components
    detector = mp.solutions.face_mesh.FaceMesh(
        static_image_mode=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
        refine_landmarks=True,
    )
    
    eye_det = EyeDetector(show_processing=False)
    head_pose = HeadPoseEstimator(show_axis=True)
    adaptive_scorer = AdaptiveAttentionScorer(user_id=user_id)
    
    # Open camera
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("ERROR: Cannot open camera")
        return False
    
    start_time = time.time()
    sample_count = 0
    
    print("\nCalibration in progress...")
    print("Look at the camera window to see your pose.")
    
    while True:
        elapsed = time.time() - start_time
        
        if elapsed >= duration_seconds:
            break
        
        ret, frame = cap.read()
        if not ret:
            print("ERROR: Cannot read frame")
            break
        
        # Flip frame
        frame = cv2.flip(frame, 2)
        
        # Convert to grayscale
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        frame_size = frame.shape[1], frame.shape[0]
        gray = np.expand_dims(gray, axis=2)
        gray = np.concatenate([gray, gray, gray], axis=2)
        
        # Detect face
        lms = detector.process(gray).multi_face_landmarks
        
        if lms:
            landmarks = get_landmarks(lms)
            
            # Get metrics
            ear = eye_det.get_EAR(landmarks=landmarks)
            gaze = eye_det.get_Gaze_Score(
                frame=gray, landmarks=landmarks, frame_size=frame_size
            )
            frame_det, roll, pitch, yaw = head_pose.get_pose(
                frame=frame, landmarks=landmarks, frame_size=frame_size
            )
            
            if frame_det is not None:
                frame = frame_det
            
            # Add calibration sample
            if ear is not None and gaze is not None:
                adaptive_scorer.add_calibration_sample(
                    ear=ear,
                    gaze=gaze,
                    pitch=pitch[0] if pitch is not None else None,
                    yaw=yaw[0] if yaw is not None else None,
                    roll=roll[0] if roll is not None else None
                )
                sample_count += 1
            
            # Display current metrics
            if ear is not None:
                cv2.putText(frame, f"EAR: {ear:.3f}", (10, 50),
                           cv2.FONT_HERSHEY_PLAIN, 2, (255, 255, 255), 2)
            if gaze is not None:
                cv2.putText(frame, f"Gaze: {gaze:.3f}", (10, 90),
                           cv2.FONT_HERSHEY_PLAIN, 2, (255, 255, 255), 2)
            if pitch is not None:
                cv2.putText(frame, f"Pitch: {pitch.round(1)[0]}", (10, 130),
                           cv2.FONT_HERSHEY_PLAIN, 2, (255, 255, 255), 2)
        
        # Display progress
        remaining = duration_seconds - int(elapsed)
        progress = int((elapsed / duration_seconds) * 100)
        cv2.putText(frame, f"Calibrating... {progress}%", (10, frame_size[1] - 60),
                   cv2.FONT_HERSHEY_PLAIN, 2, (0, 255, 0), 2)
        cv2.putText(frame, f"Time remaining: {remaining}s", (10, frame_size[1] - 20),
                   cv2.FONT_HERSHEY_PLAIN, 2, (0, 255, 0), 2)
        cv2.putText(frame, f"Samples: {sample_count}", (10, frame_size[1] - 100),
                   cv2.FONT_HERSHEY_PLAIN, 1.5, (255, 255, 0), 2)
        
        # Show frame
        cv2.imshow("Calibration - Stay in normal study position", frame)
        
        # Allow early exit
        if cv2.waitKey(1) & 0xFF == ord('q'):
            print("\nCalibration cancelled by user")
            cap.release()
            cv2.destroyAllWindows()
            return False
    
    # Cleanup
    cap.release()
    cv2.destroyAllWindows()
    
    # Finalize calibration
    print(f"\n\nCalibration complete! Collected {sample_count} samples.")
    print("Computing personalized thresholds...")
    
    success = adaptive_scorer.finalize_calibration(min_samples=50)
    
    if success:
        print("\n" + "=" * 60)
        print("CALIBRATION SUCCESSFUL!")
        print("=" * 60)
        thresholds = adaptive_scorer.get_thresholds()
        print("\nYour personalized thresholds:")
        print(f"  EAR threshold: {thresholds['ear_thresh']:.3f}")
        print(f"  Gaze threshold: {thresholds['gaze_thresh']:.3f}")
        print(f"  Pitch threshold: ±{thresholds['pitch_thresh']:.1f}°")
        print(f"  Yaw threshold: ±{thresholds['yaw_thresh']:.1f}°")
        print(f"  Roll threshold: ±{thresholds['roll_thresh']:.1f}°")
        print(f"\nCalibration saved to: {adaptive_scorer.calibration_file}")
        print("\nThe detection system will now use these personalized thresholds.")
        print("Run the enhanced detection with: python enhanced_main.py")
        return True
    else:
        print("\n" + "=" * 60)
        print("CALIBRATION FAILED")
        print("=" * 60)
        print(f"Insufficient samples collected: {sample_count}")
        print("Please try again and ensure your face is visible for the full duration.")
        return False


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Calibrate study focus detection for a user")
    parser.add_argument("--user-id", type=str, default="default",
                       help="User identifier (default: 'default')")
    parser.add_argument("--duration", type=int, default=120,
                       help="Calibration duration in seconds (default: 120)")
    
    args = parser.parse_args()
    
    success = calibrate_user(user_id=args.user_id, duration_seconds=args.duration)
    
    sys.exit(0 if success else 1)
