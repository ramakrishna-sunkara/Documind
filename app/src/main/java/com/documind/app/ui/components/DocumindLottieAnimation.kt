package com.documind.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

enum class DocumindLottieAsset(val fileName: String) {
    AIAnimation("lottie/ai_animation.json"),
    Loading("lottie/loading.json"),
    Document("lottie/document.json"),
    Offline("lottie/offline.json")
}

@Composable
fun DocumindLottieAnimation(
    asset: DocumindLottieAsset,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    iterations: Int = LottieConstants.IterateForever,
    showFallbackSpinner: Boolean = true
) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset(asset.fileName)
    )
    val composition = compositionResult.value
    if (composition == null) {
        if (showFallbackSpinner) {
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.45f),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        return
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
