package com.manekelsa.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.manekelsa.R
import com.manekelsa.adapter.WorkerAdapter
import com.manekelsa.databinding.ActivityMainBinding
import com.manekelsa.model.Worker
import androidx.core.view.isVisible
import com.manekelsa.utils.snackbar
import com.manekelsa.utils.toast
import com.manekelsa.viewmodel.WorkerViewModel

/**
 * MainActivity — Primary screen of the Mane-Kelsa app.
 *
 * Responsibilities:
 *  1. Display a RecyclerView list of nearby domestic workers
 *  2. Request GPS permission and fetch user's current location
 *  3. Pass location to ViewModel for distance-based sorting
 *  4. Handle Call (Intent) and Thumbs-Up actions from the list items
 *  5. Navigate to RegisterWorkerActivity via FAB
 *  6. Navigate to WorkerDetailActivity on card tap
 *
 * Architecture role: THIN VIEW — no business logic here.
 * All data transformation happens in [WorkerViewModel].
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding — generated class from activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // ViewModel — survives configuration changes (screen rotation)
    private val viewModel: WorkerViewModel by viewModels()

    // RecyclerView adapter
    private lateinit var adapter: WorkerAdapter

    // FusedLocationProviderClient — Google's recommended location API
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ─────────────────────────────────────────────────────────────────────────
    // Permission launcher (modern API — replaces onRequestPermissionsResult)
    // ─────────────────────────────────────────────────────────────────────────

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchCurrentLocation()
        } else {
            // App still works without location — list sorts alphabetically
            binding.root.snackbar(
                message = getString(R.string.location_permission_denied),
                actionLabel = getString(R.string.retry)
            ) { checkAndRequestLocationPermission() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        checkAndRequestLocationPermission()
        setupFab()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView() {
        adapter = WorkerAdapter(
            onCallClick = { phone -> dialWorker(phone) },
            onThumbsUp = { workerId -> viewModel.thumbsUp(workerId) },
            onItemClick = { worker -> openWorkerDetail(worker) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            // Smooth scrolling performance optimization for long lists
            setHasFixedSize(false)
        }
    }

    private fun setupFab() {
        binding.fabAddWorker.setOnClickListener {
            startActivity(Intent(this, RegisterWorkerActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewModel Observers
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        // Observe the real-time worker list
        viewModel.workers.observe(this) { workers ->
            android.util.Log.d("MainActivity", "Workers received: ${workers.size}")
            adapter.submitList(workers)
            updateEmptyState(workers)
        }

        // Observe UI state for errors / loading feedback
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is WorkerViewModel.UiState.Error -> {
                    binding.root.snackbar(state.message)
                    viewModel.resetUiState()
                }
                else -> Unit
            }
        }
    }

    private fun updateEmptyState(workers: List<Worker>) {
        binding.layoutEmptyState.isVisible = workers.isEmpty()
        binding.recyclerView.isVisible = workers.isNotEmpty()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkAndRequestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                fetchCurrentLocation()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // User previously denied — show rationale then request
                binding.root.snackbar(
                    message = getString(R.string.location_rationale),
                    actionLabel = getString(R.string.grant)
                ) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            else -> {
                // First-time request
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Fetches the device's current GPS location using [FusedLocationProviderClient].
     *
     * Uses [getCurrentLocation] with HIGH_ACCURACY priority (instead of lastLocation)
     * to guarantee a fresh fix even when the device has been stationary.
     *
     * The [CancellationTokenSource] ties the location request to the Activity's
     * lifecycle — cancelled automatically when the Activity is destroyed.
     */
    @SuppressLint("MissingPermission") // permission checked before this call
    private fun fetchCurrentLocation() {
        val cancellationToken = CancellationTokenSource()

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                location?.let {
                    // Pass coordinates to ViewModel — triggers re-sort of worker list
                    viewModel.setUserLocation(it.latitude, it.longitude)
                    // Also update adapter so distance labels refresh immediately
                    adapter.updateUserLocation(it.latitude, it.longitude)
                } ?: run {
                    // Location is null — device has no GPS fix (indoors, airplane mode)
                    toast(getString(R.string.location_unavailable))
                }
            }
            .addOnFailureListener {
                toast(getString(R.string.location_error))
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation & Actions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the phone dialler pre-filled with the worker's number.
     * ACTION_DIAL (not ACTION_CALL) — requires no runtime permission,
     * the user manually presses dial.
     */
    private fun dialWorker(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        startActivity(intent)
    }

    private fun openWorkerDetail(worker: Worker) {
        val intent = Intent(this, WorkerDetailActivity::class.java).apply {
            putExtra(WorkerDetailActivity.EXTRA_WORKER_ID, worker.id)
            putExtra(WorkerDetailActivity.EXTRA_WORKER_NAME, worker.name)
        }
        startActivity(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // Firestore real-time listener auto-refreshes — this just re-fetches location
                checkAndRequestLocationPermission()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
