package com.huggets.mynotes.data

import android.content.Context

class PreferenceRepository(context: Context) {
    private val preferenceDao = ApplicationDatabase.getDb(context).preferenceDao()

    suspend fun getPreference(name: String) = preferenceDao.getPreference(name)

    suspend fun setPreference(preference: Preference) = preferenceDao.insert(preference)
}