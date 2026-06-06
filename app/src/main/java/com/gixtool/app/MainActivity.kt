package com.gixtool.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gixtool.app.ads.AdManager
import com.gixtool.app.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on for performance monitoring
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize AdMob
        MobileAds.initialize(this) {
            adManager = AdManager(this)
            adManager.loadBannerAd(binding.adBannerContainer)
            adManager.loadInterstitialAd()
            adManager.loadRewardedAd()
        }

        // Navigation setup
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    fun getAdManager(): AdManager? = if (::adManager.isInitialized) adManager else null

    override fun onDestroy() {
        super.onDestroy()
        if (::adManager.isInitialized) adManager.destroy()
    }
}
