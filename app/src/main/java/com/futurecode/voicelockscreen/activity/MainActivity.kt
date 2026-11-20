package com.futurecode.voicelockscreen.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityMainBinding
import com.futurecode.voicelockscreen.utils.setLightStatusBarWithDarkIcons

class MainActivity : BaseActivity() {
    private var navController: NavController? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Option A: Custom Dark Color Pass karein (Matching your screenshot)
        // App is light-only: splash and language selection use the light status bar with dark icons.
        window.setLightStatusBarWithDarkIcons(getColor(R.color.bg))
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = navHostFragment!!.navController

    }

    fun goToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}