package com.coroutine.mastery.wheretowatch.domain.model

data class WatchProviders(
    val stream: List<Provider>,
    val rent: List<Provider>,
    val buy: List<Provider>,
    val link: String?,
) {
    val isEmpty: Boolean get() = stream.isEmpty() && rent.isEmpty() && buy.isEmpty()

    companion object {
        val EMPTY = WatchProviders(emptyList(), emptyList(), emptyList(), link = null)
    }
}
