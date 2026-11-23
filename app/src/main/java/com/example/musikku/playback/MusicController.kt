package com.example.musikku.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import android.net.Uri
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * State pemutaran lengkap yang diobservasi oleh Mini Player, Now Playing, dan Antrean.
 */
data class PlaybackState(
    val currentSong: SongEntity? = null,
    val queue: List<SongEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF, // 0: OFF, 1: ONE, 2: ALL
    val sleepTimerRemainingSeconds: Long = 0L
)

/**
 * Pengontrol pemutaran musik di sisi klien (UI / ViewModel).
 * Mengelola MediaController, antrean lagu, shuffle, repeat, seek, sleep timer, dan deteksi error.
 */
class MusicController(
    private val context: Context,
    private val listeningStatsRepository: com.example.musikku.data.repository.ListeningStatsRepository? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _userMessageEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessageEvent: SharedFlow<String> = _userMessageEvent.asSharedFlow()

    fun postUserMessage(message: String) {
        _userMessageEvent.tryEmit(message)
    }

    private var currentPlaylist: List<SongEntity> = emptyList()
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var preResolveJob: Job? = null

    /**
     * Callback untuk menyelesaikan stream URL lagu (misal dari Spotify atau link YouTube kedaluwarsa)
     * sebelum atau saat pemutaran.
     */
    var onResolveSongStream: (suspend (SongEntity) -> SongEntity?)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressLoop()
            } else {
                stopProgressLoop()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val controller = mediaController
            val songId = mediaItem?.mediaId?.toLongOrNull()
            val foundIndex = if (songId != null) currentPlaylist.indexOfFirst { it.id == songId } else -1
            val index = if (foundIndex >= 0) foundIndex else {
                val next = _playbackState.value.currentIndex + 1
                if (next in currentPlaylist.indices) next else 0
            }
            val song = currentPlaylist.getOrNull(index) ?: currentPlaylist.firstOrNull { it.id == songId }

            _playbackState.update {
                it.copy(
                    currentSong = song,
                    currentIndex = index,
                    currentPosition = 0L,
                    duration = song?.duration ?: 0L
                )
            }
            if (index >= 0) {
                preResolveNextSong(index)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncStateWithController()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateWithController()
            if (playbackState == Player.STATE_ENDED) {
                val nextIdx = _playbackState.value.currentIndex + 1
                if (nextIdx in currentPlaylist.indices) {
                    playQueueItem(nextIdx)
                } else if (_playbackState.value.repeatMode == Player.REPEAT_MODE_ALL && currentPlaylist.isNotEmpty()) {
                    playQueueItem(0)
                } else {
                    _playbackState.update { it.copy(isPlaying = false) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("MusicController", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode}), msg=${error.message}", error)
            val current = _playbackState.value.currentSong
            val msg = if (current != null) {
                when {
                    current.source == "YOUTUBE" -> {
                        "Lagu '${current.title}' dari YouTube tidak dapat diputar: ${error.errorCodeName} (${error.message ?: "Koneksi bermasalah"})."
                    }
                    current.source == "JAMENDO" || current.contentUri.startsWith("http") -> {
                        "Gagal memutar streaming online '${current.title}'. Periksa koneksi internet Anda."
                    }
                    else -> {
                        "Lagu '${current.title}' tidak dapat diputar (file telah dihapus dari HP atau rusak)."
                    }
                }
            } else {
                "Gagal memutar lagu: file tidak ditemukan atau jaringan bermasalah."
            }
            _userMessageEvent.tryEmit(msg)
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(playerListener)
                    syncStateWithController()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Putar daftar lagu mulai dari indeks tertentu sebagai antrean baru.
     */
    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty() || startIndex !in songs.indices) return
        currentPlaylist = songs

        val targetSong = songs[startIndex]
        if (isPlayableDirectly(targetSong)) {
            val controller = mediaController
            if (controller != null) {
                executePlay(controller, songs, startIndex)
            } else {
                controllerFuture?.addListener({
                    mediaController?.let { c ->
                        executePlay(c, songs, startIndex)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } else {
            // Lagu target memerlukan resolusi stream (misal link Spotify atau YouTube expired)
            _playbackState.update {
                it.copy(
                    currentSong = targetSong,
                    queue = songs,
                    currentIndex = startIndex,
                    isPlaying = false
                )
            }
            scope.launch {
                val resolved = onResolveSongStream?.invoke(targetSong)
                if (resolved != null && isPlayableDirectly(resolved)) {
                    val updated = currentPlaylist.map { if (it.id == targetSong.id) resolved else it }
                    currentPlaylist = updated
                    val controller = mediaController
                    if (controller != null) {
                        executePlay(controller, updated, startIndex)
                    } else {
                        controllerFuture?.addListener({
                            mediaController?.let { c ->
                                executePlay(c, updated, startIndex)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                } else {
                    _userMessageEvent.tryEmit("Audio untuk '${targetSong.title}' tidak dapat dimuat dari YouTube Music.")
                }
            }
        }
    }

    private fun executePlay(controller: MediaController, songs: List<SongEntity>, startIndex: Int) {
        val song = songs[startIndex]
        if (!isFileAvailable(song)) {
            val notFoundMsg = if (song.source == "YOUTUBE" || song.source == "JAMENDO" || song.contentUri.startsWith("http")) {
                "Lagu '${song.title}' tidak dapat dimuat dari internet."
            } else {
                "Lagu '${song.title}' tidak ditemukan di HP (file telah dihapus)."
            }
            _userMessageEvent.tryEmit(notFoundMsg)
            return
        }

        // Cek apakah ada lagu yang belum siap diputar (misal Spotify URI belum di-resolve)
        // JANGAN pernah kirim URI 'spotify:...' ke ExoPlayer agar tidak memicu 'unknown protocol: spotify'
        val hasUnresolved = songs.any { !isPlayableDirectly(it) }
        val mediaItems = if (hasUnresolved) {
            listOf(song.toMediaItem())
        } else {
            songs.map { it.toMediaItem() }
        }
        val playerStartIndex = if (hasUnresolved) 0 else startIndex

        controller.setMediaItems(mediaItems, playerStartIndex, 0L)
        controller.prepare()
        controller.play()
        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = songs,
                currentIndex = startIndex,
                isPlaying = true,
                currentPosition = 0L,
                duration = song.duration,
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }

        preResolveNextSong(startIndex)
    }

    private fun preResolveNextSong(currentIndex: Int) {
        preResolveJob?.cancel()
        val nextIndex = currentIndex + 1
        if (nextIndex !in currentPlaylist.indices) return
        val nextSong = currentPlaylist[nextIndex]

        if (isPlayableDirectly(nextSong)) {
            val controller = mediaController ?: return
            val currentExoCount = controller.mediaItemCount
            val currentExoIndex = controller.currentMediaItemIndex
            if (currentExoCount <= currentExoIndex + 1) {
                controller.addMediaItem(nextSong.toMediaItem())
            }
            return
        }

        preResolveJob = scope.launch {
            try {
                val resolved = onResolveSongStream?.invoke(nextSong)
                if (resolved != null && isPlayableDirectly(resolved)) {
                    currentPlaylist = currentPlaylist.map { if (it.id == nextSong.id) resolved else it }
                    _playbackState.update { it.copy(queue = currentPlaylist) }
                    val controller = mediaController ?: return@launch
                    val currentExoCount = controller.mediaItemCount
                    val currentExoIndex = controller.currentMediaItemIndex
                    if (currentExoCount <= currentExoIndex + 1) {
                        controller.addMediaItem(resolved.toMediaItem())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicController", "Pre-resolve gagal untuk '${nextSong.title}': ${e.message}")
            }
        }
    }

    /** Putar item tertentu dari antrean saat ini berdasarkan indeks. */
    fun playQueueItem(index: Int) {
        if (index !in currentPlaylist.indices) return
        val song = currentPlaylist[index]

        if (isPlayableDirectly(song)) {
            val controller = mediaController ?: return
            var playerItemIndex = -1
            for (i in 0 until controller.mediaItemCount) {
                if (controller.getMediaItemAt(i).mediaId == song.id.toString()) {
                    playerItemIndex = i
                    break
                }
            }
            if (playerItemIndex >= 0) {
                controller.seekToDefaultPosition(playerItemIndex)
                controller.play()
                _playbackState.update {
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        isPlaying = true
                    )
                }
                preResolveNextSong(index)
            } else {
                executePlay(controller, currentPlaylist, index)
            }
        } else {
            _playbackState.update {
                it.copy(
                    currentSong = song,
                    currentIndex = index,
                    isPlaying = false
                )
            }
            scope.launch {
                val resolved = onResolveSongStream?.invoke(song)
                if (resolved != null && isPlayableDirectly(resolved)) {
                    currentPlaylist = currentPlaylist.map { if (it.id == song.id) resolved else it }
                    val controller = mediaController ?: return@launch
                    executePlay(controller, currentPlaylist, index)
                } else {
                    _userMessageEvent.tryEmit("Audio untuk '${song.title}' tidak dapat dimuat.")
                }
            }
        }
    }

    /** Hapus item dari antrean. */
    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in currentPlaylist.indices) {
            controller.removeMediaItem(index)
            val updated = currentPlaylist.toMutableList().apply { removeAt(index) }
            currentPlaylist = updated
            val newIndex = controller.currentMediaItemIndex
            val newSong = updated.getOrNull(newIndex)
            _playbackState.update {
                it.copy(
                    queue = updated,
                    currentIndex = newIndex,
                    currentSong = newSong
                )
            }
        }
    }

    /** Ubah urutan item di antrean pemutaran. */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in currentPlaylist.indices && toIndex in currentPlaylist.indices && fromIndex != toIndex) {
            controller.moveMediaItem(fromIndex, toIndex)
            val updated = currentPlaylist.toMutableList()
            val moved = updated.removeAt(fromIndex)
            updated.add(toIndex, moved)
            currentPlaylist = updated
            val newIndex = controller.currentMediaItemIndex
            val newSong = updated.getOrNull(newIndex)
            _playbackState.update {
                it.copy(
                    queue = updated,
                    currentIndex = newIndex,
                    currentSong = newSong
                )
            }
        }
    }

    /** Tambahkan lagu ke akhir antrean pemutaran. */
    fun addToQueue(song: SongEntity) {
        val controller = mediaController ?: return
        controller.addMediaItem(song.toMediaItem())
        val updated = currentPlaylist + song
        currentPlaylist = updated
        _playbackState.update { it.copy(queue = updated) }
    }

    /** Masukkan lagu agar diputar tepat setelah lagu yang sedang aktif. */
    fun playNext(song: SongEntity) {
        val controller = mediaController ?: return
        val nextIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(currentPlaylist.size)
        controller.addMediaItem(nextIndex, song.toMediaItem())
        val updated = currentPlaylist.toMutableList().apply { add(nextIndex, song) }
        currentPlaylist = updated
        _playbackState.update { it.copy(queue = updated) }
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun skipToNext() {
        val nextIndex = _playbackState.value.currentIndex + 1
        if (nextIndex in currentPlaylist.indices) {
            playQueueItem(nextIndex)
        } else if (_playbackState.value.repeatMode == Player.REPEAT_MODE_ALL && currentPlaylist.isNotEmpty()) {
            playQueueItem(0)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        if (controller != null && controller.currentPosition > 3_000) {
            controller.seekTo(0)
            return
        }
        val prevIndex = _playbackState.value.currentIndex - 1
        if (prevIndex in currentPlaylist.indices) {
            playQueueItem(prevIndex)
        } else if (currentPlaylist.isNotEmpty()) {
            controller?.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(isShuffleEnabled = newShuffle) }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        // Rotasi: OFF -> ALL -> ONE -> OFF
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    private fun syncStateWithController() {
        val controller = mediaController ?: return
        val index = controller.currentMediaItemIndex
        val currentId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        val song = currentPlaylist.getOrNull(index)
            ?: currentPlaylist.firstOrNull { it.id == currentId }
            ?: _playbackState.value.currentSong
        val isPlaying = controller.isPlaying
        val duration = if (controller.duration > 0) controller.duration else (song?.duration ?: 0L)
        val position = controller.currentPosition.coerceAtLeast(0L)

        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = currentPlaylist,
                currentIndex = index,
                isPlaying = isPlaying,
                currentPosition = position,
                duration = duration,
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
        if (isPlaying) {
            startProgressLoop()
        }
    }

    private var accumulatedListeningMs = 0L

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = if (controller.duration > 0) controller.duration else (_playbackState.value.duration)
                    _playbackState.update {
                        it.copy(
                            currentPosition = pos,
                            duration = dur
                        )
                    }

                    // Rekam statistik waktu mendengarkan musik secara real-time setiap detik
                    accumulatedListeningMs += 500L
                    if (accumulatedListeningMs >= 1000L) {
                        val secondsToAdd = accumulatedListeningMs / 1000L
                        accumulatedListeningMs %= 1000L
                        listeningStatsRepository?.addListeningSeconds(secondsToAdd)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        if (accumulatedListeningMs >= 1000L) {
            val secondsToAdd = accumulatedListeningMs / 1000L
            accumulatedListeningMs %= 1000L
            listeningStatsRepository?.addListeningSeconds(secondsToAdd)
        }
        mediaController?.let { controller ->
            _playbackState.update {
                it.copy(currentPosition = controller.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    /**
     * Memulai Sleep Timer untuk mematikan pemutaran otomatis setelah [minutes] menit.
     * Jika [minutes] <= 0, timer akan dibatalkan.
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerRemainingSeconds = 0L) }
            return
        }

        sleepTimerJob = scope.launch {
            var remaining = minutes * 60L
            while (remaining > 0) {
                _playbackState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
                delay(1000L)
                remaining--
            }
            _playbackState.update { it.copy(sleepTimerRemainingSeconds = 0L) }
            pause()
            _userMessageEvent.emit("Sleep timer selesai. Musik dihentikan otomatis.")
        }
    }

    /** Membatalkan Sleep Timer yang sedang berjalan. */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.update { it.copy(sleepTimerRemainingSeconds = 0L) }
    }

    /**
     * Memeriksa apakah lagu siap diputar langsung oleh ExoPlayer tanpa memerlukan resolusi URL baru.
     */
    fun isPlayableDirectly(song: SongEntity): Boolean {
        if (song.source == "LOCAL" || song.source == "DOWNLOADED") {
            return isFileAvailable(song)
        }
        if (song.source == "JAMENDO" && (song.contentUri.startsWith("http://") || song.contentUri.startsWith("https://"))) {
            return true
        }
        if (song.source == "YOUTUBE") {
            if (song.contentUri.startsWith("spotify:") || song.contentUri.isBlank()) {
                return false
            }
            val isExpired = try {
                val exp = Uri.parse(song.contentUri).getQueryParameter("expire")?.toLongOrNull()
                exp != null && (System.currentTimeMillis() / 1000L) >= (exp - 60)
            } catch (_: Exception) {
                false
            }
            return !isExpired && (song.contentUri.startsWith("http://") || song.contentUri.startsWith("https://"))
        }
        return false
    }

    /**
     * Memeriksa apakah file audio lokal atau stream masih tersedia.
     */
    fun isFileAvailable(song: SongEntity): Boolean {
        // Lagu online (YouTube / Jamendo) yang dialirkan langsung dari internet
        if (song.source == "YOUTUBE" || song.source == "JAMENDO" || song.contentUri.startsWith("http://") || song.contentUri.startsWith("https://")) {
            return song.contentUri.isNotBlank() || !song.filePath.isNullOrBlank()
        }

        // Lagu unduhan atau file lokal yang memiliki file path fisik di penyimpanan perangkat
        if (song.source == "DOWNLOADED" || (!song.filePath.isNullOrBlank() && song.filePath.startsWith("/"))) {
            val f = File(song.filePath.orEmpty())
            if (!f.exists()) return false
        }

        // Lagu lokal MediaStore (content://...)
        if (song.source == "LOCAL" && song.contentUri.startsWith("content://")) {
            return try {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(song.contentUri), "r")?.use { true } ?: false
            } catch (e: Exception) {
                false
            }
        }
        return true
    }

    /** Update cover album untuk lagu yang sedang aktif maupun di antrean. */
    fun updateSongArtwork(songId: Long, newAlbumArtUri: String) {
        currentPlaylist = currentPlaylist.map {
            if (it.id == songId) it.copy(albumArtUri = newAlbumArtUri) else it
        }
        _playbackState.update { state ->
            val updatedSong = if (state.currentSong?.id == songId) {
                state.currentSong.copy(albumArtUri = newAlbumArtUri)
            } else {
                state.currentSong
            }
            state.copy(
                currentSong = updatedSong,
                queue = currentPlaylist
            )
        }
    }

    /**
     * Dijalankan saat lagu dihapus dari perangkat.
     * Menghapus lagu dari antrean pemutaran dan menghentikan musik jika lagu yang dihapus sedang diputar.
     */
    fun onSongDeleted(songId: Long) {
        val state = _playbackState.value
        val isCurrentPlaying = state.currentSong?.id == songId
        val index = currentPlaylist.indexOfFirst { it.id == songId }
        if (index != -1) {
            removeQueueItem(index)
        }
        if (isCurrentPlaying) {
            if (currentPlaylist.isEmpty()) {
                mediaController?.stop()
                mediaController?.clearMediaItems()
                _playbackState.update {
                    it.copy(
                        currentSong = null,
                        currentIndex = -1,
                        isPlaying = false,
                        currentPosition = 0L,
                        duration = 0L,
                        queue = emptyList()
                    )
                }
            }
        }
    }

    fun release() {
        sleepTimerJob?.cancel()
        progressJob?.cancel()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
    }
}

/** Alias PlayerController untuk keselarasan dengan penamaan kontrak spesifikasi */
typealias PlayerController = MusicController
