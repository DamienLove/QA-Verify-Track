package com.qa.verifyandtrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.qa.verifyandtrack.app.data.service.AdService
import com.qa.verifyandtrack.app.ui.QAApp

class MainActivity : ComponentActivity() {

    private lateinit var adService: AdService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AdMob
        adService = AdService(this)
        adService.initialize()

        setContent {
            QAApp()
        }
    }
}
