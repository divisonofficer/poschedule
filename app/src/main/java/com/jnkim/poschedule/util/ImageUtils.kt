package com.jnkim.poschedule.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val TARGET_SIZE = 384
    private const val JPEG_QUALITY = 90

    /**
     * Resize and encode image to Base64 for Gemini API.
     * Resizes to 384x384 maintaining aspect ratio, then encodes with NO_WRAP.
     *
     * @param imageFile Input image file
     * @return Base64 encoded string (NO_WRAP), or null if processing fails
     */
    fun resizeAndEncodeToBase64(imageFile: File): String? {
        return try {
            Log.d(TAG, "Processing image: ${imageFile.name}, size: ${imageFile.length()} bytes")

            // Decode image
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode image file")
                return null
            }

            Log.d(TAG, "Original dimensions: ${originalBitmap.width}x${originalBitmap.height}")

            // Resize maintaining aspect ratio
            val resizedBitmap = resizeBitmap(originalBitmap, TARGET_SIZE)
            Log.d(TAG, "Resized dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val imageBytes = outputStream.toByteArray()

            Log.d(TAG, "Compressed size: ${imageBytes.size} bytes")

            // Encode to Base64 with NO_WRAP (critical for Gemini API)
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            // Cleanup
            originalBitmap.recycle()
            resizedBitmap.recycle()
            outputStream.close()

            Log.d(TAG, "Base64 encoding complete: ${base64.length} chars")
            base64
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image: ${e.message}", e)
            null
        }
    }

    /**
     * Resize bitmap to fit within target size while maintaining aspect ratio.
     * Uses high-quality bilinear filtering.
     */
    private fun resizeBitmap(original: Bitmap, maxSize: Int): Bitmap {
        val width = original.width
        val height = original.height

        // Calculate scaling factor
        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    /**
     * Get MIME type for image file based on extension.
     * Defaults to image/jpeg for compatibility.
     */
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/jpeg" // Safe default
        }
    }
}
