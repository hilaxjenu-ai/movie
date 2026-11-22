package com.hilax.play

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bg = Color.rgb(8, 13, 24)
        val cyan = Color.rgb(39, 215, 255)
        val white = Color.WHITE

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20)
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "HILAX PLAY"
            textSize = 27f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 4)
        }
        val subtitle = TextView(this).apply {
            text = "Your streaming player"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        }
        urlInput = EditText(this).apply {
            hint = "Paste a direct, authorized video URL"
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)
            setPadding(14)
            setBackgroundColor(Color.rgb(25, 34, 50))
        }
        val playButton = Button(this).apply {
            text = "PLAY VIDEO"
            setTextColor(bg)
            setBackgroundColor(cyan)
            setOnClickListener { playEnteredUrl() }
        }
        status = TextView(this).apply {
            text = "Paste a direct MP4/HLS stream URL to test playback."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(2, 12, 2, 12)
        }
        playerView = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            useController = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 230
            )
        }
        val note = TextView(this).apply {
            text = "Note: Automatic UpToDub page-link resolution and stream renewal are not connected yet. They require a supported/authorized playback method from the source website."
            textSize = 12f
            setTextColor(Color.rgb(180, 190, 205))
            setPadding(2, 18, 2, 0)
        }

        page.addView(title)
        page.addView(subtitle)
        page.addView(urlInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        page.addView(playButton)
        page.addView(status)
        page.addView(playerView)
        page.addView(note)
        setContentView(page)
    }

    private fun playEnteredUrl() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            status.text = "Please paste a video URL."
            return
        }
        // This starter supports direct media URLs. A movie-page URL needs a
        // source-authorized resolver before it can be passed to Media3.
        val looksLikeDirectMedia = url.contains(".mp4", true) ||
                url.contains(".m3u8", true) ||
                url.contains(".mpd", true)
        if (!looksLikeDirectMedia) {
            status.text = "This looks like a movie-page URL. Page-link resolver is not connected yet."
            return
        }

        try {
            player?.release()
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        status.text = "Playback error: ${error.errorCodeName}"
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) status.text = "Video ready."
                    }
                })
                exo.prepare()
                exo.playWhenReady = true
            }
            status.text = "Loading video…"
        } catch (e: Exception) {
            status.text = "Could not start playback: ${e.message ?: "unknown error"}"
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
