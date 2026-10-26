package com.aslanbek.fitclub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aslanbek.fitclub.ui.FitClubApp
import com.aslanbek.fitclub.ui.theme.FitClubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate, restored = ${savedInstanceState != null}")
        enableEdgeToEdge()
        setContent {
            FitClubTheme {
                FitClubApp()
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy, isChangingConfigurations = $isChangingConfigurations")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
