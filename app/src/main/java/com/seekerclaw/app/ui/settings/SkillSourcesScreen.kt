package com.seekerclaw.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seekerclaw.app.config.ConfigManager
import com.seekerclaw.app.config.SkillSource
import com.seekerclaw.app.ui.components.cornerGlowBorder
import com.seekerclaw.app.ui.theme.RethinkSans
import com.seekerclaw.app.ui.theme.SeekerClawColors
import com.seekerclaw.app.util.Analytics
import com.seekerclaw.app.ui.components.CardSurface
import com.seekerclaw.app.ui.components.InfoRow
import com.seekerclaw.app.ui.components.SeekerClawSwitch

@Composable
fun SkillSourcesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val shape = remember { RoundedCornerShape(SeekerClawColors.CornerRadius) }
    var sources by remember { mutableStateOf<List<SkillSource>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sources = ConfigManager.loadSkillSources(context)
    }

    // Add/Edit dialog
    if (showAddDialog) {
        val isEditing = editingIndex >= 0
        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingIndex = -1 },
            title = {
                Text(
                    text = if (isEditing) "Edit Skill Source" else "Add Skill Source",
                    fontFamily = RethinkSans,
                    fontWeight = FontWeight.Bold,
                    color = SeekerClawColors.TextPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Source Name", fontFamily = RethinkSans, fontSize = 12.sp) },
                        placeholder = { Text("e.g., My Catalog", fontFamily = RethinkSans, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = RethinkSans,
                            fontSize = 14.sp,
                            color = SeekerClawColors.TextPrimary,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SeekerClawColors.Primary,
                            unfocusedBorderColor = SeekerClawColors.TextDim.copy(alpha = 0.3f),
                            cursorColor = SeekerClawColors.Primary,
                        ),
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("Catalog URL", fontFamily = RethinkSans, fontSize = 12.sp) },
                        placeholder = { Text("https://api.example.com/v1", fontFamily = RethinkSans, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = SeekerClawColors.TextPrimary,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SeekerClawColors.Primary,
                            unfocusedBorderColor = SeekerClawColors.TextDim.copy(alpha = 0.3f),
                            cursorColor = SeekerClawColors.Primary,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = editName.trim()
                    val url = editUrl.trim().trimEnd('/')
                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        val newSource = SkillSource(name = name, url = url, enabled = true)
                        val mutable = sources.toMutableList()
                        if (isEditing && editingIndex in mutable.indices) {
                            mutable[editingIndex] = newSource
                        } else {
                            mutable.add(newSource)
                        }
                        sources = mutable
                        ConfigManager.saveSkillSources(context, mutable)
                        Analytics.featureUsed("skill_source_${if (isEditing) "edited" else "added"}")
                        Toast.makeText(context, "Source ${if (isEditing) "updated" else "added"}", Toast.LENGTH_SHORT).show()
                    }
                    showAddDialog = false
                    editingIndex = -1
                }) {
                    Text(
                        text = if (isEditing) "Save" else "Add",
                        fontFamily = RethinkSans,
                        fontWeight = FontWeight.Bold,
                        color = SeekerClawColors.Primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingIndex = -1
                }) {
                    Text("Cancel", fontFamily = RethinkSans, color = SeekerClawColors.TextDim)
                }
            },
            containerColor = SeekerClawColors.Surface,
            shape = shape,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeekerClawColors.Background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u2190",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                color = SeekerClawColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClickLabel = "Back") { onBack() }
                    .padding(8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Skill Sources",
                fontFamily = RethinkSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SeekerClawColors.TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                editName = ""
                editUrl = ""
                editingIndex = -1
                showAddDialog = true
            }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add source",
                    tint = SeekerClawColors.Primary,
                )
            }
        }

        // Content
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (sources.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No skill sources configured.\nTap + to add your first catalog.",
                            fontFamily = RethinkSans,
                            fontSize = 14.sp,
                            color = SeekerClawColors.TextDim,
                            lineHeight = 22.sp,
                        )
                    }
                }
            } else {
                items(sources.withIndex().toList(), key = { it.index }) { (index, source) ->
                    SkillSourceCard(
                        source = source,
                        shape = shape,
                        onToggle = { enabled ->
                            val mutable = sources.toMutableList()
                            if (index in mutable.indices) {
                                mutable[index] = source.copy(enabled = enabled)
                                sources = mutable
                                ConfigManager.saveSkillSources(context, mutable)
                            }
                        },
                        onEdit = {
                            editName = source.name
                            editUrl = source.url
                            editingIndex = index
                            showAddDialog = true
                        },
                        onDelete = {
                            val mutable = sources.toMutableList()
                            if (index in mutable.indices) {
                                mutable.removeAt(index)
                                sources = mutable
                                ConfigManager.saveSkillSources(context, mutable)
                                Analytics.featureUsed("skill_source_deleted")
                                Toast.makeText(context, "Source removed", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "The marketplace will search all enabled skill catalogs and aggregate the results.",
                    fontFamily = RethinkSans,
                    fontSize = 12.sp,
                    color = SeekerClawColors.TextDim,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun SkillSourceCard(
    source: SkillSource,
    shape: RoundedCornerShape,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    CardSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    fontFamily = RethinkSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (source.enabled) SeekerClawColors.TextPrimary else SeekerClawColors.TextDim,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = source.url,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SeekerClawColors.TextDim,
                )
            }
            Spacer(Modifier.width(8.dp))
            SeekerClawSwitch(
                checked = source.enabled,
                onCheckedChange = onToggle,
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit source",
                    tint = SeekerClawColors.TextDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete source",
                    tint = SeekerClawColors.Error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "Delete Source",
                    fontFamily = RethinkSans,
                    fontWeight = FontWeight.Bold,
                    color = SeekerClawColors.Error,
                )
            },
            text = {
                Text(
                    "Remove \"${source.name}\" from your skill sources?",
                    fontFamily = RethinkSans,
                    fontSize = 13.sp,
                    color = SeekerClawColors.TextSecondary,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        "Delete",
                        fontFamily = RethinkSans,
                        fontWeight = FontWeight.Bold,
                        color = SeekerClawColors.Error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        "Cancel",
                        fontFamily = RethinkSans,
                        color = SeekerClawColors.TextDim,
                    )
                }
            },
            containerColor = SeekerClawColors.Surface,
            shape = shape,
        )
    }
}
