# Instructions  
## Android App configurations
- Android manifest.xml : include the uses permissions, and networkSecurityConfig
```xml
    <!--In manifest.xml -->
    <!-- Needed for network calls -->
    <uses-permission android:name="android.permission.INTERNET" />
...
    <application
    ...
        android:networkSecurityConfig="@xml/network_security_config">
    ...
     </application>
```
- In networkSecurityConfig.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```
- MainActivity.java
  - Use http://10.0.2.2:3000 as the base URL when using the android emulator 
  - Use local IP address if using the physical android phone  
Quick way to find local IP address on mac/linux:
```bash
ipconfig getifaddr en0
```
## Running Flask in Python (Updated Version with Object Detection)
- From root directory, navigate to the StateDetectionLogic directory
```bash
cd StateDetectionLogic
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
PORT=3000 python -m driver_state_detection.server
```
- The server is now started, and the vision process is ready to begin when the user presses the Start button on the app
- Alternatively, one may test the vision process in their local terminal (without focus score and database mapping) by using 
```bash
curl -X POST http://localhost:3000/start
```

## Running Flask in Python (Older Version)
- Navigate to the StateDetectionLogic Folder  
- In terminal, run the following commands (and dependencies in the exact order):
```bash
python3 -m venv .venv
```
```bash
. .venv/bin/activate
```
```bash
pip install firebase-admin
```
```bash
pip install google-cloud-firestore
```
```bash
pip install Flask opencv-python flask-sock requests mediapipe opencv-python
```

Note: Refer to requirements.txt and pip install dependencies in the venv
```bash
PORT=3000 .venv/bin/python -m driver_state_detection.server
```

## Run server from StateDetectorLogic dir (if .venv is in driver_state_detection directory)
```bash
PORT=3000 driver_state_detection/.venv/bin/python -m driver_state_detection.server
```

## Run logic detection 
```bash
go into driver_state_detection dir
```
```bash
python3 -m venv .venv
```
```bash
source .venv/bin/activate
```
* pip install flask opencv-python mediapipe numpy (If not installed)
```bash
python3 main.py
```


## Clean port  
- Check if the port is being used:
```bash
sudo lsof -iTCP -sTCP:LISTEN -P -n
```  
- Kill process using its pid  
```bash
  kill -9 <pid>
```
# References
This project makes use of Driver State Detection (https://github.com/e-candeloro/Driver-State-Detection).
