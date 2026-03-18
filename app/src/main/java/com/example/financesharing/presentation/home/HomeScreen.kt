package com.example.financesharing.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.example.financesharing.presentation.components.GiftEventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GiftEventsViewModel,
    onCreateEventClick: () -> Unit,
    onEventClick: (String) -> Unit,
    onInvitationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasNotifications: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is GiftEventsUiState.Error) {
            snackbarHostState.showSnackbar((uiState as GiftEventsUiState.Error).message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GiftShare",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onInvitationsClick) {
                        NotificationBell(hasNotifications = hasNotifications)
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Профиль"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEventClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Создать сбор"
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is GiftEventsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is GiftEventsUiState.Success -> {
                HomeContent(
                    modifier = Modifier.padding(innerPadding),
                    active = state.active,
                    completed = state.completed,
                    onEventClick = onEventClick
                )
            }

            is GiftEventsUiState.Error -> {
                // Show last known successful data if any (omitted for brevity)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Что-то пошло не так. Попробуйте позже.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationBell(hasNotifications: Boolean, dotSize: Dp = 8.dp) {
    val color = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .size(48.dp)
            .drawBehind {
                if (hasNotifications) {
                    val radius = dotSize.toPx() / 2f
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(size.width - radius * 1.2f, radius * 1.2f)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = "Приглашения"
        )
    }
}

@Composable
private fun HomeContent(
    active: List<com.example.financesharing.domain.model.GiftEventDomain>,
    completed: List<com.example.financesharing.domain.model.GiftEventDomain>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (active.isNotEmpty()) {
            item {
                SectionHeader(title = "Активные сборы")
            }
            items(active, key = { it.id }) { event ->
                GiftEventCard(
                    event = event,
                    onClick = onEventClick
                )
            }
        }

        if (completed.isNotEmpty()) {
            item {
                SectionHeader(title = "Завершенные")
            }
            items(completed, key = { it.id }) { event ->
                GiftEventCard(
                    event = event,
                    onClick = onEventClick
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}


