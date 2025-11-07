package com.cs250.kratos.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * # DashboardViewModel
 *
 * A simple [ViewModel] that exposes a piece of observable text to be displayed
 * in the [DashboardFragment].
 *
 * This follows the **MVVM (Model–View–ViewModel)** pattern:
 * - The **ViewModel** holds UI data (`text`) that survives configuration changes.
 * - The **View (Fragment)** observes this data via [LiveData] and updates the UI automatically.
 *
 * ## Behavior
 * - The private `_text` holds a mutable `MutableLiveData<String>`.
 * - The public [text] property exposes an immutable `LiveData<String>` for observation.
 *   This ensures that UI code can **observe** data but not **modify** it directly.
 *
 * ## Example Usage
 * ```kotlin
 * // In DashboardFragment:
 * dashboardViewModel.text.observe(viewLifecycleOwner) { message ->
 *     binding.textDashboard.text = message
 * }
 * ```
 *
 * ## Best Practices
 * - Always expose `LiveData` publicly and keep `MutableLiveData` private.
 * - Update `_text.value` inside the ViewModel only when business logic changes.
 * - The View should never mutate ViewModel data directly.
 */
class DashboardViewModel : ViewModel() {

    /** Backing field for dashboard text (mutable within the ViewModel only). */
    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }

    /**
     * Public immutable LiveData that the UI observes.
     * Exposing it as LiveData prevents accidental modification from the UI layer.
     */
    val text: LiveData<String> = _text
}
