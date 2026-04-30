package com.manekelsa.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a domestic worker.
 * Added multiple annotations to ensure compatibility with various Firestore field naming conventions.
 */
data class Worker(
    @DocumentId
    val id: String = "",
    
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("skill")
    @set:PropertyName("skill")
    var skill: String = "",
    
    @get:PropertyName("phone")
    @set:PropertyName("phone")
    var phone: String = "",
    
    @get:PropertyName("dailyRate")
    @set:PropertyName("dailyRate")
    var dailyRate: Double = 0.0,
    
    @get:PropertyName("photoUrl")
    @set:PropertyName("photoUrl")
    var photoUrl: String = "",
    
    // We map both "isAvailable" and "available" to this property for safety
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = false,
    
    @get:PropertyName("latitude")
    @set:PropertyName("latitude")
    var latitude: Double = 0.0,
    
    @get:PropertyName("longitude")
    @set:PropertyName("longitude")
    var longitude: Double = 0.0,
    
    @get:PropertyName("thumbsUp")
    @set:PropertyName("thumbsUp")
    var thumbsUp: Long = 0L,

    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null
)
