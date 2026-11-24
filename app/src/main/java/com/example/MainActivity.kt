package com.example

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.remote.QuranRepository
import com.example.player.QuranPlayerController
import com.example.ui.NurAppRoot

class MainActivity : ComponentActivity() {

    private lateinit var quranRepository: QuranRepository
    private lateinit var playerController: QuranPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        quranRepository = QuranRepository()
        playerController = QuranPlayerController(applicationContext, quranRepository)

        setContent {
            NurAppRoot(
                quranRepository = quranRepository,
                playerController = playerController
            )
        }
    }

    override fun onDestroy() {
        if (::playerController.isInitialized) {
            playerController.release()
        }
        super.onDestroy()
    }
}
