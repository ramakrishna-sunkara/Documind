package com.documind.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.documind.app.domain.model.ExtractionState
import com.documind.app.ui.components.DocumindLottieAnimation
import com.documind.app.ui.components.DocumindLottieAsset
import com.documind.app.ui.components.ErrorDialog
import com.documind.app.ui.components.GradientPrimaryButton
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.DocumindScreenBackground
import com.documind.app.ui.theme.ErrorRed
import com.documind.app.ui.theme.OnDeviceBadge
import com.documind.app.ui.theme.Secondary40
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val lottieAsset: DocumindLottieAsset,
    val tagIcon: ImageVector,
    val tag: String,
    val tagColor: Color,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.Document,
        tagIcon = Icons.Default.CloudOff,
        tag = "Privacy First",
        tagColor = ErrorRed,
        title = "Your documents\nnever leave\nyour phone.",
        description = "Cloud AI tools upload your files to remote servers. DocuMind processes everything locally — ideal for legal, financial, and personal documents."
    ),
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.AIAnimation,
        tagIcon = Icons.Default.Memory,
        tag = "On-Device AI",
        tagColor = Secondary40,
        title = "Powered by\nGemma 1B.\nNo internet.",
        description = "Google's Gemma model runs directly on your device via MediaPipe. Chat, summarize, and extract insights with zero cloud dependency."
    ),
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.Offline,
        tagIcon = Icons.Default.WifiOff,
        tag = "Works Offline",
        tagColor = OnDeviceBadge,
        title = "Import.\nAsk.\nUnderstand.",
        description = "PDF, Word, web articles, or pasted text — ask any question and get answers privately, even with no internet connection."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    extractionState: ExtractionState,
    onComplete: () -> Unit,
    onTryDemo: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    DocumindScreenBackground(modifier = modifier, showOrbs = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = onboardingPages[pageIndex])
            }

            // Page dots
            PageIndicator(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA
            GradientPrimaryButton(
                text = if (isLastPage) "Get Started" else "Continue",
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    }

    if (extractionState is ExtractionState.Error) {
        ErrorDialog(
            title = "Demo Could Not Load",
            message = extractionState.message,
            onDismiss = onDismissError,
            onRetry = onTryDemo,
            retryLabel = "Try Again"
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Illustration — top half, unconstrained
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            DocumindLottieAnimation(
                asset = page.lottieAsset,
                size = 200.dp
            )
        }

        // Tag pill
        Surface(
            shape = RoundedCornerShape(DocumindDimens.ChipRadius),
            color = page.tagColor.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = page.tagIcon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = page.tagColor
                )
                Text(
                    text = page.tag,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = page.tagColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big, bold left-aligned title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Left-aligned description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 6.dp,
                animationSpec = tween(250),
                label = "dot_width"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
