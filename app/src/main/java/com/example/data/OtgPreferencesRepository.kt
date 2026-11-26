package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.otgDataStore: DataStore<Preferences> by preferencesDataStore(name = "otg_drive_prefs")

data class OtgUserPreferences(
    val sortField: SortField = SortField.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val viewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val lastUsbTreeUri: String? = null,
    val phoneSafTreeUri: String? = null,
    val volumeUriMap: Map<String, String> = emptyMap()
)

class OtgPreferencesRepository(private val context: Context) {

    private object Keys {
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val LAST_USB_URI = stringPreferencesKey("last_usb_uri")
        val PHONE_SAF_URI = stringPreferencesKey("phone_saf_uri")
        val VOLUME_MAP_PREFIX = "vol_uri_"
    }

    val preferencesFlow: Flow<OtgUserPreferences> = context.otgDataStore.data.map { prefs ->
        val sortField = runCatching {
            SortField.valueOf(prefs[Keys.SORT_FIELD] ?: SortField.NAME.name)
        }.getOrDefault(SortField.NAME)
        val sortOrder = runCatching {
            SortOrder.valueOf(prefs[Keys.SORT_ORDER] ?: SortOrder.ASCENDING.name)
        }.getOrDefault(SortOrder.ASCENDING)
        val viewMode = runCatching {
            ViewMode.valueOf(prefs[Keys.VIEW_MODE] ?: ViewMode.LIST.name)
        }.getOrDefault(ViewMode.LIST)

        val volMap = mutableMapOf<String, String>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith(Keys.VOLUME_MAP_PREFIX) && value is String) {
                volMap[key.name.removePrefix(Keys.VOLUME_MAP_PREFIX)] = value
            }
        }

        OtgUserPreferences(
            sortField = sortField,
            sortOrder = sortOrder,
            viewMode = viewMode,
            showHiddenFiles = prefs[Keys.SHOW_HIDDEN] ?: false,
            lastUsbTreeUri = prefs[Keys.LAST_USB_URI],
            phoneSafTreeUri = prefs[Keys.PHONE_SAF_URI],
            volumeUriMap = volMap
        )
    }

    suspend fun saveVolumeTreeUri(volumeKey: String, uri: Uri) {
        context.otgDataStore.edit { prefs ->
            prefs[stringPreferencesKey(Keys.VOLUME_MAP_PREFIX + volumeKey)] = uri.toString()
            prefs[Keys.LAST_USB_URI] = uri.toString()
        }
    }

    suspend fun removeVolumeTreeUri(volumeKey: String) {
        context.otgDataStore.edit { prefs ->
            val key = stringPreferencesKey(Keys.VOLUME_MAP_PREFIX + volumeKey)
            val removed = prefs.remove(key)
            if (removed != null && prefs[Keys.LAST_USB_URI] == removed) {
                prefs.remove(Keys.LAST_USB_URI)
            }
        }
    }

    suspend fun saveLastUsbTreeUri(uri: Uri?) {
        context.otgDataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.LAST_USB_URI)
            } else {
                prefs[Keys.LAST_USB_URI] = uri.toString()
            }
        }
    }

    suspend fun savePhoneSafTreeUri(uri: Uri?) {
        context.otgDataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.PHONE_SAF_URI)
            } else {
                prefs[Keys.PHONE_SAF_URI] = uri.toString()
            }
        }
    }

    suspend fun updateSort(field: SortField, order: SortOrder) {
        context.otgDataStore.edit { prefs ->
            prefs[Keys.SORT_FIELD] = field.name
            prefs[Keys.SORT_ORDER] = order.name
        }
    }

    suspend fun updateViewMode(viewMode: ViewMode) {
        context.otgDataStore.edit { prefs ->
            prefs[Keys.VIEW_MODE] = viewMode.name
        }
    }

    suspend fun updateShowHidden(show: Boolean) {
        context.otgDataStore.edit { prefs ->
            prefs[Keys.SHOW_HIDDEN] = show
        }
    }

    suspend fun getVolumeUriMap(): Map<String, String> {
        return preferencesFlow.first().volumeUriMap
    }
}
