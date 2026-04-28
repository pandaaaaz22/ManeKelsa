package com.manekelsa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.manekelsa.model.Worker
import com.manekelsa.repository.WorkerRepository
import com.manekelsa.utils.LocationUtils
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * WorkerViewModel acts as the bridge between the UI layer (Activities/Fragments)
 * and the data layer (WorkerRepository).
 *
 * Key responsibilities:
 *  - Exposes [workers] LiveData (sorted by distance) for the list screen
 *  - Exposes [uiState] for loading/error/success feedback
 *  - Stores the user's current GPS coordinates for distance sorting
 *  - Delegates all Firestore operations to [WorkerRepository]
 *  - Survives configuration changes (screen rotation) — Activities observe, not hold data
 *
 * Lifecycle: bound to the Activity/Fragment back-stack entry via viewModels() delegate.
 */
class WorkerViewModel : ViewModel() {

    private val repository = WorkerRepository()

    // ─────────────────────────────────────────────────────────────────────────
    // UI State — single sealed class to represent all screen states
    // ─────────────────────────────────────────────────────────────────────────

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    // ─────────────────────────────────────────────────────────────────────────
    // User Location — set once after FusedLocationProviderClient delivers a fix
    // ─────────────────────────────────────────────────────────────────────────

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    /**
     * Called from MainActivity after a successful GPS fix.
     * Triggers recomputation of the sorted worker list.
     */
    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        // Force LiveData re-emission by toggling a refresh trigger
        _locationReady.value = true
    }

    private val _locationReady = MutableLiveData(false)

    // ─────────────────────────────────────────────────────────────────────────
    // Worker List — real-time Flow from Firestore, mapped to sorted LiveData
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Live list of all workers, sorted by distance from the user's GPS position.
     * Emits a new list every time Firestore data changes (real-time updates).
     *
     * If location is unavailable (user denied permission), list is sorted
     * by name alphabetically as a sensible fallback.
     */
    val workers: LiveData<List<Worker>> = repository
        .getAllWorkers()
        .map { list -> sortWorkers(list) }
        .asLiveData()

    // ─────────────────────────────────────────────────────────────────────────
    // Filter State
    // ─────────────────────────────────────────────────────────────────────────

    private val _selectedSkillFilter = MutableLiveData<String?>(null)
    val selectedSkillFilter: LiveData<String?> = _selectedSkillFilter

    /**
     * Filtered + sorted worker list based on selected skill chip.
     */
    val filteredWorkers: LiveData<List<Worker>> = workers.map { list ->
        val filter = _selectedSkillFilter.value
        if (filter.isNullOrBlank()) list
        else list.filter { it.skill.equals(filter, ignoreCase = true) }
    }

    fun setSkillFilter(skill: String?) {
        _selectedSkillFilter.value = skill
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sorting Logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun sortWorkers(workers: List<Worker>): List<Worker> {
        return if (userLatitude == 0.0 && userLongitude == 0.0) {
            // Fallback: sort alphabetically when GPS is unavailable
            workers.sortedBy { it.name }
        } else {
            // Primary: sort by distance (nearest first) using Haversine formula
            workers.sortedBy { worker ->
                LocationUtils.distanceKm(
                    userLatitude, userLongitude,
                    worker.latitude, worker.longitude
                )
            }
        }
    }

    /**
     * Calculates the distance (in km) from the user to a specific worker.
     * Used in item views to display "X km away".
     */
    fun distanceTo(worker: Worker): Double {
        if (userLatitude == 0.0 && userLongitude == 0.0) return -1.0
        return LocationUtils.distanceKm(
            userLatitude, userLongitude,
            worker.latitude, worker.longitude
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write Operations — each launches a coroutine in viewModelScope
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a new worker profile. On success, emits [UiState.Success].
     * On failure, emits [UiState.Error] with the exception message.
     */
    fun addWorker(worker: Worker) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.addWorker(worker).fold(
                onSuccess = {
                    _uiState.value = UiState.Success("Worker registered successfully!")
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Failed to save worker")
                }
            )
        }
    }

    /**
     * Toggles the 'isAvailable' field for [workerId].
     * Called from the SwitchCompat in WorkerDetailActivity.
     */
    fun toggleAvailability(workerId: String, isAvailable: Boolean) {
        viewModelScope.launch {
            repository.updateAvailability(workerId, isAvailable).onFailure { e ->
                _uiState.value = UiState.Error("Failed to update availability: ${e.message}")
            }
        }
    }

    /**
     * Increments thumbsUp for [workerId] by 1 (atomic server-side operation).
     */
    fun thumbsUp(workerId: String) {
        viewModelScope.launch {
            repository.incrementThumbsUp(workerId).onFailure { e ->
                _uiState.value = UiState.Error("Rating failed: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing worker's profile data.
     */
    fun updateWorker(workerId: String, worker: Worker) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.updateWorker(workerId, worker).fold(
                onSuccess = { _uiState.value = UiState.Success("Profile updated!") },
                onFailure = { e -> _uiState.value = UiState.Error(e.message ?: "Update failed") }
            )
        }
    }

    /**
     * Deletes a worker from Firestore.
     */
    fun deleteWorker(workerId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.deleteWorker(workerId).fold(
                onSuccess = { _uiState.value = UiState.Success("Worker removed") },
                onFailure = { e -> _uiState.value = UiState.Error(e.message ?: "Delete failed") }
            )
        }
    }

    /**
     * Reset UI state back to idle — call after consuming a Success or Error event.
     */
    fun resetUiState() {
        _uiState.value = UiState.Idle
    }
}
