package com.coroutine.mastery.wheretowatch

import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.Provider
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders

fun movie(id: Int, title: String = "Movie $id") = Movie(
    id = id,
    title = title,
    year = 2010,
    genres = listOf("Action"),
    overview = "",
    posterUrl = null,
    rating = 8.0,
    voteCount = 100,
)

fun movieDetails(id: Int, title: String = "Movie $id") = MovieDetails(
    id = id,
    title = title,
    tagline = null,
    overview = "",
    year = 2010,
    runtimeMinutes = 148,
    genres = listOf("Action"),
    rating = 8.0,
    voteCount = 100,
    posterUrl = null,
    backdropUrl = null,
)

fun usProviders(vararg names: String) = mapOf("US" to watchProviders(*names))

fun watchProviders(vararg names: String) = WatchProviders(
    stream = names.mapIndexed { index, name -> Provider(index, name, logoUrl = null) },
    rent = emptyList(),
    buy = emptyList(),
    link = null,
)
