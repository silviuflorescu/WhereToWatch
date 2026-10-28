package com.coroutine.mastery.wheretowatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coroutine.mastery.wheretowatch.ui.details.MovieDetailsScreen
import com.coroutine.mastery.wheretowatch.ui.region.RegionViewModel
import com.coroutine.mastery.wheretowatch.ui.search.SearchScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val regionViewModel: RegionViewModel = hiltViewModel()
    NavHost(navController = navController, startDestination = SearchRoute) {
        val openDetails = { movieId: Int -> navController.navigate(MovieDetailsRoute(movieId)) }
        composable<SearchRoute> {
            SearchScreen(regionViewModel = regionViewModel, onMovieClick = openDetails)
        }
        composable<MovieDetailsRoute> {
            MovieDetailsScreen(
                regionViewModel = regionViewModel,
                onBack = { navController.navigateUp() },
                onMovieClick = openDetails,
            )
        }
    }
}
