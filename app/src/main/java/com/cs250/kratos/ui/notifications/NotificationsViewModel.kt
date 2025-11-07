package com.cs250.kratos.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * # NotificationsViewModel
 *
 * A simple [ViewModel] that provides observable text for the
 * [NotificationsFragment] to display.
 *
 * This ViewModel demonstrates the **MVVM (Model–View–ViewModel)** pattern:
 * - The **ViewModel** manages the UI-related data and survives configuration changes.
 * - The **View (Fragment)** observes this data through [LiveData] and updates its UI automatically.
 *
 * ## Behavior
 * - `_text` is a private [MutableLiveData] used internally to hold the notification message.
 * - `text` is a public immutable [LiveData] that the UI observes.
 * - Exposing only immutable [LiveData] enforces **unidirectional data flow** (ViewModel → UI),
 *   which helps maintain separation of concerns and prevents the UI from modifying ViewModel state directly.
 *
 * ## Example Usage
 * ```kotlin
 * // Inside NotificationsFragment:
 * notificationsViewModel.text.observe(viewLifecycleOwner) { message ->
 *     binding.textNotifications.text = message
 * }
 * ```
 *
 * ## Best Practices
 * - Keep mutable LiveData private and expose immutable LiveData publicly.
 * - Use ViewModels to encapsulate UI logic and preserve data during rotation/config changes.
 * - Trigger LiveData updates from within the ViewModel (never from the Fragment).
 */
class NotificationsViewModel : ViewModel() {

    /** Private backing property for the text displayed in the Notifications screen. */
    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }

    /**
     * Public immutable LiveData exposed to the UI.
     * Fragments can observe this but cannot modify it directly.
     */
    val text: LiveData<String> = _text
}
