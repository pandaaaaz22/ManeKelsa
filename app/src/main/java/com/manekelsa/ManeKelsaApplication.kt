package com.manekelsa

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * ManeKelsaApplication — Custom Application class.
 *
 * Registered in AndroidManifest.xml via android:name=".ManeKelsaApplication".
 * Runs before any Activity is created — ideal for one-time SDK initialization.
 *
 * Responsibilities:
 *  1. Initialize Firebase SDK
 *  2. Configure Firestore offline persistence (workers viewable without internet)
 *  3. Set up any global app configuration
 */
class ManeKelsaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ManeKelsaApp", "Application onCreate started")
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        android.util.Log.d("ManeKelsaApp", "Firebase initialized")
        configureFirestore()
    }

    private fun configureFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            // Enable disk persistence: data is cached locally on the device.
            // When the user is offline, the app still shows the last known worker list.
            .setPersistenceEnabled(true)
            // Cache size: 50 MB (default is 40 MB). Sufficient for a worker directory.
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
    }
}
