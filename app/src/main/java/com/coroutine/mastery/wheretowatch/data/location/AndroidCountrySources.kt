package com.coroutine.mastery.wheretowatch.data.location

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class AndroidNetworkCountrySource @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkCountrySource {
    override fun countryCode(): String? {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java) ?: return null
        return listOf(telephonyManager.networkCountryIso, telephonyManager.simCountryIso)
            .firstOrNull { !it.isNullOrBlank() }
    }
}

class AndroidDeviceLocaleSource @Inject constructor() : DeviceLocaleSource {
    override fun countryCode(): String? = Locale.getDefault().country.takeIf { it.isNotBlank() }
}
