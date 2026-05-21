package com.restlock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.MintBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun SupportCreatorDialog(
    onWatchAd: () -> Unit,
    onNoThanks: () -> Unit,
) {
    Dialog(onDismissRequest = onNoThanks) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            cornerRadius = 24,
            contentPadding = 20,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Support the creator?",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = RestLockPalette.TextHigh,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Could you watch a short ad to support the creator of Earn it!?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RestLockPalette.TextMid,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PrimaryAction(
                    label = "Yes, support the creator",
                    onClick = onWatchAd,
                    leadingIcon = Icons.Rounded.Favorite,
                    brush = MintBrush,
                    contentColor = RestLockPalette.Ink0,
                )
                SecondaryAction(
                    label = "No thanks",
                    onClick = onNoThanks,
                )
            }
        }
    }
}
