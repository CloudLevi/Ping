package com.cloudlevi.ping

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*
import android.os.Build
import android.annotation.TargetApi
import android.content.SharedPreferences
import androidx.core.view.forEach
import com.cloudlevi.ping.ext.applyText
import com.cloudlevi.ping.ext.visibleOrGone
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val viewModelMainActivity: MainActivityViewModel by viewModels()

    lateinit var googleSignInClient: GoogleSignInClient
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(BuildConfig.G_CLIENT_ID)
        .requestEmail()
        .build()

    @Inject
    lateinit var dataStoreManager: PreferencesManager

    private var sharedPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ToyotaUI.main()

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            val currentUser = firebaseAuth.currentUser!!
            viewModelMainActivity.setUserData(
                currentUser.uid,
                currentUser.email ?: "",
                currentUser.displayName ?: ""
            )
            setupViews(true)


        } else setupViews(false)
    }

    override fun attachBaseContext(base: Context) {
        sharedPrefs = base.getSharedPreferences("prefs", 0)
        super.attachBaseContext(updateBaseContextLocale(base))
    }

    private fun updateBaseContextLocale(context: Context): Context? {
        val language = sharedPrefs?.getString("language_code", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else updateResourcesLocaleLegacy(context, locale)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context? {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context? {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    override fun onResume() {
        super.onResume()
        setUserOnline(true)
    }

    override fun onPause() {
        super.onPause()
        setUserOnline(false)
    }

    fun setUserOnline(isOnline: Boolean) {
        viewModelMainActivity.setUserOnline(isOnline)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupViews(loggedIn: Boolean) {

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        when (loggedIn) {
            true -> navGraph.startDestination = R.id.homeFragment
            false -> navGraph.startDestination = R.id.loginFragment
        }

        navController.graph = navGraph

        NavigationUI.setupWithNavController(binding.bottomMenu, navController)
    }

//    override fun onBackPressed() {
//        if (navController.currentDestination?.id == R.id.loginFragment) finish()
//        else super.onBackPressed()
//    }

    fun hideNavigation() {
        binding.bottomMenu.visibility = View.GONE
    }

    fun showNavigation() {
        binding.bottomMenu.visibility = View.VISIBLE
    }

    fun switchLoading(isLoading: Boolean, progressText: String = "") {
        binding.apply {
            progressLayout.visibleOrGone(isLoading)
            bottomMenu.menu.forEach {
                it.isEnabled = !isLoading
            }
            progressTV.applyText(progressText)
        }
    }

    fun changeLoadingText(progressText: String) {
        binding.progressTV.applyText(progressText)
    }

    fun setLocale(languageCode: String?) {
        sharedPrefs?.edit()?.putString("language_code", languageCode)?.commit()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources: Resources = resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        invalidateBottomBar()
    }

    private fun invalidateBottomBar() {
        binding.bottomMenu.menu.clear()
        binding.bottomMenu.inflateMenu(R.menu.bottom_navigation)
        NavigationUI.setupWithNavController(binding.bottomMenu, navController)
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