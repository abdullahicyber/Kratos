package com.cs250.kratos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.cs250.kratos.databinding.ActivityMainBinding
import com.cs250.kratos.ui.MyAccountFragment
import com.cs250.kratos.ui.PeopleFragment

/**
 * # MainActivity
 *
 * The main entry point for signed-in users.
 * This activity hosts the app’s primary navigation and swaps between fragments
 * using a **BottomNavigationView**.
 *
 * ## Responsibilities
 * - Inflate the main layout via [ActivityMainBinding].
 * - Load the default fragment (People screen) on first launch.
 * - Handle bottom navigation selection between:
 *   - [PeopleFragment] → shows all users and allows starting chats.
 *   - [MyAccountFragment] → shows the user’s account info and sign-out button.
 *
 * ## Lifecycle Overview
 * - **onCreate:** Sets up binding, initializes default fragment, and configures
 *   navigation listener.
 *
 * ## Navigation Design
 * The fragments are swapped using the AndroidX `FragmentManager.commit {}` DSL
 * for concise, type-safe fragment transactions.
 *
 * Example transitions:
 * ```kotlin
 * supportFragmentManager.commit {
 *     replace(R.id.fragment_container, PeopleFragment())
 * }
 * ```
 *
 * ## Layout Requirements
 * The layout file `activity_main.xml` should define:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <LinearLayout
 *         android:orientation="vertical"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent">
 *
 *         <FrameLayout
 *             android:id="@+id/fragment_container"
 *             android:layout_width="match_parent"
 *             android:layout_height="0dp"
 *             android:layout_weight="1"/>
 *
 *         <com.google.android.material.bottomnavigation.BottomNavigationView
 *             android:id="@+id/bottomNav"
 *             android:layout_width="match_parent"
 *             android:layout_height="wrap_content"
 *             app:menu="@menu/bottom_nav_menu"/>
 *     </LinearLayout>
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Keep navigation logic inside `MainActivity`; let each fragment handle its own UI.
 * - Use fragment transactions with `commit {}` rather than `beginTransaction().commit()`
 *   for readability.
 * - Rely on ViewBinding to avoid `findViewById` and improve safety.
 */
class   MainActivity : AppCompatActivity() {

    /** ViewBinding instance for accessing layout elements safely. */
    private lateinit var binding: ActivityMainBinding

    /**
     * Initializes the main activity UI and sets up navigation between fragments.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load default fragment only once (first launch).
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, PeopleFragment())
            }
        }

        // Configure bottom navigation to swap fragments.
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_people -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, PeopleFragment())
                    }
                    true
                }
                R.id.navigation_my_account -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, MyAccountFragment())
                    }
                    true
                }
                else -> false
            }
        }
    }
}
