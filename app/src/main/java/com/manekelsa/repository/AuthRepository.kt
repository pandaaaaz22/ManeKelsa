package com.manekelsa.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.manekelsa.model.User
import com.manekelsa.model.UserRole
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun getCurrentUser() = auth.currentUser

    suspend fun signIn(email: String, pass: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        val uid = result.user?.uid ?: throw Exception("Login failed")
        val userDoc = usersCollection.document(uid).get().await()
        val user = userDoc.toObject(User::class.java) ?: throw Exception("User data not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signUp(email: String, pass: String, name: String, role: UserRole): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        val uid = result.user?.uid ?: throw Exception("Registration failed")
        val user = User(uid, email, name, role)
        usersCollection.document(uid).set(user).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserData(uid: String): User? {
        return usersCollection.document(uid).get().await().toObject(User::class.java)
    }

    fun logout() {
        auth.signOut()
    }
}
