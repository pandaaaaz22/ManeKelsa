package com.manekelsa.model

/**
 * User roles in the Mane-Kelsa system.
 */
enum class UserRole {
    WORKER,
    CLIENT
}

/**
 * Data class representing a registered user (either a Worker or a Client).
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.CLIENT,
    val createdAt: Long = System.currentTimeMillis()
)
