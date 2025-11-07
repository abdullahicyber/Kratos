package com.cs250.kratos.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * # HomeViewModel
 *
 * A simple [ViewModel] that holds and exposes observable text data
 * for the [HomeFragment].
 *
 * This follows the **MVVM (Model–View–ViewModel)** architecture:
 * - The **ViewModel** stores and manages UI-related data.
 * - The **View (Fragment)** observes this data and updates the UI automatically.
 *
 * ## Behavior
 * - `_text`: a private [MutableLiveData] that stores the current message.
 * - `text`: a public immutable [LiveData] exposed to the UI.
 *
 * The use of immutable LiveData ensures that the UI layer can **observe**
 * changes but cannot **mutate** the data directly — enforcing a unidirectional
 * data flow from ViewModel → UI.
 *
 * ## Example Usage
 * ```kotlin
 * // Inside HomeFragment:
 * homeViewModel.text.observe(viewLifecycleOwner) { message ->
 *     binding.textHome.text = message
 * }
 * ```
 *
 * ## Best Practices
 * - Keep [MutableLiveData] private inside the ViewModel.
 * - Expose only immutable [LiveData] to the Fragment or Activity.
 * - Perform data updates inside the ViewModel (not from the UI).
 */
class HomeViewModel : ViewModel() {

    /** Backing field for the text displayed on the Home screen. */
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }

    /**
     * Public read-only LiveData for UI observation.
     * Fragments should observe this instead of modifying it directly.
     */
    val text: LiveData<String> = _text
}
