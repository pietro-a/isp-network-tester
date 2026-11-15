package space.antonov.ispnetworktester.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

private val SERVER_ADDRESS = stringPreferencesKey("server_address")
private val SERVER_PORT = stringPreferencesKey("server_port")
private val PING_ADDRESS = stringPreferencesKey("ping_address")
private val USE_SERVER_ADDRESS_FOR_PING = booleanPreferencesKey("use_server_address_for_ping")
private val DOWNLOAD_SIZE = stringPreferencesKey("download_size")

class SettingsRepository(context: Context) {
    private val dataStore: DataStore<Preferences> =
        context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettings(
                serverAddress = preferences[SERVER_ADDRESS] ?: AppSettings.DEFAULT_SERVER_ADDRESS,
                serverPort = preferences[SERVER_PORT] ?: AppSettings.DEFAULT_SERVER_PORT,
                pingAddress = preferences[PING_ADDRESS] ?: AppSettings.DEFAULT_PING_ADDRESS,
                useServerAddressForPing = preferences[USE_SERVER_ADDRESS_FOR_PING] ?: true,
                downloadSize = preferences[DOWNLOAD_SIZE] ?: AppSettings.DEFAULT_DOWNLOAD_SIZE,
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS] = settings.serverAddress
            preferences[SERVER_PORT] = settings.serverPort
            preferences[PING_ADDRESS] = settings.pingAddress
            preferences[USE_SERVER_ADDRESS_FOR_PING] = settings.useServerAddressForPing
            preferences[DOWNLOAD_SIZE] = settings.downloadSize
        }
    }
}
