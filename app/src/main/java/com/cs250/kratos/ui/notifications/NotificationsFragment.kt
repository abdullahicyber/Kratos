package com.cs250.kratos.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cs250.kratos.databinding.FragmentNotificationsBinding

/**
 * # NotificationsFragment
 *
 * A simple [Fragment] that displays notification-related content and observes
 * a [NotificationsViewModel] for UI data.
 *
 * This class demonstrates:
 * - **MVVM pattern** — where the [ViewModel] holds data and the Fragment observes it.
 * - **ViewBinding** — used for type-safe view access.
 * - **Lifecycle safety** — binding is only valid between `onCreateView` and `onDestroyView`.
 *
 * ## Lifecycle Summary
 * - **onCreateView:**
 *   Inflates the layout, initializes the ViewModel, and sets up LiveData observation.
 * - **onDestroyView:**
 *   Clears the binding reference to avoid memory leaks.
 *
 * ## ViewModel Behavior
 * The [NotificationsViewModel] provides a `LiveData<String>` (`text` property)
 * which emits text updates to display in a TextView (`textNotifications`).
 * The Fragment observes this LiveData and updates the UI automatically.
 *
 * ## Layout Reference
 * The layout file `fragment_notifications.xml` should define a TextView like:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <TextView
 *         android:id="@+id/text_notifications"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:text="Notifications" />
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Only access [binding] between `onCreateView` and `onDestroyView`.
 * - Use `viewLifecycleOwner` when observing LiveData to ensure proper cleanup.
 * - Keep UI logic here and business logic in the ViewModel.
 */
class NotificationsFragment : Fragment() {

    /** Backing property for view binding (nullable outside the valid lifecycle). */
    private var _binding: FragmentNotificationsBinding? = null

    /**
     * Non-null binding property valid only between `onCreateView` and `onDestroyView`.
     * Access this instead of `_binding` directly.
     */
    private val binding get() = _binding!!

    /**
     * Inflates the fragment layout, initializes the ViewModel, and sets up observers.
     *
     * @param inflater Used to inflate the XML layout.
     * @param container Optional parent view that the UI attaches to.
     * @param savedInstanceState Saved instance state bundle, if any.
     * @return The root [View] representing the fragment’s layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtain the ViewModel scoped to this Fragment
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        // Inflate the layout using ViewBinding
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Observe LiveData from the ViewModel and update UI reactively
        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) { newText ->
            textView.text = newText
        }

        return root
    }

    /**
     * Clears the binding reference when the fragment’s view is destroyed
     * to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
