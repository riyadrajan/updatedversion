#!/usr/bin/env python3
"""
Verification script to check all enhancements are properly installed and working
"""

import sys
import os
from pathlib import Path

# Color codes for terminal output
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
RESET = '\033[0m'

def print_status(message, status):
    """Print colored status message"""
    if status == "OK":
        print(f"{GREEN}✅ {message}{RESET}")
    elif status == "FAIL":
        print(f"{RED}❌ {message}{RESET}")
    elif status == "WARN":
        print(f"{YELLOW}⚠️  {message}{RESET}")
    else:
        print(f"{BLUE}ℹ️  {message}{RESET}")

def check_file_exists(filepath, description):
    """Check if a file exists"""
    if Path(filepath).exists():
        print_status(f"{description}: {filepath}", "OK")
        return True
    else:
        print_status(f"{description} MISSING: {filepath}", "FAIL")
        return False

def check_module_import(module_name, description):
    """Check if a Python module can be imported"""
    try:
        __import__(module_name)
        print_status(f"{description} installed", "OK")
        return True
    except ImportError:
        print_status(f"{description} NOT installed", "FAIL")
        return False

def main():
    print(f"\n{BLUE}{'='*60}")
    print("Study Focus Tracker - Installation Verification")
    print(f"{'='*60}{RESET}\n")
    
    base_path = Path(__file__).parent
    detection_path = base_path / "StateDetectionLogic" / "driver_state_detection"
    
    all_ok = True
    
    # Check Python version
    print(f"{BLUE}[1/6] Checking Python Version{RESET}")
    version = sys.version_info
    if version.major == 3 and version.minor >= 8:
        print_status(f"Python {version.major}.{version.minor}.{version.micro}", "OK")
    else:
        print_status(f"Python {version.major}.{version.minor}.{version.micro} (requires 3.8+)", "FAIL")
        all_ok = False
    print()
    
    # Check core dependencies
    print(f"{BLUE}[2/6] Checking Core Dependencies{RESET}")
    deps = {
        'numpy': 'NumPy',
        'cv2': 'OpenCV',
        'mediapipe': 'MediaPipe',
    }
    for module, name in deps.items():
        if not check_module_import(module, name):
            all_ok = False
    print()
    
    # Check optional dependencies
    print(f"{BLUE}[3/6] Checking Optional Dependencies{RESET}")
    optional_deps = {
        'ultralytics': 'YOLOv8 (Object Detection)',
        'flask': 'Flask (Server)',
        'firebase_admin': 'Firebase Admin',
    }
    for module, name in optional_deps.items():
        if not check_module_import(module, name):
            print_status(f"{name} not installed (optional)", "WARN")
    print()
    
    # Check enhanced detection modules
    print(f"{BLUE}[4/6] Checking Enhanced Detection Modules{RESET}")
    modules = {
        'object_detector.py': 'YOLOv8 Object Detector',
        'context_analyzer.py': 'Context Analyzer',
        'adaptive_scorer.py': 'Adaptive Scorer',
        'pattern_recognizer.py': 'Pattern Recognizer',
        'enhanced_main.py': 'Enhanced Main Script',
    }
    for filename, desc in modules.items():
        filepath = detection_path / filename
        if not check_file_exists(filepath, desc):
            all_ok = False
    print()
    
    # Check calibration tool
    print(f"{BLUE}[5/6] Checking Calibration Tool{RESET}")
    calib_path = base_path / "StateDetectionLogic" / "calibrate_user.py"
    if not check_file_exists(calib_path, "Calibration Script"):
        all_ok = False
    print()
    
    # Check Android app files
    print(f"{BLUE}[6/6] Checking Android App Enhancements{RESET}")
    android_base = base_path / "AndroidApp" / "StudyTrackerBasicTest" / "app" / "src" / "main" / "java" / "com" / "example" / "studytrackerbasictest"
    android_files = {
        'AppUsageMonitor.java': 'App Usage Monitor',
        'UsageStatsHelper.java': 'Usage Stats Helper',
    }
    for filename, desc in android_files.items():
        filepath = android_base / filename
        if not check_file_exists(filepath, desc):
            all_ok = False
    print()
    
    # Test module imports
    print(f"{BLUE}Testing Module Imports{RESET}")
    sys.path.insert(0, str(detection_path))
    
    try:
        from object_detector import ObjectDetector
        print_status("ObjectDetector imports successfully", "OK")
    except Exception as e:
        print_status(f"ObjectDetector import failed: {e}", "FAIL")
        all_ok = False
    
    try:
        from context_analyzer import ContextAnalyzer
        print_status("ContextAnalyzer imports successfully", "OK")
    except Exception as e:
        print_status(f"ContextAnalyzer import failed: {e}", "FAIL")
        all_ok = False
    
    try:
        from adaptive_scorer import AdaptiveAttentionScorer
        print_status("AdaptiveAttentionScorer imports successfully", "OK")
    except Exception as e:
        print_status(f"AdaptiveAttentionScorer import failed: {e}", "FAIL")
        all_ok = False
    
    try:
        from pattern_recognizer import PatternRecognizer
        print_status("PatternRecognizer imports successfully", "OK")
    except Exception as e:
        print_status(f"PatternRecognizer import failed: {e}", "FAIL")
        all_ok = False
    
    print()
    
    # Final summary
    print(f"{BLUE}{'='*60}{RESET}")
    if all_ok:
        print(f"{GREEN}✅ ALL CHECKS PASSED!{RESET}")
        print(f"\n{GREEN}All enhancements are properly installed and ready to use.{RESET}")
        print(f"\n{BLUE}Next Steps:{RESET}")
        print("1. Install optional dependencies: pip install ultralytics")
        print("2. Run calibration: cd StateDetectionLogic && python calibrate_user.py")
        print("3. Test detection: cd driver_state_detection && python enhanced_main.py")
    else:
        print(f"{RED}❌ SOME CHECKS FAILED{RESET}")
        print(f"\n{YELLOW}Please install missing dependencies:{RESET}")
        print("pip install -r requirements.txt")
    print(f"{BLUE}{'='*60}{RESET}\n")
    
    return 0 if all_ok else 1

if __name__ == "__main__":
    sys.exit(main())
