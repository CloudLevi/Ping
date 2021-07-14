package com.cloudlevi.ping

import android.content.ContentValues
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val viewModelMainActivity: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null){
            val currentUser = firebaseAuth.currentUser!!
            viewModelMainActivity.setUserData(currentUser.uid,
                currentUser.email?: "",
                currentUser.displayName?: "")
            setupViews(true)


        } else setupViews(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupViews(loggedIn: Boolean){

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        when(loggedIn){
            true -> navGraph.startDestination = R.id.homeFragment
            false -> navGraph.startDestination = R.id.loginFragment
        }

        navController.graph = navGraph

        NavigationUI.setupWithNavController(binding.bottomMenu, navController)
        //setupActionBarWithNavController(navController)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id){
                R.id.loginFragment -> binding.bottomMenu.visibility = View.GONE
                R.id.registerFragment -> binding.bottomMenu.visibility = View.GONE
                R.id.homeFragment -> binding.bottomMenu.visibility = View.VISIBLE
            }
        }

    }
}

const val REQUEST_ERROR_EMAIL_FIELD = 1
const val REQUEST_ERROR_PASSWORD_FIELD = 2
const val REQUEST_ERROR_CONFIRM_PASSWORD_FIELD = 3
const val REQUEST_ERROR_USERNAME_FIELD = 4

const val PRICE_TYPE_ALL = -1
const val PRICE_TYPE_PER_DAY = 0
const val PRICE_TYPE_PER_WEEK = 1
const val PRICE_TYPE_PER_MONTH = 2

const val APT_TYPE_ALL = -1
const val APT_TYPE_FLAT = 0
const val APT_TYPE_HOUSE = 1

const val APT_FURNISHED_ALL = -1
const val APT_FURNISHED_YES = 0
const val APT_FURNISHED_NO = 1

const val PICKER_TYPE_FLOOR = 1
const val PICKER_TYPE_ROOMS = 2
const val CLICK_TYPE_PLUS = 1
const val CLICK_TYPE_MINUS = 2
const val HOMEFRAGMENT_LISTVIEW = 1
const val HOMEFRAGMENT_GRIDVIEW = 2

const val CHEAPEST_FIRST_CHIP = 0
const val EXPENSIVE_FIRST_CHIP = 1
const val NEWEST_FIRST_CHIP = 2
const val HIGHER_RATED_FIRST_CHIP = 3

const val SLIDER_TYPE_RATING = 0
const val SLIDER_TYPE_FLOOR = 1
const val SLIDER_TYPE_ROOMS = 2
const val SLIDER_TYPE_PRICE = 3