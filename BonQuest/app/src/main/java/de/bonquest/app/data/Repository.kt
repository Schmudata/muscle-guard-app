package de.bonquest.app.data

import android.content.Context
import de.bonquest.app.domain.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/** Stores everything as one JSON file in the app's private storage (offline, no account). */
class Repository(context: Context) {
    private val file = File(context.filesDir, "bonquest.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private fun load(): AppData = runCatching {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    }.getOrElse { AppData() }

    @Synchronized
    fun update(transform: (AppData) -> AppData): AppData {
        val next = transform(_data.value)
        _data.value = next
        scope.launch {
            writeLock.withLock {
                val tmp = File(file.parentFile, "bonquest.json.tmp")
                tmp.writeText(json.encodeToString(AppData.serializer(), _data.value))
                tmp.renameTo(file)
            }
        }
        return next
    }
}
