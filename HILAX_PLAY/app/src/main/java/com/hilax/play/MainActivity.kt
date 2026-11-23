package com.hilax.play

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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

    // Playback state kept across stop/start and rotation
    private var currentUrl: String? = null
    private var resumePosition = 0L
    private var resumePlayWhenReady = true

    private val prefs by lazy { getSharedPreferences("hilax_play", Context.MODE_PRIVATE) }

    private val bg = Color.rgb(7, 12, 23)
    private val panel = Color.rgb(19, 29, 45)
    private val cyan = Color.rgb(39, 215, 255)
    private val white = Color.WHITE

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int = 10) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    // 16:9 player height in portrait, capped so the header is always visible
    private fun portraitVideoHeight(): Int {
        val m = resources.displayMetrics
        val byWidth = (m.widthPixels - dp(32)) * 9 / 16
        return minOf(byWidth, m.heightPixels / 2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        buildUI()

        // Restore state (rotation / process recreation)
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString(KEY_URL)
            resumePosition = savedInstanceState.getLong(KEY_POS, 0L)
            resumePlayWhenReady = savedInstanceState.getBoolean(KEY_PLAY, true)
            if (savedInstanceState.getBoolean(KEY_FULL, false)) applyFullscreen(true)
        }

        // Last used link
        if (currentUrl == null) {
            urlInput.setText(prefs.getString(KEY_LAST_URL, "") ?: "")
        } else {
            urlInput.setText(currentUrl)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) applyFullscreen(false) else finish()
            }
        })

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** Lets other apps / browsers open a video link directly in HILAX PLAY. */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.toString()?.let {
                urlInput.setText(it)
                playVideo()
            }
        }
    }

    private fun buildUI() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val title = TextView(this).apply {
            text = "HILAX PLAY"
            textSize = 28f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "YOUR MOVIE EXPERIENCE"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }

        val linkLabel = TextView(this).apply {
            text = "MOVIE LINK"
            textSize = 12f
            setTextColor(cyan)
            setPadding(dp(2), dp(4), dp(2), dp(6))
        }

        urlInput = EditText(this).apply {
            hint = "Paste your video URL here"
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    playVideo()
                    true
                } else false
            }
        }

        val pasteButton = Button(this).apply {
            text = "PASTE"
            textSize = 12f
            isAllCaps = false
            setTextColor(white)
            background = rounded(Color.rgb(30, 43, 60))
            setOnClickListener { pasteFromClipboard() }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(urlInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(
                pasteButton,
                LinearLayout.LayoutParams(dp(76), dp(44)).apply { marginStart = dp(8) }
            )
        }

        val playButton = Button(this).apply {
            text = "▶  PLAY VIDEO"
            textSize = 14f
            setTextColor(this@MainActivity.bg)
            background = rounded(this@MainActivity.cyan)
            setOnClickListener { playVideo() }
        }

        status = TextView(this).apply {
            text = "Ready to play"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(dp(2), dp(10), dp(2), dp(10))
        }

        header.addView(title)
        header.addView(subtitle)
        header.addView(linkLabel)
        header.addView(inputRow)
        header.addView(
            playButton,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(12)
            }
        )
        header.addView(status)

        videoContainer = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

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
            textSize = 20f
            setTextColor(white)
            background = rounded(Color.argb(170, 30, 43, 60), 8)
            setPadding(0, 0, 0, 0)
            contentDescription = "Toggle fullscreen"
            setOnClickListener { applyFullscreen(!isFullscreen) }
        }

        videoContainer.addView(playerView)
        videoContainer.addView(
            fullscreenButton,
            FrameLayout.LayoutParams(dp(44), dp(40)).apply {
                gravity = Gravity.END or Gravity.TOP
                setMargins(0, dp(8), dp(8), 0)
            }
        )

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
                portraitVideoHeight()
            )
        )

        setContentView(root)
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            status.text = "Clipboard is empty"
        } else {
            urlInput.setText(text)
            urlInput.setSelection(text.length)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
        urlInput.clearFocus()
    }

    // ---------------------------------------------------------------- Playback

    private fun playVideo() {
        val url = urlInput.text.toString().trim()

        val validScheme = url.startsWith("https://", true) || url.startsWith("http://", true)
        if (!validScheme || !Patterns.WEB_URL.matcher(url).matches()) {
            status.text = "Please enter a valid http/https URL"
            return
        }

        hideKeyboard()
        prefs.edit().putString(KEY_LAST_URL, url).apply()

        currentUrl = url
        resumePosition = 0L
        resumePlayWhenReady = true

        releasePlayer(saveState = false)
        initPlayer()
    }

    private fun initPlayer() {
        val url = currentUrl ?: return
        if (player != null) return

        status.text = "Loading video..."

        try {
            val exo = ExoPlayer.Builder(this).build()
            player = exo
            playerView.player = exo

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    status.text = when (state) {
                        Player.STATE_BUFFERING -> "Buffering..."
                        Player.STATE_READY -> "Playing"
                        Player.STATE_ENDED -> "Playback finished"
                        else -> status.text
                    }
                    updateKeepScreenOn()
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    updateKeepScreenOn()
                }

                override fun onPlayerError(error: PlaybackException) {
                    status.text = friendlyError(error)
                    updateKeepScreenOn()
                }
            })

            exo.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            if (resumePosition > 0) exo.seekTo(resumePosition)
            exo.playWhenReady = resumePlayWhenReady
            exo.prepare()
        } catch (e: Exception) {
            status.text = "Unable to play: ${e.message ?: "Unknown error"}"
        }
    }

    private fun friendlyError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Network problem. Check your internet and try again."
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            "This http link is blocked. Use an https link."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "Server refused the video (link expired or not found)."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
            "This link is not a direct video file/stream (mp4, m3u8, mpd...)."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "This video format is not supported on your device."
        else -> "Playback error: ${error.errorCodeName}"
    }

    private fun releasePlayer(saveState: Boolean) {
        player?.let {
            if (saveState) {
                resumePosition = it.currentPosition
                resumePlayWhenReady = it.playWhenReady
            }
            it.release()
        }
        player = null
        playerView.player = null
        updateKeepScreenOn()
    }

    /**
     * Screen stays on while the video is playing or buffering (user pressed play),
     * and is released when paused, ended, errored or the player is gone.
     */
    private fun updateKeepScreenOn() {
        val p = player
        val keepOn = p != null &&
            p.playWhenReady &&
            p.playbackState != Player.STATE_IDLE &&
            p.playbackState != Player.STATE_ENDED
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ------------------------------------------------------------- Lifecycle

    override fun onStart() {
        super.onStart()
        initPlayer() // no-op if no URL yet
    }

    override fun onStop() {
        // Release when invisible: frees decoders/battery, position is kept
        releasePlayer(saveState = true)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            resumePosition = it.currentPosition
            resumePlayWhenReady = it.playWhenReady
        }
        outState.putString(KEY_URL, currentUrl)
        outState.putLong(KEY_POS, resumePosition)
        outState.putBoolean(KEY_PLAY, resumePlayWhenReady)
        outState.putBoolean(KEY_FULL, isFullscreen)
    }

    override fun onDestroy() {
        releasePlayer(saveState = false)
        super.onDestroy()
    }

    // ------------------------------------------------------------ Fullscreen

    private fun applyFullscreen(enable: Boolean) {
        isFullscreen = enable

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val params = videoContainer.layoutParams as LinearLayout.LayoutParams

        if (enable) {
            header.visibility = View.GONE
            root.setPadding(0, 0, 0, 0)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            params.height = 0
            params.weight = 1f
            fullscreenButton.text = "✕"
        } else {
            header.visibility = View.VISIBLE
            root.setPadding(dp(16), dp(12), dp(16), dp(12))
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            controller.show(WindowInsetsCompat.Type.systemBars())

            params.height = portraitVideoHeight()
            params.weight = 0f
            fullscreenButton.text = "⛶"
        }
        videoContainer.layoutParams = params
    }

    private companion object {
        const val KEY_URL = "url"
        const val KEY_POS = "pos"
        const val KEY_PLAY = "play"
        const val KEY_FULL = "full"
        const val KEY_LAST_URL = "last_url"
    }
}
