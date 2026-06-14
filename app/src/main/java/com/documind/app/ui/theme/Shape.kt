package com.documind.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DocumindDimens {
    val CardRadius = 14.dp
    val ChipRadius = 8.dp
    val InputRadius = 12.dp
    val DialogRadius = 16.dp
    val ButtonRadius = 10.dp
    val BadgeRadius = 20.dp
    val BubbleRadius = 16.dp
}

val DocumindShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(DocumindDimens.ChipRadius),
    medium = RoundedCornerShape(DocumindDimens.CardRadius),
    large = RoundedCornerShape(DocumindDimens.InputRadius),
    extraLarge = RoundedCornerShape(24.dp)
)
