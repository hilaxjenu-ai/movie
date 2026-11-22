package com.hilax.play

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var root: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var videoContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var urlInput: EditText
    private lateinit var fullscreenButton: Button

    private var isFullscreen = false

    private val bg = Color.rgb(7, 12, 23)
    private val panel = Color.rgb(19, 29, 45)
    private val cyan = Color.rgb(39, 215, 255)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        buildUI()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isFullscreen) {
                        exitFullscreen()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun buildUI() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(16, 12, 16, 12)
        }

        header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "HILAX PLAY"
            textSize = 28f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 2)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "YOUR MOVIE EXPERIENCE"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        }

        val linkLabel = TextView(this).apply {
            text = "MOVIE LINK"
            textSize = 12f
            setTextColor(cyan)
            setPadding(2, 4, 2, 8)
        }

        urlInput = EditText(this).apply {
            hint = "Paste your video URL here"
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            singleLine = true
            setPadding(14, 12, 14, 12)
            setBackgroundColor(panel)
        }

        val playButton = Button(this).apply {
            text = "▶  PLAY VIDEO"
            textSize = 14f
            setTextColor(bg)
            setBackgroundColor(cyan)
            setOnClickListener { playVideo() }
        }

        status = TextView(this).apply {
            text = "Ready to play"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(2, 12, 2, 12)
        }

        header.addView(title)
        header.addView(subtitle)
        header.addView(linkLabel)
        header.addView(urlInput)

        header.addView(
            playButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 52
            ).apply {
                topMargin = 12
            }
        )

        header.addView(status)
        videoContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setKeepContentOnPlayerReset(true)
            setBackgroundColor(Color.BLACK)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        fullscreenButton = Button(this).apply {
            text = "⛶"
            textSize = 23f
            setTextColor(white)
            setBackgroundColor(Color.rgb(30, 43, 60))
            setPadding(0, 0, 0, 0)
            setOnClickListener { toggleFullscreen() }
        }

        videoContainer.addView(playerView)

        val fullParams = FrameLayout.LayoutParams(54, 48).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(0, 0, 12, 12)
        }

        videoContainer.addView(fullscreenButton, fullParams)

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            videoContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                230
            )
        )

        setContentView(root)
    }

    private fun playVideo() {
        val url = urlInput.text.toString().trim()

        if (!url.startsWith("https://") &&
            !url.startsWith("http://")) {
            status.text = "Please enter a valid URL"
            return
        }

        if (!url.contains(".mp4", true) &&
            !url.contains(".m3u8", true) &&
            !url.contains(".mpd", true)) {
            status.text =
                "Direct video URL required. Page-link resolver is not connected."
            return
        }

        try {
            player?.release()

            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo

                exo.addListener(object : Player.Listener {

                    override fun onPlaybackStateChanged(state: Int) {
                        status.text = when (state) {
                            Player.STATE_BUFFERING -> "Buffering..."
                            Player.STATE_READY -> "Video ready"
                            Player.STATE_ENDED -> "Playback finished"
                            else -> "Ready"
                        }
                    }

                    override fun onPlayerError(
                        error: androidx.media3.common.PlaybackException
                    ) {
                        status.text =
                            "Playback error: ${error.errorCodeName}"
                    }
                })

                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }

            status.text = "Loading video..."

        } catch (e: Exception) {
            status.text =
                "Unable to play: ${e.message ?: "Unknown error"}"
        }
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true

        header.visibility = View.GONE

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        val params =
            videoContainer.layoutParams as LinearLayout.LayoutParams
        params.height = 0
        params.weight = 1f
        videoContainer.layoutParams = params

        fullscreenButton.text = "✕"
    }

    private fun exitFullscreen() {
        isFullscreen = false

        header.visibility = View.VISIBLE

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        val params =
            videoContainer.layoutParams as LinearLayout.LayoutParams

        params.height = 230
        params.weight = 0f
        videoContainer.layoutParams = params

        fullscreenButton.text = "⛶"
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
