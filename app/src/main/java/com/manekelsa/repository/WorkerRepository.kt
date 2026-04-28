package com.manekelsa.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.manekelsa.model.Worker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * WorkerRepository is the single source of truth for all Worker data.
 */
class WorkerRepository {

    companion object {
        private const val COLLECTION = "workers"
        private const val FIELD_AVAILABLE = "isAvailable"
        private const val FIELD_THUMBS_UP = "thumbsUp"
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val workersCollection = db.collection(COLLECTION)

    fun getAllWorkers(): Flow<List<Worker>> = callbackFlow {
        val listener = workersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WorkerRepository", "Listen failed: ${error.message}")
                    // Don't close the flow, just send an empty list on error
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val workers = try {
                    snapshot?.documents?.mapNotNull { doc ->
                        // Log the raw data to see what we are getting
                        android.util.Log.d("WorkerRepository", "Raw Data: ${doc.data}")
                        val w = doc.toObject(Worker::class.java)
                        // Manually set ID and check for 'available' fallback
                        w?.copy(
                            id = doc.id,
                            isAvailable = w.isAvailable || (doc.get("available") as? Boolean ?: false)
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("WorkerRepository", "Mapping failed: ${e.message}")
                    emptyList<Worker>()
                }

                trySend(workers)
            }
        awaitClose { listener.remove() }
    }

    fun getWorkerById(workerId: String): Flow<Worker?> = callbackFlow {
        val listener = workersCollection
            .document(workerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject<Worker>())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addWorker(worker: Worker): Result<String> = try {
        val docRef = workersCollection.add(worker).await()
        android.util.Log.d("WorkerRepository", "Worker added with ID: ${docRef.id}")
        Result.success(docRef.id)
    } catch (e: Exception) {
        android.util.Log.e("WorkerRepository", "Error adding worker: ${e.message}")
        Result.failure(e)
    }

    suspend fun updateAvailability(workerId: String, isAvailable: Boolean): Result<Unit> = try {
        workersCollection.document(workerId).update(FIELD_AVAILABLE, isAvailable).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementThumbsUp(workerId: String): Result<Unit> = try {
        workersCollection.document(workerId).update(FIELD_THUMBS_UP, FieldValue.increment(1)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateWorker(workerId: String, updatedWorker: Worker): Result<Unit> = try {
        workersCollection.document(workerId).set(updatedWorker).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteWorker(workerId: String): Result<Unit> = try {
        workersCollection.document(workerId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
