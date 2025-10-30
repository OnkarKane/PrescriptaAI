# PrescriptaAI - Smart Prescription Digitization & Management

![PrescriptaAI Logo](logo.png) <!-- Placeholder Logo - Replace with your actual app logo if available -->

A modern Android application designed to revolutionize handwritten prescription management for doctors using advanced AI.

---

## Table of Contents

- [About PrescriptaAI](#about-prescriptaai)
- [Key Features](#key-features)
- [Technologies Used](#technologies-used)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Firebase Project Setup](#firebase-project-setup)
    - [Google Gemini API Key Setup](#google-gemini-api-key-setup)
    - [Running the App](#running-the-app)
- [APK Download](#apk-download)
- [Project Structure Highlights](#project-structure-highlights)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## About PrescriptaAI

PrescriptaAI is an innovative Android application developed to address the inherent challenges of traditional handwritten medical prescriptions. Leveraging cutting-edge Artificial Intelligence from Google Gemini, the app empowers doctors to:

1.  **Digitize:** Convert illegible or complex handwritten prescriptions into structured, electronic data instantly via the device's camera.
2.  **Manage:** Securely store and organize patient records and their complete prescription history in a cloud-based database.
3.  **Access:** Provide quick, read-only access to a patient's historical prescriptions, enhancing continuity of care and reducing medical errors.

This project serves as a robust prototype demonstrating the power of AI and modern mobile development in solving real-world healthcare inefficiencies.

---

## Key Features

✨ **Seamless Doctor Authentication:** Secure Email/Password registration and login powered by Firebase Authentication.  
📸 **In-App Prescription Scanner:** Utilizes CameraX for a smooth, integrated camera experience to capture prescription images.  
🧠 **Intelligent AI Data Extraction:** Employs Google's Gemini 2.5 Flash model with advanced prompt engineering for highly accurate OCR and structured data extraction from handwritten prescriptions (Patient Name, Age, Diagnosis, Medications - Name, Dosage, Frequency, Duration).  
✅ **Interactive Confirmation Screen:** Doctors can review, correct, and add essential details (like patient phone number) to AI-extracted data before saving.  
🗃️ **Centralized Patient Management:** Stores patient profiles (linked to the doctor's account) and a chronological history of all their prescriptions in Google Cloud Firestore.  
📊 **Real-time Home Dashboard:** Displays dynamic counters for total patients and total prescriptions managed by the logged-in doctor.  
📜 **Read-Only Prescription History:** Provides a clear, detailed, and unalterable view of all past prescriptions for any selected patient.  
🎨 **Modern UI/UX:** Designed with Material Design 3 and a custom medical-themed color palette for an intuitive and visually appealing experience in both light and dark modes.  

---

## Technologies Used

*   **Platform:** Android
*   **Language:** Kotlin
*   **IDE:** Android Studio
*   **Backend:** Google Firebase
    *   **Authentication:** Email/Password based user authentication.
    *   **Firestore:** NoSQL cloud database for all application data (doctors, patients, prescriptions).
    *   **Firebase SDK for Android:** Integration libraries.
*   **Artificial Intelligence:** Google Gemini API
    *   **Model:** `gemini-2.5-flash-latest` for multimodal image and text processing.
    *   **SDK:** `com.google.ai.client.generativeai:generativeai` for API interaction.
*   **Camera:** AndroidX CameraX Library for integrated camera functionality.
*   **JSON Parsing:** Gson library for converting AI responses to Kotlin data classes.
*   **Asynchronous Programming:** Kotlin Coroutines for efficient background task management.
*   **UI Framework:** Material Design 3 for modern UI components.

---

## Getting Started

Follow these instructions to set up and run the PrescriptaAI project locally.

### Prerequisites

*   **Android Studio (Latest Version):** [Download here](https://developer.android.com/studio)
*   **Android SDK:** API Level 24+ (Nougat) installed.
*   **Git:** Installed and configured.
*   **Google Account:** Required for Firebase and Gemini API access.

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/PrescriptaAI.git
    cd PrescriptaAI
    ```
2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select `File` > `Open...` and navigate to the cloned `PrescriptaAI` directory.
    *   Let Gradle sync the project (this may take some time to download dependencies).

### Firebase Project Setup

PrescriptaAI relies heavily on Firebase. You need to link your own Firebase project.

1.  **Create a Firebase Project:**
    *   Go to the [Firebase Console](https://console.firebase.google.com/).
    *   Click `Add project` and follow the on-screen instructions.
    *   **Disable Google Analytics** for the project (optional, but simplifies setup).
2.  **Register your Android App:**
    *   In your Firebase project, click the Android icon (`</>`) to add an Android app.
    *   **Android package name:** Copy this from your `app/build.gradle.kts` file (it's `namespace = "com.example.prescriptaai"`).
    *   **App nickname:** `PrescriptaAI` (or anything you like).
    *   **SHA-1 debug signing certificate:** This is optional for initial setup but recommended for some features. You can get it by running `signingReport` in Gradle tasks or find instructions [here](https://firebase.google.com/docs/android/setup#register-app).
    *   Click `Register app`.
3.  **Download `google-services.json`:**
    *   Follow the instructions to download the `google-services.json` file.
    *   Place this file in your Android project's `app/` directory.
4.  **Enable Firebase Services:**
    *   **Authentication:** In Firebase Console, go to `Build` > `Authentication` > `Sign-in method` tab. Enable `Email/Password`.
    *   **Firestore Database:** In Firebase Console, go to `Build` > `Firestore Database`. Click `Create database`.
        *   Choose **"Start in test mode"** for development (note: this rule expires after 30 days and will need to be updated).
        *   Choose your desired Firestore location.
        *   Click `Enable`.
    *   **Firestore Security Rules:** Your initial "test mode" rules will expire. For a demo, you can use these **insecure (for production) but functional** rules:
        ```firestore
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            match /{document=**} {
              allow read, write: if request.auth != null;
            }
          }
        }
        ```
        *   Go to `Firestore Database` > `Rules` tab.
        *   Replace the existing rules with the above and click `Publish`.
5.  **Sync Gradle:** In Android Studio, ensure your project syncs with the new Firebase configuration.

### Google Gemini API Key Setup

Your Gemini API key is kept secure and not committed to the repository.

1.  **Get your Gemini API Key:**
    *   Go to [Google AI Studio](https://aistudio.google.com/).
    *   Click `Get API key` > `Create API key in new project`.
    *   **Copy your API key.**
2.  **Create `local.properties`:**
    *   In the root directory of your PrescriptaAI project (same level as the `app` folder), create a new file named `local.properties`.
3.  **Add your API key:**
    *   Open `local.properties` and add the following line (replace `YOUR_API_KEY_HERE` with your actual key, **no quotes**):
        ```properties
        GEMINI_API_KEY=YOUR_API_KEY_HERE
        ```
4.  **Sync Gradle:** Ensure your project syncs so Android Studio can read the new property.

### Running the App

1.  **Connect a device or launch an emulator:** Ensure it's running Android API 24+ (Nougat).
2.  Click the `Run` button (green triangle) in Android Studio.

The app should now build and launch, starting with the `SignInActivity`.

---

## APK Download

You can download the latest debug APK from the [Releases](https://github.com/YOUR_USERNAME/PrescriptaAI/releases) section of this GitHub repository.

---

## Project Structure Highlights

*   `app/src/main/java/com/example/prescriptaai/activity`: Contains core Android `Activity` classes (Login, Signup, Home, Confirm Prescription, Patient Details).
*   `app/src/main/java/com/example/prescriptaai/fragments`: Contains `Fragment` classes (Home, Scan, Patients, Profile) and `RecyclerView` adapters.
*   `app/src/main/java/com/example/prescriptaai/fragments/models`: Contains Kotlin data classes (`Prescription`, `Medication`, `Patient`, `PrescriptionWithId`) that define the app's data structure.
*   `app/src/main/res/layout`: All XML layout files for activities and fragments.
*   `app/src/main/res/values/colors.xml`: Light mode color palette definitions.
*   `app/src/main/res/values-night/colors.xml`: Dark mode color palette definitions.
*   `app/src/main/res/values/themes.xml`: Application theme definitions.
*   `app/build.gradle.kts`: Module-level Gradle configuration for dependencies and build settings.
*   `local.properties`: Stores sensitive data like the Gemini API key (ignored by Git).
*   `.gitignore`: Specifies files and folders to be excluded from Git version control.

---

## Future Enhancements

*   **Robust Error Handling:** Implement more sophisticated error states and user feedback for API failures or network issues.
*   **Enhanced AI Prompting:** Further refine Gemini prompts for even greater accuracy, especially with diverse handwriting styles.
*   **User Profile Editing:** Allow doctors to update their hospital or name from the profile screen.
*   **Patient Search/Filter:** Add functionality to search for patients by name or phone number.
*   **Detailed Prescription View/Export:** Options to print or share prescription details.
*   **Offline Support:** Implement local caching for patient data to allow limited functionality without an internet connection.
*   **Patient-Side App:** Develop a complementary app for patients to view their prescriptions digitally.

---

## Contributing

Contributions are welcome! If you have suggestions or find bugs, please open an issue or submit a pull request.

---

## Contact

For any questions or inquiries, please contact:

**[Your Full Name]**  
[Your Email Address]

---
