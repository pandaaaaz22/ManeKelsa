# Mane-Kelsa (ಮನೆ-ಕೆಲಸ)

**Mane-Kelsa** (Kannada for "Home Work") is a local-first domestic worker directory designed to bridge the gap between households and domestic workers (cleaners, cooks, gardeners, etc.) in Indian neighborhoods.

The app focuses on simplicity, trust, and real-time connectivity, allowing users to find workers near them instantly.

## 🚀 Features

- **Hyper-Local Search**: Automatically sorts workers by GPS distance, showing you who is closest to your home.
- **Real-Time Availability**: Workers can toggle an "Available Today" switch, which updates instantly for all users.
- **Direct Contact**: One-tap calling and WhatsApp integration for quick communication.
- **Trust & Ratings**: A simple thumbs-up rating system to help build neighborhood trust.
- **Multilingual Support**: Full support for both **English** and **Kannada** (ಕನ್ನಡ).
- **Offline First**: Worker profiles are cached locally, so the directory remains accessible even with a poor internet connection.

## 🛠 Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Database**: Firebase Cloud Firestore (Real-time sync).
- **Storage**: Firebase Storage (Profile photo uploads).
- **Location**: Google Play Services (Fused Location Provider).
- **UI Components**: Material Design 3, ViewBinding, Glide (Image loading), Lottie (Animations).
- **Asynchronous**: Kotlin Coroutines & Flow.

## 📦 Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- A Firebase project with Firestore and Storage enabled.
- `google-services.json` placed in the `app/` directory.

### Installation
1. Clone this repository.
2. Open in Android Studio.
3. Add your `google-services.json`.
4. Update Firestore rules to allow read/write access.
5. Build and run on a physical device or emulator.

## 📄 License
This project is for educational and community-service purposes.
