package com.coroutine.mastery.wheretowatch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coroutine.mastery.wheretowatch.R
import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.ui.theme.WhereToWatchTheme

@Composable
fun IdleView(modifier: Modifier = Modifier) {
    MessageView(
        icon = Icons.Outlined.Movie,
        title = stringResource(R.string.idle_title),
        message = stringResource(R.string.idle_message),
        modifier = modifier,
    )
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyView(query: String, modifier: Modifier = Modifier) {
    MessageView(
        icon = Icons.Outlined.SearchOff,
        title = stringResource(R.string.empty_title),
        message = stringResource(R.string.empty_message, query),
        modifier = modifier,
    )
}

@Composable
fun ErrorView(error: Throwable, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (error) {
        AppError.Network -> stringResource(R.string.error_network)
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        is AppError.Server -> stringResource(R.string.error_server, error.code)
        else -> stringResource(R.string.error_unknown)
    }
    MessageView(
        icon = Icons.Outlined.CloudOff,
        title = stringResource(R.string.error_title),
        message = message,
        modifier = modifier,
        action = {
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        },
    )
}

@Composable
private fun MessageView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorViewPreview() {
    WhereToWatchTheme {
        ErrorView(error = AppError.Network, onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyViewPreview() {
    WhereToWatchTheme {
        EmptyView(query = "asdfgh")
    }
}
