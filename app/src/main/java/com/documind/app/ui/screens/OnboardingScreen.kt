package com.documind.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.documind.app.domain.model.ExtractionState
import com.documind.app.ui.components.DocumindLottieAnimation
import com.documind.app.ui.components.DocumindLottieAsset
import com.documind.app.ui.components.ErrorDialog
import com.documind.app.ui.theme.DocumindDimens
import com.documind.app.ui.theme.DocumindScreenBackground
import com.documind.app.ui.theme.ErrorRed
import com.documind.app.ui.theme.OnDeviceBadge
import com.documind.app.ui.theme.Primary40
import com.documind.app.ui.theme.Secondary40
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val lottieAsset: DocumindLottieAsset,
    val title: String,
    val description: String,
    val accentColor: Color,
    val tag: String
)

private val onboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.Document,
        title = "Your Documents Stay Private",
        description = "Cloud AI tools upload your contracts, research, and financial files to remote servers. DocuMind keeps everything on your phone.",
        accentColor = ErrorRed,
        tag = "Privacy First"
    ),
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.AIAnimation,
        title = "AI Runs On Your Device",
        description = "Powered by Google's Gemma 1B model via MediaPipe. Chat, summarize, and extract insights — with zero cloud upload.",
        accentColor = Secondary40,
        tag = "On-Device AI"
    ),
    OnboardingPage(
        lottieAsset = DocumindLottieAsset.Offline,
        title = "Import. Ask. Understand.",
        description = "Open a PDF, Word file, web article, or pasted text. Ask questions and get answers offline, anytime.",
        accentColor = OnDeviceBadge,
        tag = "Works Offline"
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
    val isLastPage: Boolean = pagerState.currentPage == onboardingPages.lastIndex
    DocumindScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
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
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = onboardingPages[pageIndex])
            }
            Spacer(modifier = Modifier.height(24.dp))
            PageIndicator(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DocumindDimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary40
                )
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DocumindLottieAnimation(
            asset = page.lottieAsset,
            size = 140.dp
        )

        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(DocumindDimens.ChipRadius),
            color = page.accentColor.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = page.accentColor
                )
                Text(
                    text = page.tag,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = page.accentColor
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected: Boolean = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(if (isSelected) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                    )
            )
        }
    }
}
