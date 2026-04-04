package com.mrhayami.vaultio.ui.walkthrough

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.UserPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun WalkthroughScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Row {
                    repeat(4) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(8.dp)
                                .padding(1.dp)
                                .padding(1.dp) // Just to make it a bit smaller or use background
                        ) {
                           Surface(
                               shape = MaterialTheme.shapes.extraSmall,
                               color = color,
                               modifier = Modifier.fillMaxSize()
                           ) {}
                        }
                    }
                }

                if (pagerState.currentPage < 3) {
                    Button(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Text("Next")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            userPreferencesRepository.setShouldShowWalkthrough(false)
                            onFinish()
                        }
                    }) {
                        Text("Get Started")
                        Icon(Icons.Default.Done, contentDescription = null)
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
                    title = "Capabilities & Limitations",
                    description = "Vaultio excels at organization and rapid scanning. Please note that data accuracy depends on the API provider, and some rare variants may require manual adjustment.",
                    imageText = "🚀"
                )
            }
        }
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
