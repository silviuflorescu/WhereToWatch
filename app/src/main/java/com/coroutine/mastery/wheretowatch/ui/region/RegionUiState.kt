package com.coroutine.mastery.wheretowatch.ui.region

import com.coroutine.mastery.wheretowatch.domain.model.Country
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import java.util.Locale

data class RegionUiState(
    val selection: RegionSelection = RegionSelection.Auto,
    val resolved: ResolvedRegion? = null,
    val countries: List<Country> = emptyList(),
) {
    val isDetecting: Boolean get() = resolved == null

    fun countryName(code: String): String = countries.firstOrNull { it.code == code }?.name ?: englishCountryName(code)
}

fun englishCountryName(code: String): String =
    runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: code

fun flagEmoji(code: String): String {
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return "🌐"
    return code.map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
}
