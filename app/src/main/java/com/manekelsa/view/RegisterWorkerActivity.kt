package com.manekelsa.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.manekelsa.R
import com.manekelsa.databinding.ActivityRegisterWorkerBinding
import com.manekelsa.model.Worker
import com.manekelsa.utils.hideKeyboard
import com.manekelsa.utils.isValidIndianPhone
import com.manekelsa.utils.isValidRate
import com.manekelsa.utils.toast
import com.manekelsa.viewmodel.WorkerViewModel

/**
 * RegisterWorkerActivity — Screen for workers to manage their professional profile.
 */
class RegisterWorkerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterWorkerBinding
    private val viewModel: WorkerViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var workerLatitude: Double = 0.0
    private var workerLongitude: Double = 0.0
    private var selectedPhotoUri: Uri? = null

    /** Opens the photo gallery and receives the selected image URI */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPhotoUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.ivProfilePhoto)
        }
    }

    /** Requests fine location permission for capturing worker's GPS coordinates */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) fetchWorkerLocation()
        else toast(getString(R.string.location_permission_denied))
    }
    
    // Add logout functionality for workers
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menu.add(0, 100, 0, "Logout")
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == 100) {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterWorkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // For workers, we don't show the back button since this is their home screen
        setupToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        setupSkillSpinner()
        setupClickListeners()
        observeViewModel()
        fetchWorkerLocationOnStart()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.register_worker_title)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSkillSpinner() {
        val skills = resources.getStringArray(R.array.skills)
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            skills
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSkill.adapter = spinnerAdapter
    }

    private fun setupClickListeners() {
        binding.ivProfilePhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSelectPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                saveWorker()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates all input fields and highlights errors inline.
     * Returns true only when all fields pass validation.
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        val name = binding.etName.text.toString().trim()
        if (name.length < 2) {
            binding.tilName.error = getString(R.string.error_name)
            isValid = false
        } else {
            binding.tilName.error = null
        }

        val phone = binding.etPhone.text.toString().trim()
        if (!phone.isValidIndianPhone()) {
            binding.tilPhone.error = getString(R.string.error_phone)
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        val rateStr = binding.etRate.text.toString().trim()
        if (!rateStr.isValidRate()) {
            binding.tilRate.error = getString(R.string.error_rate)
            isValid = false
        } else {
            binding.tilRate.error = null
        }

        return isValid
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save Worker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Orchestrates the save flow:
     *  1. Show loading state
     *  2. Upload photo to Firebase Storage (if selected)
     *  3. Build [Worker] object with all fields
     *  4. Pass to ViewModel → Repository → Firestore
     */
    private fun saveWorker() {
        showLoading(true)

        val photoUri = selectedPhotoUri
        if (photoUri != null) {
            uploadPhotoThenSave(photoUri)
        } else {
            buildAndSaveWorker(photoUrl = "")
        }
    }

    /**
     * Uploads the selected photo to Firebase Storage under "profile_photos/{timestamp}.jpg".
     * On success, proceeds to save the worker with the download URL.
     * On failure, saves without photo (graceful degradation).
     */
    private fun uploadPhotoThenSave(photoUri: Uri) {
        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("profile_photos/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(photoUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    buildAndSaveWorker(photoUrl = downloadUri.toString())
                }
            }
            .addOnFailureListener {
                // Photo upload failed — save worker anyway without photo
                toast(getString(R.string.photo_upload_failed))
                buildAndSaveWorker(photoUrl = "")
            }
    }

    private fun buildAndSaveWorker(photoUrl: String) {
        val worker = Worker(
            name = binding.etName.text.toString().trim(),
            skill = binding.spinnerSkill.selectedItem.toString(),
            phone = binding.etPhone.text.toString().trim(),
            dailyRate = binding.etRate.text.toString().toDoubleOrNull() ?: 0.0,
            photoUrl = photoUrl,
            isAvailable = binding.switchAvailable.isChecked,
            latitude = workerLatitude,
            longitude = workerLongitude,
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        )
        viewModel.addWorker(worker)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location
    // ─────────────────────────────────────────────────────────────────────────

    private fun fetchWorkerLocationOnStart() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchWorkerLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchWorkerLocation() {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                location?.let {
                    workerLatitude = it.latitude
                    workerLongitude = it.longitude
                    binding.tvLocationStatus.text = getString(R.string.location_captured)
                } ?: run {
                    binding.tvLocationStatus.text = getString(R.string.location_unavailable)
                }
            }
            .addOnFailureListener {
                binding.tvLocationStatus.text = getString(R.string.location_error)
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewModel Observer
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is WorkerViewModel.UiState.Loading -> showLoading(true)

                is WorkerViewModel.UiState.Success -> {
                    showLoading(false)
                    toast(state.message)
                    viewModel.resetUiState()
                    // Don't finish() here if it's the home screen for workers
                }

                is WorkerViewModel.UiState.Error -> {
                    showLoading(false)
                    toast(state.message)
                    viewModel.resetUiState()
                }

                else -> showLoading(false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loading State
    // ─────────────────────────────────────────────────────────────────────────

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSave.isEnabled = !isLoading
        binding.btnSave.text = if (isLoading)
            getString(R.string.saving)
        else
            getString(R.string.save)
    }
}
