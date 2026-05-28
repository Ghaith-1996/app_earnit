package com.restlock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restlock.domain.ExerciseDefinition
import com.restlock.domain.ExerciseEquipment
import com.restlock.domain.MuscleGroup
import com.restlock.ui.theme.RestLockPalette
import kotlin.math.absoluteValue

@Composable
fun ExerciseArtwork(
    exercise: ExerciseDefinition,
    modifier: Modifier = Modifier.size(56.dp),
    cornerRadius: Dp = 18.dp,
) {
    val context = LocalContext.current
    val imageResId = remember(exercise.id, context) {
        context.resources.getIdentifier(
            "exercise_${exercise.id}",
            "drawable",
            context.packageName,
        )
    }

    if (imageResId != 0) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = exercise.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.White),
        )
        return
    }

    val baseColor = exercise.muscleGroup.artworkColor()
    val accentColor = exercise.equipment.artworkColor()
    val seed = exercise.id.hashCode().absoluteValue

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(baseColor, accentColor))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val firstX = ((seed % 73) / 72f) * w
            val secondY = (((seed / 17) % 67) / 66f) * h

            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = w * 0.34f,
                center = Offset(firstX, h * 0.2f),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.14f),
                radius = w * 0.38f,
                center = Offset(w * 0.85f, secondY),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.14f, h * 0.62f),
                size = Size(w * 0.72f, h * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.24f, h * 0.76f),
                size = Size(w * 0.52f, h * 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
            )
        }

        Icon(
            imageVector = Icons.Rounded.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(24.dp),
        )

        Text(
            text = exercise.equipment.shortLabel(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.Ink0.copy(alpha = 0.84f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun MuscleGroup.artworkColor(): Color {
    return when (this) {
        MuscleGroup.Biceps -> Color(0xFF2F80ED)
        MuscleGroup.Triceps -> Color(0xFF00A3A3)
        MuscleGroup.Back -> Color(0xFF1E5F74)
        MuscleGroup.Legs -> Color(0xFF2F9E44)
        MuscleGroup.Chest -> Color(0xFFE85D75)
        MuscleGroup.Shoulders -> Color(0xFFFF9F1C)
        MuscleGroup.Core -> Color(0xFF7C5CFF)
        MuscleGroup.Traps -> Color(0xFF6C63FF)
        MuscleGroup.Forearms -> Color(0xFF20C997)
        MuscleGroup.Neck -> Color(0xFFADB5BD)
        MuscleGroup.FullBody -> Color(0xFFFFC15A)
        MuscleGroup.Cardio -> Color(0xFFFF6B81)
    }
}

private fun ExerciseEquipment.artworkColor(): Color {
    return when (this) {
        ExerciseEquipment.Dumbbell -> Color(0xFF4DE3B1)
        ExerciseEquipment.Barbell -> Color(0xFF9D4EDD)
        ExerciseEquipment.Cable -> Color(0xFF48CAE4)
        ExerciseEquipment.Machine -> Color(0xFF90BE6D)
        ExerciseEquipment.Bodyweight -> Color(0xFFFFD166)
        ExerciseEquipment.Kettlebell -> Color(0xFFFF7B00)
        ExerciseEquipment.Sled -> Color(0xFF8D99AE)
        ExerciseEquipment.Conditioning -> Color(0xFFFF6B81)
        ExerciseEquipment.Other -> Color(0xFFE9ECFF)
    }
}

private fun ExerciseEquipment.shortLabel(): String {
    return when (this) {
        ExerciseEquipment.Dumbbell -> "DB"
        ExerciseEquipment.Barbell -> "BB"
        ExerciseEquipment.Cable -> "CB"
        ExerciseEquipment.Machine -> "MC"
        ExerciseEquipment.Bodyweight -> "BW"
        ExerciseEquipment.Kettlebell -> "KB"
        ExerciseEquipment.Sled -> "SL"
        ExerciseEquipment.Conditioning -> "HI"
        ExerciseEquipment.Other -> "GY"
    }
}
