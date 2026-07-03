package com.example.albuddy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.albuddy.data.model.STTEngineType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val HA_URL = stringPreferencesKey("ha_url")
    private val HA_TOKEN = stringPreferencesKey("ha_token")
    private val ACTIVE_STT_ENGINE = stringPreferencesKey("active_stt_engine")
    private val VOSK_MODEL_DOWNLOADED = booleanPreferencesKey("vosk_model_downloaded")
    private val PLAY_MATCH_SOUND = booleanPreferencesKey("play_match_sound")
    private val VIBRATE_ON_MATCH = booleanPreferencesKey("vibrate_on_match")

    val haUrl: Flow<String?> = context.dataStore.data.map { it[HA_URL] }
    val haToken: Flow<String?> = context.dataStore.data.map { it[HA_TOKEN] }
    val activeSttEngine: Flow<STTEngineType> = context.dataStore.data.map {
        val name = it[ACTIVE_STT_ENGINE] ?: STTEngineType.NATIVE_OFFLINE.name
        STTEngineType.valueOf(name)
    }
    val voskModelDownloaded: Flow<Boolean> = context.dataStore.data.map { it[VOSK_MODEL_DOWNLOADED] ?: false }
    val playMatchSound: Flow<Boolean> = context.dataStore.data.map { it[PLAY_MATCH_SOUND] ?: true }
    val vibrateOnMatch: Flow<Boolean> = context.dataStore.data.map { it[VIBRATE_ON_MATCH] ?: true }

    suspend fun setHaUrl(url: String) {
        context.dataStore.edit { it[HA_URL] = url }
    }

    suspend fun setHaToken(token: String) {
        context.dataStore.edit { it[HA_TOKEN] = token }
    }

    suspend fun setActiveSttEngine(engine: STTEngineType) {
        context.dataStore.edit { it[ACTIVE_STT_ENGINE] = engine.name }
    }

    suspend fun setVoskModelDownloaded(downloaded: Boolean) {
        context.dataStore.edit { it[VOSK_MODEL_DOWNLOADED] = downloaded }
    }

    suspend fun setPlayMatchSound(play: Boolean) {
        context.dataStore.edit { it[PLAY_MATCH_SOUND] = play }
    }

    suspend fun setVibrateOnMatch(vibrate: Boolean) {
        context.dataStore.edit { it[VIBRATE_ON_MATCH] = vibrate }
    }
}
