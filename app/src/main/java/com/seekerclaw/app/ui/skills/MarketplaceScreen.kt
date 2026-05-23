package com.seekerclaw.app.ui.skills

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seekerclaw.app.ui.components.cornerGlowBorder
import com.seekerclaw.app.ui.theme.RethinkSans
import com.seekerclaw.app.ui.theme.SeekerClawColors
import com.seekerclaw.app.util.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MarketplaceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<MarketplaceSkill>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSkill by remember { mutableStateOf<MarketplaceSkill?>(null) }
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    val limit = 50
    val shape = remember { RoundedCornerShape(SeekerClawColors.CornerRadius) }
    val listState = rememberLazyListState()

    fun loadSkills(reset: Boolean = false) {
        scope.launch {
            if (reset) {
                isLoading = true
                offset = 0
                skills = emptyList()
                hasMore = true
            } else {
                isLoadingMore = true
            }
            error = null
            
            val result = MarketplaceRepository.searchSkills(searchQuery, context, limit = limit, offset = offset)
            
            result.onSuccess { newSkills ->
                if (reset) {
                    skills = newSkills
                } else {
                    // Deduplicate
                    val existingIds = skills.map { it.id }.toSet()
                    skills = skills + newSkills.filter { it.id !in existingIds }
                }
                hasMore = newSkills.size >= limit
                offset += newSkills.size
            }.onFailure {
                error = it.message
            }
            
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(searchQuery) {
        // Debounce search
        if (searchQuery.length >= 2 || searchQuery.isEmpty()) {
            loadSkills(reset = true)
        }
    }

    // Infinite scroll logic
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= skills.size - 5 && hasMore && !isLoading && !isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            loadSkills(reset = false)
        }
    }

    val skill = selectedSkill
    if (skill != null) {
        BackHandler { selectedSkill = null }
        MarketplaceDetailScreen(
            skill = skill,
            onBack = { selectedSkill = null },
            onInstall = {
                scope.launch {
                    val result = MarketplaceRepository.downloadSkill(skill.downloadUrl)
                    result.onSuccess { content ->
                        val success = withContext(Dispatchers.IO) {
                            installSkill(context, skill.name, content)
                        }
                        if (success) {
                            Toast.makeText(context, "Skill installed: ${skill.name}", Toast.LENGTH_SHORT).show()
                            Analytics.featureUsed("skill_installed_marketplace")
                        } else {
                            Toast.makeText(context, "Failed to install skill", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure {
                        Toast.makeText(context, "Failed to download skill: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeekerClawColors.Background),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                color = SeekerClawColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClickLabel = "Back") { onBack() }
                    .padding(8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Marketplace",
                fontFamily = RethinkSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SeekerClawColors.TextPrimary,
            )
        }

        // Search Field
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            MarketplaceSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                shape = shape
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SeekerClawColors.Accent)
            }
        } else if (error != null && skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $error", color = SeekerClawColors.Error, fontFamily = RethinkSans)
            }
        } else if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty()) "Explore skills from connected catalogs" else "No skills found",
                    color = SeekerClawColors.TextDim,
                    fontFamily = RethinkSans
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(skills, key = { it.id }) { skill ->
                    MarketplaceSkillCard(
                        skill = skill,
                        shape = shape,
                        onClick = { selectedSkill = skill },
                        onInstall = {
                            scope.launch {
                                val result = MarketplaceRepository.downloadSkill(skill.downloadUrl)
                                result.onSuccess { content ->
                                    val success = withContext(Dispatchers.IO) {
                                        installSkill(context, skill.name, content)
                                    }
                                    if (success) {
                                        Toast.makeText(context, "Skill installed: ${skill.name}", Toast.LENGTH_SHORT).show()
                                        Analytics.featureUsed("skill_installed_marketplace")
                                    } else {
                                        Toast.makeText(context, "Failed to install skill", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Failed to download skill: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                
                if (isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SeekerClawColors.Accent, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun installSkill(context: android.content.Context, name: String, content: String): Boolean {
    return try {
        val workspaceDir = File(context.filesDir, "workspace")
        val skillsDir = File(workspaceDir, "skills").apply { mkdirs() }
        
        // Clean name for directory
        val cleanName = name.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
        val skillDir = File(skillsDir, cleanName).apply { mkdirs() }
        val skillFile = File(skillDir, "SKILL.md")
        
        skillFile.writeText(content)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun MarketplaceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    shape: RoundedCornerShape,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SeekerClawColors.Surface, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "⌕",
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            color = SeekerClawColors.TextDim,
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search marketplace skills...",
                    fontFamily = RethinkSans,
                    fontSize = 14.sp,
                    color = SeekerClawColors.TextDim,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(SeekerClawColors.Accent),
                textStyle = TextStyle(
                    fontFamily = RethinkSans,
                    fontSize = 14.sp,
                    color = SeekerClawColors.TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "✕",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = SeekerClawColors.TextDim,
                modifier = Modifier.clickable(onClickLabel = "Clear search") { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun MarketplaceSkillCard(
    skill: MarketplaceSkill,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    onInstall: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SeekerClawColors.Surface, shape)
            .cornerGlowBorder()
            .clickable(onClickLabel = "View ${skill.name}", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkillAvatar(skill = skill, size = 44, shape = shape)
        
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = skill.name,
                    fontFamily = RethinkSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = SeekerClawColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (skill.source.isNotEmpty()) {
                        Text(
                            text = skill.source,
                            fontFamily = RethinkSans,
                            fontSize = 11.sp,
                            color = SeekerClawColors.Accent,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "v${skill.version}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SeekerClawColors.TextDim,
                    )
                }
            }
            if (skill.author.isNotEmpty()) {
                Text(
                    text = "by ${skill.author}",
                    fontFamily = RethinkSans,
                    fontSize = 11.sp,
                    color = SeekerClawColors.Accent,
                )
            }
            if (skill.description.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = skill.description,
                    fontFamily = RethinkSans,
                    fontSize = 13.sp,
                    color = SeekerClawColors.TextDim,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onInstall,
            colors = ButtonDefaults.buttonColors(
                containerColor = SeekerClawColors.Primary.copy(alpha = 0.15f),
                contentColor = SeekerClawColors.Primary,
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("GET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
