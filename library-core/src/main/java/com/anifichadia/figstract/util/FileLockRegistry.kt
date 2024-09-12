package com.anifichadia.figstract.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class FileLockRegistry {
    private val locks = mutableMapOf<File, Mutex>()

    suspend fun <T> withLock(file: File, block: () -> T) {
        val mutex = synchronized(this) {
            locks.getOrPut(file) { Mutex() }
        }

        mutex.withLock(action = block)
    }
}
