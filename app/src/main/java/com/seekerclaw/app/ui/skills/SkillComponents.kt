package com.seekerclaw.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.seekerclaw.app.ui.theme.SeekerClawColors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.seekerclaw.app.ui.components.CardSurface
import com.seekerclaw.app.ui.theme.RethinkSans

@Composable
fun InfoSection(
    label: String,
    content: @Composable () -> Unit,
) {
    CardSurface {
        Text(
            text = label,
            fontFamily = RethinkSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SeekerClawColors.TextDim,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun SkillAvatar(
    imageUrl: String,
    emoji: String,
    name: String,
    size: Int = 44,
    shape: RoundedCornerShape = RoundedCornerShape(SeekerClawColors.CornerRadius),
    emojiFontSize: Int = 22,
) {
    if (imageUrl.isNotEmpty()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(shape),
            loading = {
                EmojiAvatar(
                    emoji = emoji,
                    size = size,
                    shape = shape,
                    emojiFontSize = emojiFontSize,
                )
            },
            error = {
                EmojiAvatar(
                    emoji = emoji,
                    size = size,
                    shape = shape,
                    emojiFontSize = emojiFontSize,
                )
            },
        )
    } else {
        EmojiAvatar(
            emoji = emoji,
            size = size,
            shape = shape,
            emojiFontSize = emojiFontSize,
        )
    }
}

@Composable
fun EmojiAvatar(
    emoji: String,
    size: Int,
    shape: RoundedCornerShape,
    emojiFontSize: Int,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .background(SeekerClawColors.SurfaceHighlight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji.ifEmpty { "⚡" },
            fontSize = emojiFontSize.sp,
        )
    }
}

@Composable
fun SkillAvatar(
    skill: SkillInfo,
    size: Int = 44,
    shape: RoundedCornerShape = RoundedCornerShape(SeekerClawColors.CornerRadius),
    emojiFontSize: Int = 22,
) {
    SkillAvatar(
        imageUrl = skill.imageUrl,
        emoji = skill.emoji,
        name = skill.name,
        size = size,
        shape = shape,
        emojiFontSize = emojiFontSize
    )
}

@Composable
fun SkillAvatar(
    skill: MarketplaceSkill,
    size: Int = 44,
    shape: RoundedCornerShape = RoundedCornerShape(SeekerClawColors.CornerRadius),
    emojiFontSize: Int = 22,
) {
    SkillAvatar(
        imageUrl = skill.imageUrl,
        emoji = skill.emoji,
        name = skill.name,
        size = size,
        shape = shape,
        emojiFontSize = emojiFontSize
    )
}
