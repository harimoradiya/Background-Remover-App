# Background Remover App

This is a demo Android application developed in Kotlin that allows users to remove the background from images. The application communicates with a FastAPI backend hosted locally during development.

## Features
- Remove background from images seamlessly.
- Minimalist and user-friendly UI.
- Supports communication with a FastAPI backend for processing.

## Screenshots
Add screenshots of your app here to showcase the UI and functionality.

## Prerequisites
- Android Studio installed on your machine.
- FastAPI backend running locally.
- Basic understanding of Kotlin and Android development.

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/harimoradiya/Background-Remover-App.git
cd Background-Remover-App
```

### 2. Set Up FastAPI Backend
1. Clone or set up your FastAPI backend.
2. Start the FastAPI server:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```
3. Verify that the backend is running by visiting: `http://127.0.0.1:8000/docs`

### 3. Configure Android App
1. Open the project in Android Studio.
2. In your app code, configure the base URL to communicate with the backend:
   ```kotlin
   const val BASE_URL = "http://10.0.2.2:8000"
   ```

### 4. Build and Run the App
1. Build the app in Android Studio.
2. Run the app on an emulator or connected device.

## Usage
1. Launch the app on your Android device or emulator.
2. Select an image to remove the background.
3. The app communicates with the FastAPI backend to process the image.

## FastAPI Backend Information
- **Local Emulator Access**: Use `10.0.2.2` as the IP address in the app to connect to the backend.
- **Backend URL**: `http://10.0.2.2:8000`

### CORS Configuration
Ensure CORS is properly configured in your FastAPI backend to allow communication with the Android app:
```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Replace "*" with specific domains in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

## Technologies Used
- **Frontend**: Kotlin, Android
- **Backend**: FastAPI (Python)

##Screenshot
<div style="display: flex; flex-wrap: wrap; gap: 10px;">
     <img src="https://github.com/user-attachments/assets/c387b48a-71c8-42b7-88de-4e0866fe71f5" width="48%">
  <img src="https://github.com/user-attachments/assets/61a5ea18-d809-4fb9-b77d-a5bd10c1bf46" width="48%">
  <img src="https://github.com/user-attachments/assets/45bd0552-4a87-4a98-a52f-3a1718ff3504" width="48%">
  <img src="https://github.com/user-attachments/assets/682d81ce-53d0-4960-b9d5-404001821528" width="48%">

</div>


## License
This project is licensed under the [MIT License](LICENSE).

---

Feel free to contribute, suggest improvements, or report issues by creating a new issue in this repository.

## Author
**Hari Moradiya**

- [GitHub Profile](https://github.com/harimoradiya)
