package com.plainlabs.qrpdftools.util

import android.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FilterRepository - Proprietary Image Processing Engine.
 * 
 * Provides high-end filters for document scanning.
 * Mandate: Execute on Dispatchers.Default.
 */
object FilterRepository {

    /**
     * Magic Color: Enhances contrast and saturation to make text pop.
     */
    suspend fun applyMagicColor(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Multi-matrix filter: Contrast + Saturation
        val cm = ColorMatrix().apply {
            setSaturation(1.2f) // Boost color
        }
        
        // Simple contrast adjustment via matrix scaling
        val contrast = 1.2f
        val translate = (-.5f * contrast + .5f) * 255f
        val contrastMatrix = floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        cm.postConcat(ColorMatrix(contrastMatrix))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return@withContext result
    }

    /**
     * B&W: High-contrast black and white for document scanning.
     */
    suspend fun applyBlackAndWhite(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val cm = ColorMatrix()
        cm.setSaturation(0f) // Grayscale
        
        // Extreme contrast for B&W document look
        val contrast = 2.0f
        val translate = (-.5f * contrast + .5f) * 255f
        val bwMatrix = floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        cm.postConcat(ColorMatrix(bwMatrix))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return@withContext result
    }

    /**
     * Grayscale: Standard 8-bit gray look.
     */
    suspend fun applyGrayscale(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return@withContext result
    }
}
