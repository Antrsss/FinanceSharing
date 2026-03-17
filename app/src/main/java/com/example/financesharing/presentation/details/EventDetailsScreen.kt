package com.example.financesharing.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesharing.domain.model.Contributor
import com.example.financesharing.domain.model.Currency
import com.example.financesharing.domain.model.Participant
import com.example.financesharing.domain.utils.MoneyFormatter
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showMyPayDialog by remember { mutableStateOf(false) }
    var showGuestPayDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var inviteQuery by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventId) {
        viewModel.start(eventId)
    }

    val event = uiState.event
    val currency: Currency = event?.currency ?: Currency.RUB

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = event?.title ?: "Детали сбора") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: if (uiState.loading) "Загрузка..." else "Сбор не найден",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        val progress = if (event.targetAmountMinor <= 0L) 0f
        else (event.currentAmountMinor.toFloat() / event.targetAmountMinor.toFloat()).coerceIn(0f, 1f)

        val totals = remember(uiState.contributions) {
            uiState.contributions
                .groupBy { c ->
                    when (val contr = c.contributor) {
                        is Contributor.User -> "user:${contr.uid}"
                        is Contributor.Guest -> "guest:${contr.displayName}"
                    }
                }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${MoneyFormatter.formatMinor(event.currentAmountMinor, currency)} из ${MoneyFormatter.formatMinor(event.targetAmountMinor, currency)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Дедлайн: ${event.deadline}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        dialogError = null
                        amountText = ""
                        showMyPayDialog = true
                    }
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null)
                    Text(" Скинуться")
                }

                TextButton(
                    onClick = {
                        dialogError = null
                        guestName = ""
                        amountText = ""
                        showGuestPayDialog = true
                    }
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                    Text(" Гость")
                }

                if (uiState.isOwner) {
                    TextButton(
                        onClick = {
                            dialogError = null
                            inviteQuery = ""
                            showInviteDialog = true
                        }
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                        Text(" Пригласить")
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Участники и сколько скинули",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.participants, key = { it.id }) { p ->
                    val (displayName, key) = when (p) {
                        is Participant.User -> p.username to "user:${p.uid}"
                        is Participant.Guest -> p.displayName to "guest:${p.displayName}"
                    }
                    val sumMinor = totals[key] ?: 0L

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = MoneyFormatter.formatMinor(sumMinor, currency),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (showMyPayDialog) {
        SimpleAmountDialog(
            title = "Ваш взнос",
            amountText = amountText,
            onAmountChange = { amountText = it },
            error = dialogError,
            onDismiss = { showMyPayDialog = false },
            onConfirm = {
                val major = amountText.replace(',', '.').toDoubleOrNull()
                val minor = ((major ?: 0.0) * 100.0).toLong()
                if (minor <= 0L) {
                    dialogError = "Введите сумму больше 0"
                } else {
                    scope.launch {
                        runCatching { viewModel.addMyContribution(eventId, minor) }
                            .onSuccess { showMyPayDialog = false }
                            .onFailure { dialogError = it.message ?: "Не удалось добавить взнос" }
                    }
                }
            }
        )
    }

    if (showGuestPayDialog) {
        SimpleGuestDialog(
            guestName = guestName,
            amountText = amountText,
            onGuestNameChange = { guestName = it },
            onAmountChange = { amountText = it },
            error = dialogError,
            onDismiss = { showGuestPayDialog = false },
            onConfirm = {
                val major = amountText.replace(',', '.').toDoubleOrNull()
                val minor = ((major ?: 0.0) * 100.0).toLong()
                if (guestName.isBlank()) {
                    dialogError = "Введите имя гостя"
                } else if (minor <= 0L) {
                    dialogError = "Введите сумму больше 0"
                } else {
                    scope.launch {
                        runCatching { viewModel.addGuestContribution(eventId, guestName, minor) }
                            .onSuccess { showGuestPayDialog = false }
                            .onFailure { dialogError = it.message ?: "Не удалось добавить гостя" }
                    }
                }
            }
        )
    }

    if (showInviteDialog) {
        val eventTitle = event?.title.orEmpty()
        SimpleInviteDialog(
            query = inviteQuery,
            onQueryChange = { inviteQuery = it },
            error = dialogError,
            onDismiss = { showInviteDialog = false },
            onConfirm = {
                if (inviteQuery.isBlank()) {
                    dialogError = "Введите username или email"
                    return@SimpleInviteDialog
                }
                scope.launch {
                    runCatching { viewModel.inviteByUsernameOrEmail(eventId, eventTitle, inviteQuery) }
                        .onSuccess { ok ->
                            if (ok) showInviteDialog = false else dialogError = "Пользователь не найден"
                        }
                        .onFailure { dialogError = it.message ?: "Не удалось пригласить" }
                }
            }
        )
    }
}

@Composable
private fun SimpleAmountDialog(
    title: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Ок") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SimpleGuestDialog(
    guestName: String,
    amountText: String,
    onGuestNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить гостя и взнос") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = guestName,
                    onValueChange = onGuestNameChange,
                    singleLine = true,
                    label = { Text("Имя гостя") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Ок") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SimpleInviteDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить пользователя") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    label = { Text("Username или Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = "Приглашение появится у пользователя во вкладке «Приглашения».",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Отправить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}


