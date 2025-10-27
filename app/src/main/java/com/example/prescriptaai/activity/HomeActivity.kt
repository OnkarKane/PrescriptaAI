package com.example.prescriptaai.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.prescriptaai.R
import com.example.prescriptaai.databinding.ActivityHomeBinding
import com.example.prescriptaai.fragments.HomeFragment // Import the new HomeFragment
import com.example.prescriptaai.fragments.ProfileFragment // Import the new ProfileFragment
import com.example.prescriptaai.fragments.ScanFragment // Import the new ScanFragment
import com.example.prescriptaai.fragments.PatientsFragment


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- NEW: Load the HomeFragment by default when the activity starts ---
        if (savedInstanceState == null) { // To prevent re-adding fragment on configuration change
            replaceFragment(HomeFragment())
            // Set the home item as checked in the navigation menu
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
        // Set a listener for the bottom navigation view
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_scan -> {
                    replaceFragment(ScanFragment())
                    true
                }
                R.id.nav_patients -> {
                    replaceFragment(PatientsFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}