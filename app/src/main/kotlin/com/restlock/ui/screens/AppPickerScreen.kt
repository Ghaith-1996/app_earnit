package com.restlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restlock.domain.InstalledApp
import com.restlock.ui.AppPickerViewModel
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun AppPickerScreen(
    viewModel: AppPickerViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PickerTopBar(allowedCount = state.allowed.size, onBack = onBack)

            Spacer(Modifier.height(8.dp))

            SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.loading -> LoadingState()
                    state.filtered.isEmpty() -> EmptyState(query = state.query)
                    else -> AppList(
                        apps = state.filtered,
                        allowed = state.allowed,
                        onToggle = viewModel::toggle,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryAction(
                    label = "Clear allowed",
                    onClick = viewModel::clearAllowed,
                    modifier = Modifier.weight(1f),
                )
                PrimaryAction(
                    label = "Save",
                    onClick = { viewModel.save(onSaved) },
                    brush = PrimaryBrush,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PickerTopBar(allowedCount: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = RestLockPalette.TextHigh,
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Apps allowed during lock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = if (allowedCount == 0) "Strict mode: only base apps stay open"
                else "$allowedCount allowed",
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextLow,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = RestLockPalette.TextLow,
            )
        },
        placeholder = {
            Text(
                text = "Search installed apps",
                color = RestLockPalette.TextLow,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = RestLockPalette.TextHigh,
            unfocusedTextColor = RestLockPalette.TextHigh,
            focusedContainerColor = Color.White.copy(alpha = 0.06f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
            focusedBorderColor = RestLockPalette.Violet,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = RestLockPalette.Violet,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AppList(
    apps: List<InstalledApp>,
    allowed: Set<String>,
    onToggle: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                isAllowed = app.packageName in allowed,
                onClick = { onToggle(app.packageName) },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    isAllowed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isAllowed) Color.White.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.04f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppGlyph(label = app.label)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = RestLockPalette.TextLow,
            )
        }
        SelectionDot(isSelected = isAllowed)
    }
}

@Composable
private fun AppGlyph(label: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.TextHigh,
        )
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) RestLockPalette.Violet
                else Color.White.copy(alpha = 0.10f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RestLockPalette.Violet)
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (query.isBlank()) "No installed apps to allow."
            else "No apps match \"$query\".",
            style = MaterialTheme.typography.titleMedium,
            color = RestLockPalette.TextMid,
        )
    }
}
