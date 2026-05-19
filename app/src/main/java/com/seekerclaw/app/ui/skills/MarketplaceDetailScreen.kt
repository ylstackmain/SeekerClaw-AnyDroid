package com.seekerclaw.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seekerclaw.app.ui.theme.RethinkSans
import com.seekerclaw.app.ui.theme.SeekerClawColors

@Composable
fun MarketplaceDetailScreen(
    skill: MarketplaceSkill,
    onBack: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeekerClawColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "← Marketplace",
                fontFamily = RethinkSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SeekerClawColors.Primary,
                modifier = Modifier.clickable(onClickLabel = "Back to marketplace", onClick = onBack),
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = SeekerClawColors.CardBorder,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: avatar + name + version
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkillAvatar(skill = skill, size = 56, emojiFontSize = 32)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skill.name,
                        fontFamily = RethinkSans,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeekerClawColors.TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "v${skill.version}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = SeekerClawColors.TextDim,
                        )
                        if (skill.author.isNotEmpty()) {
                            Text(
                                text = " • ",
                                color = SeekerClawColors.TextDim,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "by ${skill.author}",
                                fontFamily = RethinkSans,
                                fontSize = 12.sp,
                                color = SeekerClawColors.Accent,
                            )
                        }
                    }
                }
                
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SeekerClawColors.Primary.copy(alpha = 0.15f),
                        contentColor = SeekerClawColors.Primary,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("GET", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Description
            if (skill.description.isNotEmpty()) {
                InfoSection(label = "DESCRIPTION") {
                    Text(
                        text = skill.description,
                        fontFamily = RethinkSans,
                        fontSize = 14.sp,
                        color = SeekerClawColors.TextPrimary,
                        lineHeight = 22.sp,
                    )
                }
            }

            // Triggers
            if (skill.triggers.isNotEmpty()) {
                InfoSection(label = "TRIGGERS") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        skill.triggers.forEach { trigger ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SeekerClawColors.Accent),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = trigger,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = SeekerClawColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // Requirements
            if (skill.requiresEnv.isNotEmpty()) {
                InfoSection(label = "REQUIREMENTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "This skill requires the following environment variables:",
                            fontFamily = RethinkSans,
                            fontSize = 13.sp,
                            color = SeekerClawColors.TextDim,
                        )
                        skill.requiresEnv.forEach { env ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(SeekerClawColors.TextDim),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = env,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = SeekerClawColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
