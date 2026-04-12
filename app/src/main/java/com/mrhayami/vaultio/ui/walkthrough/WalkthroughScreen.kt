package com.mrhayami.vaultio.ui.walkthrough

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import kotlinx.coroutines.launch

@Composable
fun WalkthroughScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    WalkthroughContent(
        onFinish = {
            scope.launch {
                userPreferencesRepository.setShouldShowWalkthrough(false)
                onFinish()
            }
        },
        modifier = modifier
    )
}

@Composable
fun WalkthroughContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primaryContainer
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                if (pagerState.currentPage < 3) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Get Started")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Done, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> WalkthroughPage(
                    title = "Welcome to Vaultio",
                    description = "Your ultimate companion for managing your TCG collection. Track your cards, monitor prices, and complete your Pokedex.",
                    imageText = "🎴"
                )
                1 -> WalkthroughPage(
                    title = "Bring Your Own API Key",
                    description = "Vaultio uses the JustTCG API for card data and pricing. To ensure the best experience and avoid shared rate limits, we recommend providing your own API key in the settings.",
                    imageText = "🔑"
                )
                2 -> WalkthroughPage(
                    title = "Optimized Scanning",
                    description = "For the fastest and most accurate card matching, we highly recommend downloading card sets before scanning. This allows the scanner to perform offline matching locally on your device.",
                    imageText = "🔍"
                )
                3 -> WalkthroughPage(
                    title = "Bulk Scanning Mode",
                    description = "Rapidly scan your collection with Bulk Mode. Configure default condition and printing, and auto-save high-confidence matches while logging ambiguous ones for later review.",
                    imageText = "⚡"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalkthroughContentPreview() {
    VaultioTheme {
        WalkthroughContent(onFinish = {})
    }
}

@Composable
fun WalkthroughPage(
    title: String,
    description: String,
    imageText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = imageText,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
