package com.example.financesharing.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.domain.model.Currency
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SessionState(
    val user: FirebaseUser? = null
) {
    val isLoggedIn: Boolean get() = user != null
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseProvider.auth
    private val firestore = FirebaseProvider.firestore

    private val _session = MutableStateFlow(SessionState(user = auth.currentUser))
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
        _session.value = SessionState(user = it.currentUser)
    }

    init {
        auth.addAuthStateListener(listener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
        super.onCleared()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                // Best-effort: ensure lookup indexes exist for this account
                ensureLookupIndexes()
                _uiState.value = AuthUiState.Idle
            } catch (t: Throwable) {
                _uiState.value = AuthUiState.Error(t.message ?: "Не удалось войти")
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: error("No user")

                val usernameTrim = username.trim()
                val usernameKey = usernameTrim.lowercase()
                val emailTrim = email.trim()
                val emailKey = emailTrim.lowercase()

                val userDoc = mapOf(
                    "username" to usernameTrim,
                    "email" to emailTrim,
                    "avatarId" to "avatar_1",
                    "baseCurrency" to Currency.RUB.code,
                    "walletBalance" to 0L,
                    "spentTotal" to 0L,
                    "bonusPoints" to 0L
                )

                // Always create the user profile first (critical for app UX).
                firestore.collection("users").document(uid)
                    .set(userDoc, SetOptions.merge())
                    .await()

                // Best-effort: create public lookup indexes.
                runCatching { firestore.collection("usernames").document(usernameKey).set(mapOf("uid" to uid)).await() }
                runCatching { firestore.collection("emails").document(emailKey).set(mapOf("uid" to uid)).await() }

                _uiState.value = AuthUiState.Idle
            } catch (t: Throwable) {
                _uiState.value = AuthUiState.Error(t.message ?: "Не удалось зарегистрироваться")
            }
        }
    }

    private suspend fun ensureLookupIndexes() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val snap = firestore.collection("users").document(uid).get().await()
        val username = (snap.getString("username") ?: "").trim()
        val email = (snap.getString("email") ?: user.email.orEmpty()).trim()

        if (username.isNotBlank()) {
            val key = username.lowercase()
            runCatching {
                firestore.collection("usernames").document(key)
                    .set(mapOf("uid" to uid))
                    .await()
            }
        }

        if (email.isNotBlank()) {
            val key = email.lowercase()
            runCatching {
                firestore.collection("emails").document(key)
                    .set(mapOf("uid" to uid))
                    .await()
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}

