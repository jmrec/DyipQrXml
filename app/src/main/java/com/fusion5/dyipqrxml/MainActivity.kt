package com.fusion5.dyipqrxml

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.fusion5.dyipqrxml.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Set up the ActionBar with the navigation graph
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Listen for navigation changes to update toolbar title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    supportActionBar?.title = "Dyip QR"
                }
                R.id.loginFragment -> {
                    supportActionBar?.title = "Login"
                }
                R.id.signupFragment -> {
                    supportActionBar?.title = "Register"
                }
                R.id.userProfileFragment -> {
                    supportActionBar?.title = "Profile"
                }
                R.id.quickScanFragment -> {
                    supportActionBar?.title = "Quick Scan"
                }
                R.id.terminalsFragment -> {
                    supportActionBar?.title = "Terminals"
                }
                R.id.savedFragment -> {
                    supportActionBar?.title = "Saved"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
