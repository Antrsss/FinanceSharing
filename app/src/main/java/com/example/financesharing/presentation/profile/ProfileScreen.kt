package com.example.financesharing.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesharing.domain.utils.MoneyFormatter
import com.example.financesharing.presentation.auth.AuthViewModel
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showTopUp by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExp by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Аккаунт") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Выйти")
                    }
                }
            )
        }
    ) { inner ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Загрузка...")
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is ProfileUiState.Success -> {
                val profile = state.profile
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarCircle(
                            avatarId = profile.avatarId,
                            modifier = Modifier.size(56.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Text(
                        text = "Аватар",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AvatarChoice(
                            avatarId = "avatar_1",
                            selected = profile.avatarId == "avatar_1",
                            onClick = { viewModel.setAvatar("avatar_1") }
                        )
                        AvatarChoice(
                            avatarId = "avatar_2",
                            selected = profile.avatarId == "avatar_2",
                            onClick = { viewModel.setAvatar("avatar_2") }
                        )
                        AvatarChoice(
                            avatarId = "avatar_3",
                            selected = profile.avatarId == "avatar_3",
                            onClick = { viewModel.setAvatar("avatar_3") }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Кошелёк (${profile.baseCurrency.code})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = MoneyFormatter.formatMinor(profile.walletBalanceMinor, profile.baseCurrency),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp)
                    )
                    Button(onClick = {
                        dialogError = null
                        topUpAmount = ""
                        cardNumber = ""
                        cardExp = ""
                        cardCvv = ""
                        showTopUp = true
                    }) {
                        Icon(Icons.Rounded.CreditCard, contentDescription = null)
                        Text(" Пополнить")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Сумма выкупа",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = MoneyFormatter.formatMinor(profile.spentTotalMinor, profile.baseCurrency),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Бонусы",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${profile.bonusPoints} бонус(ов)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (showTopUp) {
                    AlertDialog(
                        onDismissRequest = { showTopUp = false },
                        title = { Text("Пополнение (имитация)") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it.filter(Char::isDigit).take(16) },
                                    singleLine = true,
                                    label = { Text("Номер карты") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = cardExp,
                                        onValueChange = { cardExp = it.take(5) },
                                        singleLine = true,
                                        label = { Text("MM/YY") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = cardCvv,
                                        onValueChange = { cardCvv = it.filter(Char::isDigit).take(3) },
                                        singleLine = true,
                                        label = { Text("CVV") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                                    )
                                }
                                OutlinedTextField(
                                    value = topUpAmount,
                                    onValueChange = { topUpAmount = it },
                                    singleLine = true,
                                    label = { Text("Сумма") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                if (!dialogError.isNullOrBlank()) {
                                    Text(dialogError!!, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text(
                                        text = "Мы не сотрудничаем с банками. Данные карты никуда не отправляются.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val major = topUpAmount.replace(',', '.').toDoubleOrNull()
                                val minor = ((major ?: 0.0) * 100.0).toLong()
                                when {
                                    cardNumber.length < 16 -> dialogError = "Введите 16 цифр карты"
                                    cardCvv.length < 3 -> dialogError = "Введите CVV"
                                    minor <= 0L -> dialogError = "Введите сумму больше 0"
                                    else -> {
                                        viewModel.topUp(minor)
                                        showTopUp = false
                                    }
                                }
                            }) { Text("Пополнить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTopUp = false }) { Text("Отмена") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarChoice(
    avatarId: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(borderColor.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        AvatarCircle(avatarId = avatarId, modifier = Modifier.size(40.dp))
    }
}

@Composable
fun AvatarCircle(
    avatarId: String,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (avatarId) {
        "avatar_2" -> Icons.Rounded.Face
        "avatar_3" -> Icons.Rounded.SentimentSatisfied
        else -> Icons.Rounded.AccountCircle
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
    }
}

