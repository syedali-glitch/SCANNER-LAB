package com.plainlabs.qrpdftools.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object CoroutineOptimizer {
    val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val OptimizedIO = Dispatchers.IO
}