package com.coroutine.mastery.wheretowatch.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

@Serializable
data class MovieDetailsRoute(val movieId: Int)
