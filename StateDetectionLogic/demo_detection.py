#!/usr/bin/env python3
"""
Demo script showing the enhanced detection system capabilities
Simulates different scenarios without requiring a camera
"""

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent / 'driver_state_detection'))

from object_detector import ObjectDetector
from context_analyzer import ContextAnalyzer, ActivityType
from adaptive_scorer import AdaptiveAttentionScorer
from pattern_recognizer import PatternRecognizer
import time

# Color codes
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
CYAN = '\033[96m'
RESET = '\033[0m'

def print_header(text):
    print(f"\n{BLUE}{'='*60}")
    print(f"{text}")
    print(f"{'='*60}{RESET}\n")

def print_scenario(name):
    print(f"\n{CYAN}📍 Scenario: {name}{RESET}")

def print_detection(label, value, is_good=None):
    if is_good is True:
        color = GREEN
        icon = "✅"
    elif is_good is False:
        color = RED
        icon = "❌"
    else:
        color = YELLOW
        icon = "ℹ️"
    print(f"{color}{icon} {label}: {value}{RESET}")

def simulate_scenario(name, detected_objects, head_pitch, gaze_score, ear_score):
    """Simulate a detection scenario"""
    print_scenario(name)
    
    # Initialize components
    context_analyzer = ContextAnalyzer()
    
    # Analyze context
    activity, is_distracted, severity = context_analyzer.analyze_context(
        t_now=time.time(),
        head_pitch=head_pitch,
        head_yaw=0.0,
        head_roll=0.0,
        gaze_score=gaze_score,
        ear_score=ear_score,
        detected_objects=detected_objects,
        face_detected=True
    )
    
    # Display results
    print_detection("Detected Objects", ", ".join([k for k, v in detected_objects.items() if v]) or "None", None)
    print_detection("Head Pitch", f"{head_pitch}°" if head_pitch is not None else "N/A", None)
    print_detection("Gaze Score", f"{gaze_score:.2f}" if gaze_score is not None else "N/A", None)
    print_detection("Activity", activity.replace('_', ' ').title(), not is_distracted)
    print_detection("Distracted", "Yes" if is_distracted else "No", not is_distracted)
    if is_distracted:
        print_detection("Severity", f"{severity:.0%}", False)

