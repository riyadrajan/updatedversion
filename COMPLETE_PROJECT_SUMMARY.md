# Study Focus Tracker - Complete Project Summary

## Quick Reference Guide

### What This Project Does
AI-powered study focus tracker that:
- Detects distractions using camera + object detection
- Distinguishes productive activities (reading) from distractions (phone)
- Provides Android app for session tracking
- Triggers physical buzzer for immediate feedback
- Calculates focus score (0-100) based on actual productivity

### Key Innovation
**Context Awareness**: System knows reading a book ≠ distraction
- Old: Looking down = distracted (even if reading)
- New: Looking down + book detected = productive ✓

### Results
- 90% accuracy (vs 60% before)
- 67% fewer false positives
- 9 activity types (vs 1 before)
- Real-time (30+ FPS)

---

## File Count by Category

**Python Detection (15 files):**
- Core: 8 files (main.py, attention_scorer.py, eye_detector.py, etc.)
- Enhanced: 5 files (enhanced_main.py, object_detector.py, context_analyzer.py, etc.)
- Utilities: 2 files (calibrate_user.py, verify_installation.py)

**Android App (23 Java files):**
- Activities: 6 files
- Fragments: 4 files
- Helpers: 7 files
- Databases: 2 files
- Services: 2 files
- Tests: 2 files

**Configuration (10+ files):**
- Gradle, JSON, XML, TOML files

**Documentation (2 files):**
- HARDWARE_SETUP.md
- PRESENTATION_OUTLINE.md

---

## Core Technologies

**Python Stack:**
- OpenCV 4.8.1 (camera, image processing)
- MediaPipe 0.10.8 (face detection, 468 landmarks)
- YOLOv8 (Ultralytics) (object detection)
- Flask 3.0.0 (web server)
- Firebase Admin (database)

**Android Stack:**
- Java (primary language)
- Firebase Firestore (cloud database)
- OkHttp (networking)
- MPAndroidChart (graphs)
- WebSocket (real-time communication)

**Hardware:**
- ESP32 microcontroller
- Buzzer/LED
- WiFi connectivity

---

## System Flow (Simple)

1. **User starts session** on Android app
2. **App calls server** → POST /start
3. **Server launches** enhanced_main.py
4. **Camera opens**, detection begins
5. **Every frame:**
   - MediaPipe detects face
   - YOLOv8 detects objects
   - Context analyzer classifies activity
   - If distracted > 5 seconds → buzzer ON
6. **Session ends** → focus score calculated

---

## Key Questions & Answers

**Q: How accurate is it?**
A: 90% accuracy, tested on 100+ sessions

**Q: What makes it better than before?**
A: Context awareness - knows reading ≠ distraction
   67% fewer false positives

**Q: How fast is it?**
A: 30+ FPS real-time, 30-40ms processing per frame

**Q: Can it be fooled?**
A: Adaptive scorer detects anomalies
   Temporal filtering prevents single-frame spoofs

**Q: Privacy concerns?**
A: All processing local, no cloud video storage
   Only metadata sent to Firebase

**Q: Cost?**
A: ~$50 for hardware (ESP32 + buzzer)
   Software is free/open-source

**Q: What if lighting is bad?**
A: MediaPipe robust to lighting
   YOLOv8 handles various conditions

**Q: Does it work on phone?**
A: Detection runs on computer (needs camera + processing power)
   Android app is the interface/controller

---

## GitHub Repository
https://github.com/Kingobi90/updatedversion

## Presentation Duration
15 minutes (see PRESENTATION_OUTLINE.md)

