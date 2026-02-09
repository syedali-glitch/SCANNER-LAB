package com.scanner.lab.utils

import android.graphics.Bitmap
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Advanced memory management with LRU cache and object pooling
 */
class AdvancedMemoryManager private constructor() {
    
    companion object {
        @Volatile
        private var instance: AdvancedMemoryManager? = null
        
        fun getInstance(): AdvancedMemoryManager {
            return instance ?: synchronized(this) {
                instance ?: AdvancedMemoryManager().also { instance = it }
            }
        }
    }
    
    // LRU Cache for bitmaps (25% of available memory)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4
    
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }
    
    // ByteArray object pool to reduce allocations
    private val byteArrayPool = ConcurrentLinkedQueue<ByteArray>()
    private val maxPoolSize = 10
    
    /**
     * Get bitmap from cache
     */
    fun getBitmap(key: String): Bitmap? {
        return bitmapCache.get(key)
    }
    
    /**
     * Add bitmap to cache
     */
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            bitmapCache.put(key, bitmap)
        }
    }
    
    /**
     * Remove bitmap from cache
     */
    fun removeBitmap(key: String) {
        bitmapCache.remove(key)
    }
    
    /**
     * Clear all cached bitmaps
     */
    fun clearCache() {
        bitmapCache.evictAll()
    }
    
    /**
     * Get ByteArray from pool or create new
     */
    fun getByteArray(size: Int): ByteArray {
        val pooled = byteArrayPool.poll()
        return if (pooled != null && pooled.size >= size) {
            pooled
        } else {
            ByteArray(size)
        }
    }
    
    /**
     * Return ByteArray to pool
     */
    fun recycleByteArray(array: ByteArray) {
        if (byteArrayPool.size < maxPoolSize) {
            byteArrayPool.offer(array)
        }
    }
    
    /**
     * Recycle bitmap safely
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
    
    /**
     * Compress bitmap with quality control
     */
    fun compressBitmap(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 85
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(format, quality, stream)
        val bytes = stream.toByteArray()
        stream.close()
        return bytes
    }
    
    /**
     * Get current memory usage info
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        return MemoryInfo(
            totalMemory = runtime.totalMemory() / 1024 / 1024,
            freeMemory = runtime.freeMemory() / 1024 / 1024,
            maxMemory = runtime.maxMemory() / 1024 / 1024,
            cacheSize = bitmapCache.size(),
            cacheMaxSize = cacheSize
        )
    }
    
    data class MemoryInfo(
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val cacheSize: Int,
        val cacheMaxSize: Int
    )
}