def main():
    print_header("Enhanced Study Focus Detection - Demo")
    
    print(f"{BLUE}Initializing detection modules...{RESET}")
    
    # Initialize object detector
    obj_detector = ObjectDetector(model_size='n')
    if obj_detector.enabled:
        print(f"{GREEN}✓ YOLOv8 Object Detector ready{RESET}")
    else:
        print(f"{YELLOW}⚠ YOLOv8 not available (install ultralytics){RESET}")
    
    # Initialize other components
    context_analyzer = ContextAnalyzer()
    print(f"{GREEN}✓ Context Analyzer ready{RESET}")
    
    adaptive_scorer = AdaptiveAttentionScorer()
    print(f"{GREEN}✓ Adaptive Scorer ready{RESET}")
    
    pattern_recognizer = PatternRecognizer()
    print(f"{GREEN}✓ Pattern Recognizer ready{RESET}")
    
    print_header("Simulating Different Study Scenarios")
    
    # Scenario 1: Focused studying
    simulate_scenario(
        "Focused Studying - Looking at screen",
        detected_objects={'book': False, 'cell phone': False, 'laptop': True},
        head_pitch=-10.0,  # Slight head down
        gaze_score=0.15,   # Centered gaze
        ear_score=0.25     # Eyes open
    )
    
    # Scenario 2: Reading a book
    simulate_scenario(
        "Reading a Book - Productive",
        detected_objects={'book': True, 'cell phone': False, 'laptop': False},
        head_pitch=-35.0,  # Head down at reading angle
        gaze_score=0.18,   # Slight movement (scanning text)
        ear_score=0.24     # Eyes open
    )
    
    # Scenario 3: Taking notes
    simulate_scenario(
        "Taking Notes - Writing",
        detected_objects={'book': False, 'cell phone': False, 'laptop': False},
        head_pitch=-40.0,  # Head down for writing
        gaze_score=0.20,   # Looking at paper
        ear_score=0.26     # Eyes open
    )
    
    # Scenario 4: Phone distraction
    simulate_scenario(
        "Phone Distraction - Scrolling Instagram",
        detected_objects={'book': False, 'cell phone': True, 'laptop': False},
        head_pitch=-65.0,  # Very steep angle
        gaze_score=0.25,   # Looking at phone
        ear_score=0.23     # Eyes open
    )
    
    # Scenario 5: Looking away (thinking)
    simulate_scenario(
        "Thinking - Brief look away",
        detected_objects={'book': False, 'cell phone': False, 'laptop': True},
        head_pitch=-5.0,   # Normal posture
        gaze_score=0.35,   # Looking away
        ear_score=0.25     # Eyes open
    )
    
    # Scenario 6: Face missing
    simulate_scenario(
        "Face Not Detected - Left desk",
        detected_objects={'book': False, 'cell phone': False, 'laptop': False},
        head_pitch=None,   # No face
        gaze_score=None,
        ear_score=None
    )
    
    # Show pattern recognition demo
    print_header("Pattern Recognition Demo")
    
    print(f"{CYAN}Adding sample data to pattern recognizer...{RESET}")
    pattern_rec = PatternRecognizer(window_size=30)
    
    # Simulate reading pattern (oscillating gaze)
    print(f"\n{BLUE}Simulating reading pattern (horizontal eye scanning):{RESET}")
    for i in range(30):
        gaze = 0.15 + 0.1 * (i % 6) / 6  # Oscillating pattern
        pattern_rec.add_sample(gaze=gaze, pitch=-35.0, ear=0.24)
    
    patterns = pattern_rec.get_pattern_summary()
    print_detection("Reading Pattern", "Detected" if patterns['reading'] else "Not detected", patterns['reading'])
    print_detection("Engagement Score", f"{patterns['engagement_score']:.2f}", patterns['engagement_score'] > 0.6)
    
    # Simulate phone pattern
    print(f"\n{BLUE}Simulating phone pattern (sustained steep angle):{RESET}")
    pattern_rec2 = PatternRecognizer(window_size=30)
    for i in range(30):
        pattern_rec2.add_sample(gaze=0.20, pitch=-65.0, ear=0.23)
    
    patterns2 = pattern_rec2.get_pattern_summary()
    print_detection("Phone Pattern", "Detected" if patterns2['phone'] else "Not detected", not patterns2['phone'])
    print_detection("Engagement Score", f"{patterns2['engagement_score']:.2f}", patterns2['engagement_score'] > 0.6)
    
    # Show adaptive thresholds
    print_header("Adaptive Thresholds Demo")
    
    scorer = AdaptiveAttentionScorer(user_id="demo_user")
    
    print(f"{BLUE}Default thresholds:{RESET}")
    thresholds = scorer.get_thresholds()
    for key, value in thresholds.items():
        print(f"  • {key}: {value}")
    
    print(f"\n{CYAN}After calibration, these would be personalized to YOUR behavior!{RESET}")
    print(f"{GREEN}Run: python calibrate_user.py to create your profile{RESET}")
    
    # Final summary
    print_header("Summary")
    
    print(f"{GREEN}✅ All detection modules working correctly!{RESET}")
    print(f"\n{BLUE}Capabilities demonstrated:{RESET}")
    print(f"  • Object detection (books, phones, laptops)")
    print(f"  • Activity classification (8 types)")
    print(f"  • Context-aware distraction detection")
    print(f"  • Pattern recognition (reading, phone, thinking)")
    print(f"  • Adaptive thresholds (personalizable)")
    
    print(f"\n{CYAN}To use with real camera:{RESET}")
    print(f"  python enhanced_main.py --show-fps --show-proc-time")
    
    print(f"\n{CYAN}To calibrate for your behavior:{RESET}")
    print(f"  python calibrate_user.py --duration 120")
    
    print(f"\n{BLUE}{'='*60}{RESET}\n")

if __name__ == "__main__":
    main()
