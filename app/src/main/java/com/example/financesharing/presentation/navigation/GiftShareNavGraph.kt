package com.example.financesharing.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesharing.presentation.auth.AuthViewModel
import com.example.financesharing.presentation.auth.LoginScreen
import com.example.financesharing.presentation.auth.RegisterScreen
import com.example.financesharing.presentation.create.CreateEventScreen
import com.example.financesharing.presentation.details.EventDetailsScreen
import com.example.financesharing.presentation.home.GiftEventsViewModel
import com.example.financesharing.presentation.home.GiftEventsViewModelFactory
import com.example.financesharing.presentation.home.HomeScreen
import com.example.financesharing.presentation.invitations.InvitationsScreen
import com.example.financesharing.presentation.profile.ProfileScreen
import com.example.financesharing.notifications.WorkScheduler
import com.example.financesharing.data.repository.firestore.FirestoreInvitationRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

sealed class GiftShareScreen(val route: String) {
    data object Login : GiftShareScreen("auth/login")
    data object Register : GiftShareScreen("auth/register")
    data object Home : GiftShareScreen("home")
    data object CreateEvent : GiftShareScreen("create_event")
    data object Invitations : GiftShareScreen("invitations")
    data object Profile : GiftShareScreen("profile")
    data object EventDetails : GiftShareScreen("event_details/{eventId}") {
        fun createRoute(eventId: String): String = "event_details/$eventId"
    }
}

@Composable
fun GiftShareNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val session by authViewModel.session.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(session.isLoggedIn) {
        if (session.isLoggedIn) {
            WorkScheduler.schedule(context.applicationContext)
        }
    }

    LaunchedEffect(session.isLoggedIn) {
        val current = navController.currentDestination?.route
        val graphId = navController.graph.id
        if (graphId == 0) return@LaunchedEffect
        if (session.isLoggedIn) {
            if (current == GiftShareScreen.Login.route || current == GiftShareScreen.Register.route) {
                navController.navigate(GiftShareScreen.Home.route) {
                    popUpTo(graphId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (current != GiftShareScreen.Login.route && current != GiftShareScreen.Register.route) {
                navController.navigate(GiftShareScreen.Login.route) {
                    popUpTo(graphId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (session.isLoggedIn) GiftShareScreen.Home.route else GiftShareScreen.Login.route,
        modifier = modifier
    ) {
        composable(route = GiftShareScreen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onRegisterClick = { navController.navigate(GiftShareScreen.Register.route) }
            )
        }

        composable(route = GiftShareScreen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = GiftShareScreen.Home.route) {
            val eventsViewModel: GiftEventsViewModel = viewModel(factory = GiftEventsViewModelFactory())
            val invitesRepo = remember { FirestoreInvitationRepository() }
            var hasInvites by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                invitesRepo.observePendingInvitations()
                    .catch { hasInvites = false }
                    .collectLatest { list ->
                        hasInvites = list.isNotEmpty()
                    }
            }
            HomeScreen(
                viewModel = eventsViewModel,
                onCreateEventClick = {
                    navController.navigate(GiftShareScreen.CreateEvent.route)
                },
                onEventClick = { eventId ->
                    navController.navigate(GiftShareScreen.EventDetails.createRoute(eventId))
                },
                onInvitationsClick = {
                    navController.navigate(GiftShareScreen.Invitations.route)
                },
                onProfileClick = {
                    navController.navigate(GiftShareScreen.Profile.route)
                },
                hasNotifications = hasInvites
            )
        }

        composable(route = GiftShareScreen.CreateEvent.route) {
            CreateEventScreen(
                onBackClick = { navController.popBackStack() },
                onCreated = { eventId ->
                    navController.navigate(GiftShareScreen.EventDetails.createRoute(eventId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = GiftShareScreen.EventDetails.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailsScreen(
                eventId = eventId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = GiftShareScreen.Invitations.route) {
            InvitationsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = GiftShareScreen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}


