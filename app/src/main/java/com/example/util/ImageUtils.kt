package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageUtils {
    /**
     * Compresses an image URI into a compact base64 JPEG data string (~20-60KB)
     * so product & store photos sync reliably across devices via Cloud Firestore.
     */
    fun compressUriToDataUri(context: Context, uri: Uri, maxDim: Int = 360, quality: Int = 72): String? {
        return try {
            val resolver = context.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while (origW / sampleSize > maxDim * 2 || origH / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scale = maxDim.toFloat() / max(decoded.width, decoded.height).toFloat()
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            scaled.recycle()
            val bytes = baos.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUriToBitmap(dataUri: String): Bitmap? {
        if (dataUri.isBlank()) return null
        return try {
            val base64Part = if (dataUri.contains(",")) dataUri.substringAfter(",") else dataUri
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun createCameraOutputUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
