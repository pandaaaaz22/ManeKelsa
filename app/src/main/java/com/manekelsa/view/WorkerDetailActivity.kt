package com.manekelsa.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.manekelsa.R
import com.manekelsa.databinding.ActivityWorkerDetailBinding
import com.manekelsa.model.Worker
import com.manekelsa.utils.LocationUtils
import com.manekelsa.utils.hide
import com.manekelsa.utils.show
import com.manekelsa.utils.snackbar
import com.manekelsa.utils.toast
import com.manekelsa.viewmodel.WorkerViewModel

/**
 * WorkerDetailActivity — Full profile view for a single domestic worker.
 *
 * Features on this screen:
 *  - Full profile display (photo, name, skill, rate, distance, rating)
 *  - Availability toggle (SwitchCompat → real-time Firestore update)
 *  - Call button (Intent.ACTION_DIAL)
 *  - Thumbs-up button with confirmation dialog (prevents accidental ratings)
 *  - WhatsApp quick-message button
 *  - Edit profile button (navigates to RegisterWorkerActivity in edit mode)
 *
 * The worker is loaded from Firestore in real-time using [workerId] passed
 * via Intent extras, so data stays fresh even if the list updated while
 * this screen was open.
 */
class WorkerDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORKER_ID = "extra_worker_id"
        const val EXTRA_WORKER_NAME = "extra_worker_name"
    }

    private lateinit var binding: ActivityWorkerDetailBinding
    private val viewModel: WorkerViewModel by viewModels()

    private var workerId: String = ""
    private var currentWorker: Worker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workerId = intent.getStringExtra(EXTRA_WORKER_ID) ?: run {
            toast("Worker not found")
            finish()
            return
        }

        val workerName = intent.getStringExtra(EXTRA_WORKER_NAME) ?: ""
        setupToolbar(workerName)
        observeWorker()
        observeUiState()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupToolbar(workerName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = workerName.ifBlank { getString(R.string.worker_profile) }
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data Observation — real-time single worker document
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeWorker() {
        // Re-use the WorkerRepository's single-document Flow via an on-the-fly LiveData
        // We leverage the workers LiveData and filter for our specific workerId
        viewModel.workers.observe(this) { workers ->
            val worker = workers.find { it.id == workerId }
            worker?.let {
                currentWorker = it
                bindWorkerData(it)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is WorkerViewModel.UiState.Success -> {
                    binding.root.snackbar(state.message)
                    viewModel.resetUiState()
                }
                is WorkerViewModel.UiState.Error -> {
                    binding.root.snackbar(state.message)
                    viewModel.resetUiState()
                }
                else -> Unit
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data Binding — maps Worker fields to Views
    // ─────────────────────────────────────────────────────────────────────────

    private fun bindWorkerData(worker: Worker) {
        with(binding) {

            // ── Text fields ──────────────────────────────────────────────────
            tvDetailName.text = worker.name
            tvDetailSkill.text = worker.skill
            tvDetailRate.text = "₹${worker.dailyRate.toInt()} / day"
            tvDetailPhone.text = worker.phone
            tvDetailThumbsUp.text = "👍  ${worker.thumbsUp} ratings"

            // ── Distance ────────────────────────────────────────────────────
            val dist = viewModel.distanceTo(worker)
            tvDetailDistance.text = LocationUtils.formatDistance(dist)
            tvDetailDistance.isVisible = dist >= 0

            // ── Availability status chip ─────────────────────────────────────
            if (worker.isAvailable) {
                chipAvailability.text = getString(R.string.available)
                chipAvailability.setChipBackgroundColorResource(R.color.available_green)
            } else {
                chipAvailability.text = getString(R.string.unavailable)
                chipAvailability.setChipBackgroundColorResource(R.color.unavailable_red)
            }

            // ── Availability switch — update WITHOUT triggering listener ──────
            // Temporarily remove listener to avoid feedback loop when we set the
            // switch state programmatically (Firestore update → rebind → switch.isChecked = X)
            switchDetailAvailability.setOnCheckedChangeListener(null)
            switchDetailAvailability.isChecked = worker.isAvailable
            switchDetailAvailability.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleAvailability(workerId, isChecked)
            }

            // ── Profile photo ────────────────────────────────────────────────
            Glide.with(this@WorkerDetailActivity)
                .load(worker.photoUrl.ifBlank { null })
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .centerCrop()
                .into(ivDetailPhoto)

            // ── Action buttons ───────────────────────────────────────────────
            btnDetailCall.setOnClickListener {
                dialWorker(worker.phone)
            }

            btnDetailWhatsapp.setOnClickListener {
                openWhatsApp(worker.phone)
            }

            btnDetailThumbsUp.setOnClickListener {
                showThumbsUpConfirmation(worker)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────────

    private fun dialWorker(phone: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    private fun openWhatsApp(phone: String) {
        // Remove leading zeros and country code formatting for WhatsApp deep-link
        val cleanedPhone = phone.replace(Regex("[^0-9+]"), "")
        val whatsappPhone = if (cleanedPhone.startsWith("+")) cleanedPhone else "+91$cleanedPhone"
        val message = Uri.encode(getString(R.string.whatsapp_greeting))
        val url = "https://api.whatsapp.com/send?phone=$whatsappPhone&text=$message"

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast(getString(R.string.whatsapp_not_installed))
        }
    }

    /**
     * Shows a confirmation dialog before incrementing thumbsUp.
     * Prevents accidental ratings from a single misclick.
     */
    private fun showThumbsUpConfirmation(worker: Worker) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rate_worker))
            .setMessage(getString(R.string.rate_confirmation, worker.name))
            .setPositiveButton(getString(R.string.yes_thumbs_up)) { _, _ ->
                viewModel.thumbsUp(workerId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
