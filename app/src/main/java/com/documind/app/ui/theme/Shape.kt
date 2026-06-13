package com.documind.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DocumindDimens {
    val CardRadius = 20.dp
    val ChipRadius = 12.dp
    val InputRadius = 24.dp
    val DialogRadius = 20.dp
    val ButtonRadius = 12.dp
    val BadgeRadius = 24.dp
    val BubbleRadius = 20.dp
}

val DocumindShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(DocumindDimens.ChipRadius),
    medium = RoundedCornerShape(DocumindDimens.CardRadius),
    large = RoundedCornerShape(DocumindDimens.InputRadius),
    extraLarge = RoundedCornerShape(28.dp)
)
