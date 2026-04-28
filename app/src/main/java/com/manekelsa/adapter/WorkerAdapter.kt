package com.manekelsa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.manekelsa.R
import com.manekelsa.databinding.ItemWorkerBinding
import com.manekelsa.model.Worker
import com.manekelsa.utils.LocationUtils

/**
 * WorkerAdapter binds a [List<Worker>] to a RecyclerView using [ListAdapter],
 * which provides efficient, animated updates via [DiffUtil].
 *
 * Design decisions:
 *  - [ListAdapter] instead of plain [RecyclerView.Adapter]: async diff computation
 *    on a background thread avoids UI jank when Firestore updates arrive.
 *  - ViewBinding instead of findViewById: null-safe, no casting, compile-time checked.
 *  - Lambda callbacks for actions: keeps the adapter decoupled from Activity/ViewModel.
 *
 * @param userLat       Current user latitude (for "X km away" display)
 * @param userLon       Current user longitude
 * @param onCallClick   Called with worker's phone number when Call button is tapped
 * @param onThumbsUp    Called with worker ID when 👍 button is tapped
 * @param onItemClick   Called with Worker object when the card is tapped (opens detail)
 */
class WorkerAdapter(
    private var userLat: Double = 0.0,
    private var userLon: Double = 0.0,
    private val onCallClick: (phone: String) -> Unit,
    private val onThumbsUp: (workerId: String) -> Unit,
    private val onItemClick: (worker: Worker) -> Unit
) : ListAdapter<Worker, WorkerAdapter.WorkerViewHolder>(WorkerDiffCallback()) {

    // ─────────────────────────────────────────────────────────────────────────
    // DiffUtil Callback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DiffUtil compares old and new lists to calculate the minimum set of changes.
     *
     * [areItemsTheSame]: checks identity (same Firestore document ID?)
     * [areContentsTheSame]: checks value equality (did any field change?)
     *
     * The data class [Worker] auto-generates equals() based on all properties,
     * so [areContentsTheSame] can simply use == comparison.
     */
    class WorkerDiffCallback : DiffUtil.ItemCallback<Worker>() {
        override fun areItemsTheSame(oldItem: Worker, newItem: Worker): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Worker, newItem: Worker): Boolean =
            oldItem == newItem
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────────────────

    inner class WorkerViewHolder(
        private val binding: ItemWorkerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds one [Worker] to all views in item_worker.xml.
         * Called by [onBindViewHolder]; keeps binding logic encapsulated in ViewHolder.
         */
        fun bind(worker: Worker) {
            with(binding) {

                // ── Basic info ───────────────────────────────────────────────
                tvWorkerName.text = worker.name
                tvWorkerSkill.text = worker.skill
                tvWorkerRate.text = "₹${worker.dailyRate.toInt()}/day"
                tvThumbsUpCount.text = "👍 ${worker.thumbsUp}"

                // ── Distance label ───────────────────────────────────────────
                val dist = LocationUtils.distanceKm(
                    userLat, userLon, worker.latitude, worker.longitude
                )
                tvDistance.text = if (LocationUtils.isValidLocation(userLat, userLon))
                    LocationUtils.formatDistance(dist)
                else
                    ""

                // ── Availability indicator (coloured dot + text) ──────────────
                val availableColor = ContextCompat.getColor(
                    root.context,
                    if (worker.isAvailable) R.color.available_green else R.color.unavailable_red
                )
                viewAvailabilityDot.setBackgroundColor(availableColor)
                tvAvailabilityStatus.text = if (worker.isAvailable)
                    root.context.getString(R.string.available)
                else
                    root.context.getString(R.string.unavailable)
                tvAvailabilityStatus.setTextColor(availableColor)

                // ── Profile photo via Glide ──────────────────────────────────
                Glide.with(root.context)
                    .load(worker.photoUrl.ifBlank { null })
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(ivWorkerPhoto)

                // ── Click listeners ──────────────────────────────────────────
                btnCall.setOnClickListener {
                    onCallClick(worker.phone)
                }
                btnThumbsUp.setOnClickListener {
                    onThumbsUp(worker.id)
                    // Animate the button for tactile feedback
                    btnThumbsUp.animate()
                        .scaleX(1.3f).scaleY(1.3f).setDuration(100)
                        .withEndAction {
                            btnThumbsUp.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                }
                root.setOnClickListener {
                    onItemClick(worker)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ListAdapter overrides
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the user's GPS position and forces a full rebind so distance
     * labels refresh. Called from MainActivity after a GPS fix.
     *
     * Using notifyDataSetChanged() here is intentional: ALL items need their
     * distance labels refreshed when location first becomes available.
     */
    fun updateUserLocation(lat: Double, lon: Double) {
        userLat = lat
        userLon = lon
        notifyDataSetChanged()
    }
}
