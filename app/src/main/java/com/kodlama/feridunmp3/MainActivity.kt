package com.kodlama.feridunmp3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TrackAdapter
    private lateinit var list: RecyclerView
    private lateinit var emptyPanel: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var emptyButton: Button
    private lateinit var nowTitle: TextView
    private lateinit var nowArtist: TextView
    private lateinit var seek: SeekBar
    private lateinit var btnPlay: ImageButton
    private lateinit var toolbar: Toolbar

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() =
        controllerFuture?.takeIf { it.isDone && !it.isCancelled }?.let {
            try { it.get() } catch (e: Exception) { null }
        }

    private var tracks: List<MediaItem> = emptyList()
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private var userSeeking = false
    private var permissionAskedOnce = false
    private var crashMode = false

    private val ticker = object : Runnable {
        override fun run() {
            updateProgress()
            if (controller?.isPlaying == true) ui.postDelayed(this, 500)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayIcon()
            ui.removeCallbacks(ticker)
            if (isPlaying) ui.post(ticker) else updateProgress()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateNowPlaying()
        override fun onPlaybackStateChanged(playbackState: Int) = updateProgress()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (hasAudioPermission()) loadTracks() else showPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crash = java.io.File(filesDir, App.CRASH_FILE)
        if (crash.exists()) {
            val text = crash.readText()
            crash.delete()
            val tv = TextView(this).apply {
                this.text = text
                textSize = 11f
                setTextIsSelectable(true)
                setPadding(24, 24, 24, 24)
            }
            crashMode = true
            setContentView(android.widget.ScrollView(this).apply { addView(tv) })
            return
        }
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        list = findViewById(R.id.list)
        emptyPanel = findViewById(R.id.empty_panel)
        emptyText = findViewById(R.id.empty_text)
        emptyButton = findViewById(R.id.empty_button)
        nowTitle = findViewById(R.id.now_title)
        nowArtist = findViewById(R.id.now_artist)
        seek = findViewById(R.id.seek)
        btnPlay = findViewById(R.id.btn_play)

        adapter = TrackAdapter { index -> playAt(index) }
        list.layoutManager = LinearLayoutManager(this)
        list.setHasFixedSize(true)
        list.adapter = adapter

        btnPlay.setOnClickListener {
            val c = controller ?: return@setOnClickListener
            if (c.isPlaying) c.pause() else if (c.mediaItemCount > 0) c.play()
            else if (tracks.isNotEmpty()) playAt(0)
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener { controller?.seekToNextMediaItem() }
        findViewById<ImageButton>(R.id.btn_prev).setOnClickListener { controller?.seekToPreviousMediaItem() }

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                userSeeking = false
                controller?.seekTo((sb?.progress ?: 0).toLong())
            }
        })

        if (hasAudioPermission()) loadTracks() else requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        if (crashMode) return
        val token = SessionToken(this, android.content.ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = controller ?: return@addListener
            c.addListener(playerListener)
            updateNowPlaying()
            updatePlayIcon()
            if (c.isPlaying) ui.post(ticker) else updateProgress()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        if (crashMode) { super.onStop(); return }
        ui.removeCallbacks(ticker)
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        R.id.action_refresh -> {
            if (hasAudioPermission()) loadTracks() else requestPermissions(); true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------- İzinler ----------

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasAudioPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        permissionAskedOnce = true
        permissionLauncher.launch(requiredPermissions())
    }

    private fun showPermissionState() {
        list.visibility = View.GONE
        emptyPanel.visibility = View.VISIBLE
        val canAskAgain = !permissionAskedOnce || shouldShowRequestPermissionRationale(
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (canAskAgain) {
            emptyText.setText(R.string.permission_needed)
            emptyButton.setText(R.string.grant_permission)
            emptyButton.setOnClickListener { requestPermissions() }
        } else {
            emptyText.setText(R.string.permission_denied_settings)
            emptyButton.setText(R.string.open_app_settings)
            emptyButton.setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            }
        }
    }

    // ---------- Liste ----------

    private fun loadTracks() {
        io.execute {
            val result = try { TrackRepository.load(applicationContext) } catch (e: Exception) { emptyList() }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                tracks = result
                adapter.submit(result)
                adapter.setCurrent(controller?.currentMediaItem?.mediaId)
                if (result.isEmpty()) {
                    list.visibility = View.GONE
                    emptyPanel.visibility = View.VISIBLE
                    emptyText.setText(R.string.no_music)
                    emptyButton.setText(R.string.refresh)
                    emptyButton.setOnClickListener { loadTracks() }
                } else {
                    emptyPanel.visibility = View.GONE
                    list.visibility = View.VISIBLE
                    toolbar.subtitle = getString(R.string.song_count, result.size)
                }
            }
        }
    }

    private fun playAt(index: Int) {
        val c = controller ?: return
        c.setMediaItems(tracks, index, 0L)
        c.prepare()
        c.play()
    }

    // ---------- Oynatıcı arayüzü ----------

    private fun updateNowPlaying() {
        val item = controller?.currentMediaItem
        nowTitle.text = item?.mediaMetadata?.title ?: getString(R.string.nothing_playing)
        nowArtist.text = item?.mediaMetadata?.artist ?: ""
        adapter.setCurrent(item?.mediaId)
        updateProgress()
    }

    private fun updatePlayIcon() {
        val playing = controller?.isPlaying == true
        btnPlay.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        btnPlay.contentDescription = getString(if (playing) R.string.pause else R.string.play)
    }

    private fun updateProgress() {
        val c = controller ?: return
        if (userSeeking) return
        val dur = c.duration
        if (dur > 0) {
            seek.max = dur.toInt()
            seek.progress = c.currentPosition.toInt()
        } else {
            seek.max = 0
            seek.progress = 0
        }
    }
}
