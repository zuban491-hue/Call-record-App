package com.callcapture.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.callcapture.app.OmniCallApplication
import com.callcapture.app.data.model.AudioFormatOption
import com.callcapture.app.data.model.AudioSourceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val isCellularEnabled: Boolean,
    val isWhatsAppEnabled: Boolean,
    val isMessengerEnabled: Boolean,
    val isTelegramEnabled: Boolean,
    val audioSource: AudioSourceOption,
    val audioFormat: AudioFormatOption,
    val isSpeakerphoneAssistantEnabled: Boolean,
    val softwareGainMultiplier: Float
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as OmniCallApplication).preferenceManager

    private val _state = MutableStateFlow(
        SettingsState(
            isCellularEnabled = prefs.isCellularRecordEnabled,
            isWhatsAppEnabled = prefs.isWhatsAppRecordEnabled,
            isMessengerEnabled = prefs.isMessengerRecordEnabled,
            isTelegramEnabled = prefs.isTelegramRecordEnabled,
            audioSource = prefs.audioSourceOption,
            audioFormat = prefs.audioFormatOption,
            isSpeakerphoneAssistantEnabled = prefs.isSpeakerphoneAssistantEnabled,
            softwareGainMultiplier = prefs.softwareGainMultiplier
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun refreshFromPreferences() {
        _state.value = _state.value.copy(isCellularEnabled = prefs.isCellularRecordEnabled,
            isWhatsAppEnabled = prefs.isWhatsAppRecordEnabled, isMessengerEnabled = prefs.isMessengerRecordEnabled,
            isTelegramEnabled = prefs.isTelegramRecordEnabled)
    }

    fun setCellularEnabled(enabled: Boolean) {
        prefs.isCellularRecordEnabled = enabled
        _state.value = _state.value.copy(isCellularEnabled = enabled)
    }

    fun setWhatsAppEnabled(enabled: Boolean) {
        prefs.isWhatsAppRecordEnabled = enabled
        _state.value = _state.value.copy(isWhatsAppEnabled = enabled)
    }

    fun setMessengerEnabled(enabled: Boolean) {
        prefs.isMessengerRecordEnabled = enabled
        _state.value = _state.value.copy(isMessengerEnabled = enabled)
    }

    fun setTelegramEnabled(enabled: Boolean) {
        prefs.isTelegramRecordEnabled = enabled
        _state.value = _state.value.copy(isTelegramEnabled = enabled)
    }

    fun setAudioSource(source: AudioSourceOption) {
        prefs.audioSourceOption = source
        _state.value = _state.value.copy(audioSource = source)
    }

    fun setAudioFormat(format: AudioFormatOption) {
        prefs.audioFormatOption = format
        _state.value = _state.value.copy(audioFormat = format)
    }

    fun setSpeakerphoneAssistantEnabled(enabled: Boolean) {
        prefs.isSpeakerphoneAssistantEnabled = enabled
        _state.value = _state.value.copy(isSpeakerphoneAssistantEnabled = enabled)
    }

    fun setSoftwareGainMultiplier(gain: Float) {
        prefs.softwareGainMultiplier = gain
        _state.value = _state.value.copy(softwareGainMultiplier = gain)
    }

}
