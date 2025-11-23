"""
Object Detection Module using YOLOv8
Detects study-related objects: books, phones, laptops, etc.
"""

import numpy as np
from typing import Dict, List, Optional, Tuple

try:
    from ultralytics import YOLO
    YOLO_AVAILABLE = True
except ImportError:
    YOLO_AVAILABLE = False
    print("Warning: ultralytics not installed. Object detection disabled.")
    print("Install with: pip install ultralytics")


class ObjectDetector:
    """Detects objects in frame using YOLOv8 to provide context for attention detection."""
    
    # COCO dataset class IDs relevant to studying
    STUDY_OBJECTS = {
        'book': 73,
        'cell phone': 67,
        'laptop': 63,
        'keyboard': 66,
        'mouse': 64,
        'cup': 41,  # Coffee/water
        'bottle': 39,  # Water bottle
    }
    
    # Objects that indicate breaks/distractions
    DISTRACTION_OBJECTS = {
        'cell phone': 67,
    }
    
    # Objects that are acceptable during study
    NEUTRAL_OBJECTS = {
        'cup': 41,
        'bottle': 39,
    }
    
    def __init__(self, model_size: str = 'n', confidence_threshold: float = 0.35):
        """
        Initialize object detector.
        
        Parameters
        ----------
        model_size : str
            YOLO model size: 'n' (nano), 's' (small), 'm' (medium), 'l' (large), 'x' (xlarge)
            Nano is fastest, xlarge is most accurate. Default 'n' for real-time performance.
        confidence_threshold : float
            Minimum confidence score for detections (0.0 to 1.0)
            Lower threshold (0.35) helps detect phones in different orientations
        """
        self.confidence_threshold = confidence_threshold
        self.model = None
        self.enabled = YOLO_AVAILABLE
        
        # Different thresholds for different objects
        self.object_thresholds = {
            'cell phone': 0.30,  # Lower threshold for phones (harder to detect on ear)
            'bottle': 0.35,      # Lower for bottles
            'cup': 0.35,         # Lower for cups
            'book': 0.40,        # Standard for books
            'laptop': 0.45,      # Higher for laptops (easier to detect)
            'keyboard': 0.45,
            'mouse': 0.40,
        }
        
        if YOLO_AVAILABLE:
            try:
                model_name = f'yolov8{model_size}.pt'
                self.model = YOLO(model_name)
                print(f"YOLOv8 model loaded: {model_name}")
            except Exception as e:
                print(f"Failed to load YOLO model: {e}")
                self.enabled = False
        
        # Detection history for temporal filtering
        self.detection_history: Dict[str, List[bool]] = {obj: [] for obj in self.STUDY_OBJECTS.keys()}
        self.history_length = 8  # Reduced to 8 frames for faster response
        
    def detect_objects(self, frame: np.ndarray) -> Dict[str, bool]:
        """
        Detect study-related objects in frame.
        
        Parameters
        ----------
        frame : np.ndarray
            Input frame (BGR format)
            
        Returns
        -------
        Dict[str, bool]
            Dictionary mapping object names to presence (True/False)
        """
        if not self.enabled or self.model is None:
            return {obj: False for obj in self.STUDY_OBJECTS.keys()}
        
        try:
            # Run detection with lower confidence for better sensitivity
            results = self.model(frame, verbose=False, conf=0.25)
            
            # Extract detected classes
            detected_objects = {obj: False for obj in self.STUDY_OBJECTS.keys()}
            
            for result in results:
                if result.boxes is not None:
                    for box in result.boxes:
                        class_id = int(box.cls[0])
                        confidence = float(box.conf[0])
                        
                        # Check if this is a study-related object with custom threshold
                        for obj_name, obj_id in self.STUDY_OBJECTS.items():
                            threshold = self.object_thresholds.get(obj_name, self.confidence_threshold)
                            if class_id == obj_id and confidence >= threshold:
                                detected_objects[obj_name] = True
            
            # Update detection history for temporal filtering
            for obj_name, detected in detected_objects.items():
                self.detection_history[obj_name].append(detected)
                if len(self.detection_history[obj_name]) > self.history_length:
                    self.detection_history[obj_name].pop(0)
            
            # Apply temporal filtering (object must be detected in 2+ of last 8 frames)
            # Reduced threshold for faster response
            filtered_objects = {}
            for obj_name in detected_objects.keys():
                if len(self.detection_history[obj_name]) >= 2:
                    detection_count = sum(self.detection_history[obj_name])
                    # Phone needs only 2 detections, others need 3
                    min_detections = 2 if obj_name == 'cell phone' else 3
                    filtered_objects[obj_name] = detection_count >= min_detections
                else:
                    filtered_objects[obj_name] = detected_objects[obj_name]
            
            return filtered_objects
            
        except Exception as e:
            print(f"Object detection error: {e}")
            return {obj: False for obj in self.STUDY_OBJECTS.keys()}
    
    def get_detection_details(self, frame: np.ndarray) -> List[Dict]:
        """
        Get detailed detection information including bounding boxes.
        
        Parameters
        ----------
        frame : np.ndarray
            Input frame
            
        Returns
        -------
        List[Dict]
            List of detections with class, confidence, and bbox
        """
        if not self.enabled or self.model is None:
            return []
        
        try:
            results = self.model(frame, verbose=False, conf=self.confidence_threshold)
            detections = []
            
            for result in results:
                if result.boxes is not None:
                    for box in result.boxes:
                        class_id = int(box.cls[0])
                        class_name = self.model.names[class_id]
                        confidence = float(box.conf[0])
                        bbox = box.xyxy[0].cpu().numpy()
                        
                        detections.append({
                            'class_id': class_id,
                            'class_name': class_name,
                            'confidence': confidence,
                            'bbox': bbox,
                        })
            
            return detections
            
        except Exception as e:
            print(f"Detection details error: {e}")
            return []
    
    def is_studying_with_materials(self) -> bool:
        """Check if study materials (book, laptop) are present."""
        if not self.detection_history:
            return False
        
        book_present = sum(self.detection_history.get('book', [])) >= 3
        laptop_present = sum(self.detection_history.get('laptop', [])) >= 3
        
        return book_present or laptop_present
    
    def is_phone_present(self) -> bool:
        """Check if phone is consistently detected."""
        if not self.detection_history:
            return False
        
        return sum(self.detection_history.get('cell phone', [])) >= 3
