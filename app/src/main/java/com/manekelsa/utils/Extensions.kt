package com.manekelsa.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

// ─────────────────────────────────────────────────────────────────────────────
// Context / Activity Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shows a short Toast message. Usage: context.toast("Hello!")
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Hides the soft keyboard from the currently focused view.
 */
fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
}

// ─────────────────────────────────────────────────────────────────────────────
// View Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sets visibility to VISIBLE.
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Sets visibility to GONE (no space taken in layout).
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Sets visibility to INVISIBLE (space retained in layout).
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Toggles between VISIBLE and GONE.
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Shows a Snackbar with an optional action button.
 *
 * @param message      Text to display
 * @param actionLabel  Label for the optional action button (null = no action)
 * @param action       Lambda executed when the action button is clicked
 */
fun View.snackbar(
    message: String,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
) {
    val snack = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    if (actionLabel != null && action != null) {
        snack.setAction(actionLabel) { action() }
    }
    snack.show()
}

// ─────────────────────────────────────────────────────────────────────────────
// String Validation Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns true if the string is a valid Indian phone number.
 * Accepts formats: 9876543210, +919876543210, 09876543210
 */
fun String.isValidIndianPhone(): Boolean {
    val cleaned = this.replace(" ", "").replace("-", "")
    return Regex("^(\\+91|91|0)?[6-9][0-9]{9}$").matches(cleaned)
}

/**
 * Returns true if the string represents a positive number (for daily rate).
 */
fun String.isValidRate(): Boolean {
    val rate = this.toDoubleOrNull() ?: return false
    return rate > 0
}

/**
 * Formats a phone number for display (adds spaces for readability).
 * "+919876543210" → "+91 98765 43210"
 */
fun String.formatPhoneDisplay(): String {
    return if (startsWith("+91") && length == 13) {
        "${substring(0, 3)} ${substring(3, 8)} ${substring(8)}"
    } else this
}

// ─────────────────────────────────────────────────────────────────────────────
// Number Extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Formats a Double as Indian Rupee amount: 1500.0 → "₹1,500"
 */
fun Double.toRupeeString(): String {
    return "₹${"%,.0f".format(this)}"
}
