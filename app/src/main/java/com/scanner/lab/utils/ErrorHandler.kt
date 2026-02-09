package com.scanner.lab.utils

import android.util.Log

/**
 * Error handler with safe execution wrappers
 */
object ErrorHandler {
    
    /**
     * Execute a block safely and handle exceptions
     */
    inline fun <T> safe(
        tag: String = "ErrorHandler",
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Log.e(tag, "Error occurred: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute a block safely with custom error message
     */
    inline fun <T> safeWithMessage(
        tag: String = "ErrorHandler",
        errorMessage: String,
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Log.e(tag, "$errorMessage: ${e.message}", e)
            Result.failure(Exception(errorMessage, e))
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserFriendlyMessage(exception: Exception): String {
        return when (exception) {
            is java.io.FileNotFoundException -> "File not found"
            is java.io.IOException -> "File operation failed"
            is IllegalArgumentException -> "Invalid input"
            is OutOfMemoryError -> "Not enough memory"
            else -> "An error occurred: ${exception.message ?: "Unknown error"}"
        }
    }
}
