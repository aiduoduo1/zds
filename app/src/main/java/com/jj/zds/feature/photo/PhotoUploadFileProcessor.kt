package com.jj.zds.feature.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.max

object PhotoUploadFileProcessor {

    private const val MAX_COMPRESSED_SIZE = 1600
    private const val JPEG_QUALITY = 82

    fun prepareUploadFile(source: File, mode: PhotoUploadMode, cacheDir: File, prefix: String): File {
        return when (mode) {
            PhotoUploadMode.ORIGINAL -> source
            PhotoUploadMode.COMPRESSED -> compress(source, cacheDir, prefix) ?: source
        }
    }

    private fun compress(source: File, cacheDir: File, prefix: String): File? {
        if (!source.exists() || source.length() <= 0L) return null
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val maxSide = max(bounds.outWidth, bounds.outHeight)
        if (maxSide <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(maxSide)
        }
        val bitmap = BitmapFactory.decodeFile(source.absolutePath, options) ?: return null
        val outputDir = File(cacheDir, "compressed_upload").apply { mkdirs() }
        val output = File(outputDir, "${prefix}_${source.nameWithoutExtension}.jpg")
        return runCatching {
            output.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            bitmap.recycle()
            output.takeIf { it.exists() && it.length() > 0L }
        }.getOrNull()
    }

    private fun calculateInSampleSize(maxSide: Int): Int {
        var sampleSize = 1
        while (maxSide / sampleSize > MAX_COMPRESSED_SIZE) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}
