package com.example.musikku.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BitmapLoader bertenaga Coil untuk MediaSession dan notifikasi sistem Android.
 * Mendukung pemuatan gambar resolusi tinggi dari berbagai skema URI:
 * file:// (penyimpanan lokal), content:// (MediaStore), https:// (YouTube & Jamendo).
 * Mengharuskan allowHardware(false) agar kompatibel dengan RemoteViews dan IPC SystemUI.
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : BitmapLoader {

    private val imageLoader = ImageLoader.Builder(context)
        .allowHardware(false)
        .build()

    override fun supportsMimeType(mimeType: String): Boolean {
        val type = mimeType.lowercase()
        return type.startsWith("image/") || type.contains("jpeg") || type.contains("png") || type.contains("webp")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        coroutineScope.launch {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    future.set(bitmap)
                } else {
                    future.setException(IllegalArgumentException("Gagal mendecode byte array ke Bitmap"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        if (uri == Uri.EMPTY || uri.toString().isBlank()) {
            future.setException(IllegalArgumentException("Uri gambar kosong"))
            return future
        }

        coroutineScope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .size(512, 512)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    future.set(bitmap)
                } else {
                    future.setException(IllegalStateException("Coil tidak dapat memuat gambar dari $uri"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val artworkData = metadata.artworkData
        if (artworkData != null && artworkData.isNotEmpty()) {
            return decodeBitmap(artworkData)
        }
        val artworkUri = metadata.artworkUri
        if (artworkUri != null && artworkUri != Uri.EMPTY && artworkUri.toString().isNotBlank()) {
            return loadBitmap(artworkUri)
        }
        return null
    }
}
