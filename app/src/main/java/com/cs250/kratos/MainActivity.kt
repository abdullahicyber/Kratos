package com.cs250.kratos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.cs250.kratos.databinding.ActivityMainBinding
import com.cs250.kratos.ui.MyAccountFragment
import com.cs250.kratos.ui.MyWorkoutsFragment
import com.cs250.kratos.ui.PeopleFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, PeopleFragment())
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_people -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, PeopleFragment())
                    }
                    true
                }
                R.id.navigation_my_workouts -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, MyWorkoutsFragment())
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
