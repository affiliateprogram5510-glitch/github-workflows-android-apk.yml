package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.powerForgeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "powerforge_prefs"
)

class ChecklistPreferencesRepository(private val context: Context) {

    private object Keys {
        val COMPLETED_STEPS = stringSetPreferencesKey("completed_deploy_steps")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    }

    val completedStepIds: Flow<Set<String>> = context.powerForgeDataStore.data.map { prefs ->
        prefs[Keys.COMPLETED_STEPS] ?: emptySet()
    }

    val useDynamicColor: Flow<Boolean> = context.powerForgeDataStore.data.map { prefs ->
        prefs[Keys.USE_DYNAMIC_COLOR] ?: false
    }

    suspend fun toggleStep(stepId: String) {
        context.powerForgeDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_STEPS] ?: emptySet()
            prefs[Keys.COMPLETED_STEPS] = if (stepId in current) {
                current - stepId
            } else {
                current + stepId
            }
        }
    }

    suspend fun resetSteps() {
        context.powerForgeDataStore.edit { prefs ->
            prefs[Keys.COMPLETED_STEPS] = emptySet()
        }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.powerForgeDataStore.edit { prefs ->
            prefs[Keys.USE_DYNAMIC_COLOR] = enabled
        }
    }
}
