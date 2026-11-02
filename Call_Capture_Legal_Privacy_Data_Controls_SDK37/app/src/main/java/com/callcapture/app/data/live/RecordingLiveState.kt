package com.callcapture.app.data.live

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ephemeral service progress. The Room list remains the source of truth for saved files. */
object RecordingLiveState {
    enum class Phase { IDLE, STARTING, RECORDING, SAVING, SAVED, ERROR }
    data class Status(val session: String = "", val phase: Phase = Phase.IDLE,
        val appName: String = "", val startedElapsed: Long = 0L, val detail: String = "")
    private val mutable = MutableStateFlow(Status())
    val state = mutable.asStateFlow()
    fun begin(session: String, appName: String) {
        mutable.value = Status(session, Phase.STARTING, appName)
    }
    fun report(session: String, phase: Phase, detail: String = "", startedElapsed: Long? = null) {
        // An older save job must not overwrite a newer recording's status.
        mutable.update { current ->
            if (current.session != session) current else current.copy(phase = phase, detail = detail,
                startedElapsed = startedElapsed ?: current.startedElapsed)
        }
    }
    fun rename(session: String, appName: String) {
        mutable.update { if (it.session == session) it.copy(appName = appName) else it }
    }
}
