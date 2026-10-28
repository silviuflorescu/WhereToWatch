package com.coroutine.mastery.wheretowatch.domain.repository

import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion

fun interface CountryDetector {
    suspend fun detect(): ResolvedRegion
}
