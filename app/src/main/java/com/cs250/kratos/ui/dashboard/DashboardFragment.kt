package com.cs250.kratos.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cs250.kratos.databinding.FragmentDashboardBinding

/**
 * # DashboardFragment
 *
 * A simple [Fragment] that displays dashboard-related UI and reacts to
 * observable data from a [DashboardViewModel].
 *
 * This class demonstrates:
 * - **View Binding** (via [FragmentDashboardBinding])
 * - **ViewModel** integration using [ViewModelProvider]
 * - Proper **lifecycle cleanup** by nullifying the binding reference in [onDestroyView]
 *
 * ## Lifecycle Overview
 * - `onCreateView`: inflates the layout and sets up data observation.
 * - `onDestroyView`: clears `_binding` to prevent memory leaks.
 *
 * ## ViewModel Behavior
 * The [DashboardViewModel] exposes a `LiveData<String>` called `text`
 * which this fragment observes. Whenever the text changes, it updates
 * the dashboard's main `TextView` in real time.
 *
 * ## Layout Reference
 * Uses `fragment_dashboard.xml` with a root `<layout>` tag for ViewBinding.
 * Example:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <TextView
 *         android:id="@+id/text_dashboard"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:text="Dashboard" />
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Always use `binding` only between `onCreateView` and `onDestroyView`.
 * - Never store the fragment’s context or view references beyond their lifecycle.
 */
class DashboardFragment : Fragment() {

    /** Backing property for view binding. */
    private var _binding: FragmentDashboardBinding? = null

    /**
     * Exposed non-null binding for use within lifecycle-safe window
     * (between onCreateView and onDestroyView).
     */
    private val binding get() = _binding!!

    /**
     * Called to inflate the fragment’s layout and initialize UI components.
     *
     * @param inflater Used to inflate the XML layout.
     * @param container Optional parent view the fragment’s UI should attach to.
     * @param savedInstanceState State bundle for restoring UI state (if any).
     * @return The root [View] for this fragment’s UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtain ViewModel scoped to this Fragment
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        // Inflate layout using generated ViewBinding class
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Observe LiveData from ViewModel and update TextView when it changes
        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) { newText ->
            textView.text = newText
        }

        return root
    }

    /**
     * Called when the fragment’s view hierarchy is being destroyed.
     * Here we clear the binding reference to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
