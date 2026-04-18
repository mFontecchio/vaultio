package com.mrhayami.vaultio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.ui.theme.VaultioTheme

@Composable
fun CardBadge(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    isRotated: Boolean = false
) {
    // Better contrast heuristic: values above 0.45 luminance generally prefer black text for accessibility
    val contentColor = if (containerColor.luminance() > 0.45f) Color.Black else Color.White

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = modifier.sizeIn(minWidth = 16.dp, minHeight = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(
                horizontal = if (text.length > 1) 4.dp else 0.dp,
                vertical = 1.dp
            )
        ) {
            Text(
                text = text,
                modifier = if (isRotated) Modifier.rotate(180f) else Modifier,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 10.sp,
                    letterSpacing = (-0.2).sp
                )
            )
        }
    }
}

@Composable
fun CardAttributeBadges(
    finish: String,
    printing: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Finish Badges
        when (finish) {
            PricingUtils.FINISH_HOLOFOIL -> {
                CardBadge("F", Color(0xFF6200EE), Modifier.padding(end = 2.dp))
            }
            PricingUtils.FINISH_REVERSE_HOLO -> {
                CardBadge("F", Color(0xFF03DAC6), Modifier.padding(end = 2.dp), isRotated = true)
            }
            PricingUtils.FINISH_TEXTURED -> {
                CardBadge("T", Color(0xFFFF0266), Modifier.padding(end = 2.dp))
            }
            PricingUtils.FINISH_GOLD -> {
                CardBadge("G", Color(0xFFFFD700), Modifier.padding(end = 2.dp))
            }
        }

        // Printing Badges
        when (printing) {
            PricingUtils.PRINTING_1ST_EDITION -> {
                CardBadge("1st", Color(0xFFE91E63), Modifier.padding(end = 2.dp))
            }
            PricingUtils.PRINTING_SHADOWLESS -> {
                CardBadge("SHDW", Color(0xFF607D8B), Modifier.padding(end = 2.dp))
            }
            PricingUtils.PRINTING_PROMO -> {
                CardBadge("P", Color(0xFF4CAF50), Modifier.padding(end = 2.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardAttributeBadgesPreview() {
    VaultioTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Text("Refined & Compact Finishes", style = MaterialTheme.typography.titleSmall)
            Row {
                CardAttributeBadges(finish = PricingUtils.FINISH_HOLOFOIL, printing = "")
                CardAttributeBadges(finish = PricingUtils.FINISH_REVERSE_HOLO, printing = "")
                CardAttributeBadges(finish = PricingUtils.FINISH_TEXTURED, printing = "")
                CardAttributeBadges(finish = PricingUtils.FINISH_GOLD, printing = "")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Refined Printings", style = MaterialTheme.typography.titleSmall)
            Row {
                CardAttributeBadges(finish = "", printing = PricingUtils.PRINTING_1ST_EDITION)
                CardAttributeBadges(finish = "", printing = PricingUtils.PRINTING_SHADOWLESS)
                CardAttributeBadges(finish = "", printing = PricingUtils.PRINTING_PROMO)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Compact Combinations", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                CardAttributeBadges(
                    finish = PricingUtils.FINISH_HOLOFOIL, 
                    printing = PricingUtils.PRINTING_1ST_EDITION
                )
                CardAttributeBadges(
                    finish = PricingUtils.FINISH_REVERSE_HOLO, 
                    printing = PricingUtils.PRINTING_SHADOWLESS
                )
                CardAttributeBadges(
                    finish = PricingUtils.FINISH_GOLD, 
                    printing = PricingUtils.PRINTING_PROMO
                )
            }
        }
    }
}
