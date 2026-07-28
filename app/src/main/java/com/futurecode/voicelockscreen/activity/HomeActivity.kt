package com.futurecode.voicelockscreen.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity() { // Assumed mapping, adjust if you inherit directly from AppCompatActivity

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIXED: Pehle static R.layout wala setContentView bilkul hata diya.
        // Ab hum sirf unified single-instantiated view binding instance content set karenge.
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIXED: Ab runtime binding safely initialized frame reference se fragment container fetch karega
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
}