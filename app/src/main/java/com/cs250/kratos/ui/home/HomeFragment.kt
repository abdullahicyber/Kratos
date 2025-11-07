package com.cs250.kratos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cs250.kratos.databinding.FragmentHomeBinding

/**
 * # HomeFragment
 *
 * A simple [Fragment] that displays the app’s home screen content.
 * It observes a [HomeViewModel] and updates the UI reactively when
 * the underlying LiveData changes.
 *
 * This follows the **MVVM (Model–View–ViewModel)** architecture:
 * - **ViewModel (HomeViewModel)** → holds the data (e.g., text or state)
 * - **View (HomeFragment)** → observes data and renders UI updates
 *
 * ## Lifecycle Overview
 * - `onCreateView`: inflates the layout using ViewBinding, creates/gets the ViewModel, and sets up observers.
 * - `onDestroyView`: clears the binding reference to avoid memory leaks.
 *
 * ## ViewModel Integration
 * The [HomeViewModel] provides a `LiveData<String>` (`text` property)
 * that the fragment observes. Whenever `text` changes, it updates
 * the corresponding TextView (`textHome`) automatically.
 *
 * ## Layout Reference
 * The fragment uses `fragment_home.xml` which should define a TextView like:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <TextView
 *         android:id="@+id/text_home"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:text="Home" />
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Only access [binding] between `onCreateView` and `onDestroyView`.
 * - Keep UI logic inside the Fragment, and business/data logic inside the ViewModel.
 * - Use `viewLifecycleOwner` for LiveData observation to respect Fragment lifecycle.
 */
class HomeFragment : Fragment() {

    /** Backing property for view binding. */
    private var _binding: FragmentHomeBinding? = null

    /**
     * Exposed non-null binding, valid only between onCreateView and onDestroyView.
     * Always access this instead of `_binding` directly.
     */
    private val binding get() = _binding!!

    /**
     * Called to inflate the layout and initialize the ViewModel and UI bindings.
     *
     * @param inflater Used to inflate the XML layout.
     * @param container Optional parent view for the fragment’s UI.
     * @param savedInstanceState Saved instance state bundle, if any.
     * @return The root [View] of the fragment’s layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtain the ViewModel scoped to this fragment.
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        // Inflate layout via generated binding class.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Observe LiveData from the ViewModel and update the UI reactively.
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) { newText ->
            textView.text = newText
        }

        return root
    }

    /**
     * Clears the binding reference when the view hierarchy is destroyed
     * to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
