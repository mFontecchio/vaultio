package com.mrhayami.vaultio.ui.grading

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.local.CardGradeEntity
import com.mrhayami.vaultio.data.repository.GeminiNanoClient
import com.mrhayami.vaultio.ui.components.MetadataModal
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradingScreen(
    state: GradingViewState,
    userCardId: Long,
    onEvent: (GradingEvent) -> Unit,
    sideEffects: Flow<GradingEffect>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showMetadataModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sideEffects.collect { effect ->
            when (effect) {
                is GradingEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }

                is GradingEffect.Navigation.GoBack -> onNavigateBack()
                is GradingEffect.Navigation.GoToResult -> { /* Navigate to result */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Grading Assistant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.gradeResult == null) {
                // Initial Analysis State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image Preview
                    state.capturedImage?.let { bitmap ->
                        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .height(300.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Captured Card",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Ready to Analyze",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Our AI will evaluate centering, corners, and surface.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Model Status Info
                    ModelStatusIndicator(
                        status = state.modelStatus,
                        onDownloadClick = { onEvent(GradingEvent.DownloadModel) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val bmp = state.capturedImage
                            if (bmp != null) {
                                onEvent(GradingEvent.StartAnalysis(bmp, userCardId))
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please capture an image first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = !state.isAnalyzing && state.capturedImage != null && state.modelStatus is GeminiNanoClient.ModelStatus.Ready
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze Condition")
                        }
                    }
                }
            } else {
                // The "Digital Slab" Results View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

//                    ThreeDCard(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .aspectRatio(0.718f)
//                            .padding(horizontal = 8.dp)
//                    ) { _, _ ->
//                        DigitalGradeSlab(
//                            grade = state.gradeResult,
//                            image = state.capturedImage
//                        )
//                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.718f)
                            .padding(horizontal = 16.dp)
                    ) {
                        DigitalGradeSlab(
                            grade = state.gradeResult,
                            image = state.capturedImage
                        )
                    }

                    GradeDetailedBreakdown(state.gradeResult)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (userCardId == -1L && state.pendingCard != null) {
                                showMetadataModal = true
                            } else {
                                onEvent(GradingEvent.SaveGrade(userCardId))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .height(56.dp)
                    ) {
                        Text(if (userCardId == -1L) "Save to Collection" else "Save Grade")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showMetadataModal && state.pendingCard != null && state.gradeResult != null) {
            val score = state.gradeResult.overallScore
            val mappedCondition = when {
                score >= 8.5 -> PricingUtils.CONDITION_NM
                score >= 7.0 -> PricingUtils.CONDITION_LP
                score >= 5.0 -> PricingUtils.CONDITION_MP
                score >= 3.0 -> PricingUtils.CONDITION_HP
                else -> PricingUtils.CONDITION_DMG
            }

            ModalBottomSheet(
                onDismissRequest = { showMetadataModal = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                MetadataModal(
                    card = state.pendingCard,
                    folders = state.folders,
                    initialCondition = mappedCondition,
                    onConfirm = { q, c, p, f, fIds ->
                        showMetadataModal = false
                        onEvent(GradingEvent.SaveGradeWithMetadata(q, c, p, f, fIds))
                    },
                    onBack = { showMetadataModal = false }
                )
            }
        }
    }
}

@Composable
fun ModelStatusIndicator(
    status: GeminiNanoClient.ModelStatus,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when (status) {
                        is GeminiNanoClient.ModelStatus.Ready -> Color(0xFF4CAF50)
                        is GeminiNanoClient.ModelStatus.Downloading -> MaterialTheme.colorScheme.primary
                        is GeminiNanoClient.ModelStatus.Unavailable -> MaterialTheme.colorScheme.error
                        is GeminiNanoClient.ModelStatus.Error -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (status) {
                        is GeminiNanoClient.ModelStatus.Ready -> "AI Model Ready"
                        is GeminiNanoClient.ModelStatus.Downloading -> "Downloading AI Model..."
                        is GeminiNanoClient.ModelStatus.Unavailable -> "AI Model Unavailable"
                        is GeminiNanoClient.ModelStatus.Error -> "AI Error: ${status.message}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            when (status) {
                is GeminiNanoClient.ModelStatus.Downloading -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(status.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                is GeminiNanoClient.ModelStatus.Unavailable -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to check for updates or download model (requires ~1GB).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Check / Download Model")
                    }
                }

                is GeminiNanoClient.ModelStatus.Error -> {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun DigitalGradeSlab(
    grade: CardGradeEntity,
    image: Bitmap?
) {
    val slabGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2C2C2C),
                Color(0xFF1A1A1A)
            )
        )
    }
    val imageBitmap = remember(image) { image?.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(0.718f)
            .background(brush = slabGradient, shape = RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF444444), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Grade Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gradeId = remember(grade.id) { grade.id.toString().takeLast(6) }
                    val overallScoreStr = remember(grade.overallScore) {
                        String.format(
                            Locale.US,
                            "%.1f",
                            grade.overallScore
                        )
                    }
                    
                    Column {
                        Text(
                            "VAULTIO AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text(
                            "ESTIMATED GRADE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            "ID: #$gradeId",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                    }

                    Text(
                        text = overallScoreStr,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The Card Image
            Box(
                modifier = Modifier
                    .aspectRatio(0.718f)
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun GradeDetailedBreakdown(grade: CardGradeEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "Technical Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    GradeSubScore("Centering", grade.centeringScore)
                    GradeSubScore("Corners", grade.cornersScore)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    GradeSubScore("Edges", grade.edgesScore)
                    GradeSubScore("Surface", grade.surfaceScore)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val annotatedReasoning = rememberMarkdownText(grade.reasoning)
                    Text(
                        text = annotatedReasoning,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GradeSubScore(label: String, score: Double) {
    val formattedScore = remember(score) { String.format(Locale.US, "%.1f", score) }
    val scoreColor = remember(score) { if (score >= 9.0) Color(0xFF4CAF50) else Color.Unspecified }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formattedScore,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (scoreColor != Color.Unspecified) scoreColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun rememberMarkdownText(text: String): AnnotatedString {
    return remember(text) {
        buildAnnotatedString {
            val boldRegex = Regex("""\*\*(.*?)\*\*""")
            var lastIndex = 0

            boldRegex.findAll(text).forEach { matchResult ->
                append(text.substring(lastIndex, matchResult.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(matchResult.groupValues[1])
                }
                lastIndex = matchResult.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}


@Preview
@Composable
private fun GradePreview() {
    DigitalGradeSlab(
        grade = CardGradeEntity(
            id = 0,
            userCardId = 1234,
            overallScore = 9.2,
            centeringScore = 8.5,
            cornersScore = 9.0,
            edgesScore = 9.5,
            surfaceScore = 9.0,
            reasoning = "Perfect",
            timestamp = System.currentTimeMillis()
        ),
        image = null
    )
}

@Preview
@Composable
private fun GradingScreenPreview() {
    GradingScreen(
        state = GradingViewState(),
        userCardId = 1234,
        onEvent = {},
        sideEffects = flowOf(),
        onNavigateBack = {}
    )
}

@Preview
@Composable
private fun GradeSubScorePreview() {
    GradeSubScore("Centering", 8.5)
}

@Preview
@Composable
private fun MarkdownTextPreview() {
    VaultioTheme {
        Surface {
            val text = "This is **bold** text and this is regular text with **more bold**."
            val annotated = rememberMarkdownText(text)
            Text(
                text = annotated,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GradedCardScreenPreview() {
    val longReasoning = """
        The card shows **excellent centering** on both the front and back (estimated 50/50). 
        The corners are **exceptionally sharp**, though a microscopic speck of whitening is visible 
        on the bottom-right corner under 10x magnification. 
        
        The surface is **immaculate**, with no visible scratches, print lines, or dimples detected. 
        The edges are **consistently smooth** with no silvering or nicks along the perimeter. 
        
        Overall, this card represents a **high-end Mint condition** specimen, likely to achieve 
        a top-tier grade from professional services. The technical sub-grades support the 
        strong overall evaluation of 9.2.
    """.trimIndent()

    VaultioTheme {
        GradingScreen(
            state = GradingViewState(
                gradeResult = CardGradeEntity(
                    id = 1,
                    userCardId = 123,
                    overallScore = 9.2,
                    centeringScore = 9.5,
                    cornersScore = 9.0,
                    edgesScore = 9.0,
                    surfaceScore = 9.5,
                    reasoning = longReasoning,
                    timestamp = System.currentTimeMillis()
                ),
                capturedImage = null,
                modelStatus = GeminiNanoClient.ModelStatus.Ready
            ),
            userCardId = 123,
            onEvent = {},
            sideEffects = flowOf(),
            onNavigateBack = {}
        )
    }
}

@Preview
@Composable
private fun ModelStatusIndicatorPreview() {
    VaultioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModelStatusIndicator(
                status = GeminiNanoClient.ModelStatus.Ready,
                onDownloadClick = {}
            )
            ModelStatusIndicator(
                status = GeminiNanoClient.ModelStatus.Downloading(0.5f),
                onDownloadClick = {}
            )
            ModelStatusIndicator(
                status = GeminiNanoClient.ModelStatus.Unavailable,
                onDownloadClick = {}
            )
            ModelStatusIndicator(
                status = GeminiNanoClient.ModelStatus.Error("Connection failed"),
                onDownloadClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun GradeDetailedBreakdownPreview() {
    GradeDetailedBreakdown(
        grade = CardGradeEntity(
            id = 0,
            userCardId = 1234,
            overallScore = 9.2,
            centeringScore = 8.5,
            cornersScore = 9.0,
            edgesScore = 9.5,
            surfaceScore = 9.0,
            reasoning = "Perfect",
            timestamp = System.currentTimeMillis()
        ),
    )
}