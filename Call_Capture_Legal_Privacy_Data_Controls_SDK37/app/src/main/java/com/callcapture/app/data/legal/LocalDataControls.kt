package com.callcapture.app.data.legal

import android.content.Context
import com.callcapture.app.OmniCallApplication
import com.callcapture.app.data.db.AppDatabase
import com.callcapture.app.data.live.RecordingLiveState
import com.callcapture.app.data.messages.MessageHistoryStore
import com.callcapture.app.service.CallRecordingCoordinator
import com.callcapture.app.service.CallRecordingService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

object LocalDataControls {
    @Volatile var deleting: Boolean = false
        private set
    private val deletionMutex = Mutex()
    /** Call from the UI thread before IO deletion. Stops capture, not Android permission grants. */
    fun pause(context: Context) {
        val prefs = OmniCallApplication.instance.preferenceManager
        prefs.isCellularRecordEnabled = false
        prefs.isWhatsAppRecordEnabled = false
        prefs.isMessengerRecordEnabled = false
        prefs.isTelegramRecordEnabled = false
        CallRecordingCoordinator.stopUser(context)
    }
    suspend fun pauseMessages(context: Context) = withContext(Dispatchers.IO) {
        MessageHistoryStore.get(context).setEnabled(false)
    }
    data class Result(val recordingRows: Int, val messageRows: Int, val failedFiles: Int)
    suspend fun deleteSavedData(context: Context): Result = deletionMutex.withLock {
        deleting = true
        try {
        withContext(Dispatchers.IO) {
        // Do not race an in-flight recording finalization or delete its open audio file.
        withTimeout(15000) {
            RecordingLiveState.state.first {
                !CallRecordingService.isServiceRecording && it.phase !in listOf(
                    RecordingLiveState.Phase.RECORDING, RecordingLiveState.Phase.STARTING, RecordingLiveState.Phase.SAVING)
            }
        }
        val messages = MessageHistoryStore.get(context)
        messages.setEnabled(false)
        val messageCount = messages.count()
        messages.clear()
        val dao = AppDatabase.getInstance(context).recordingDao()
        val rows = dao.getAllRecordings().first()
        val directory = File(context.filesDir, "recordings").canonicalFile
        val allowedPrefix = directory.path + File.separator
        var deletedRows = 0
        val failed = mutableSetOf<String>()
        for (row in rows) {
            val file = File(row.filePath).canonicalFile
            if (!file.path.startsWith(allowedPrefix)) { failed.add(file.path); continue }
            if (!file.exists() || file.delete()) {
                dao.deleteById(row.id)
                deletedRows += 1
            } else failed.add(file.path)
        }
        // Remove local orphan audio left by interrupted/failed captures; never follow paths outside this directory.
        val knownPaths = rows.map { File(it.filePath).canonicalPath }.toSet()
        directory.listFiles()?.forEach { entry ->
            val file = entry.canonicalFile
            if (file.path !in knownPaths && file.isFile && file.path.startsWith(allowedPrefix) && file.exists() && !file.delete()) failed.add(file.path)
        }
        Result(deletedRows, messageCount, failed.size)
        }
        } finally { deleting = false }
    }
}
