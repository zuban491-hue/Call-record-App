package com.callcapture.app.data.model

import android.media.MediaRecorder

enum class AudioSourceOption(val title: String, val sourceValue: Int) {
    MIC("Microphone", MediaRecorder.AudioSource.MIC),
    VOICE_RECOGNITION("Microphone (voice recognition mode)", MediaRecorder.AudioSource.VOICE_RECOGNITION)
}

enum class AudioFormatOption(val extension: String, val mimeType: String) {
    AAC("m4a", "audio/mp4"),
    WAV("wav", "audio/wav")
}
