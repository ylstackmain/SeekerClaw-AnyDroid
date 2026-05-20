package com.seekerclaw.app.ui.skills

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceSkill(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val emoji: String,
    val author: String = "",
    val imageUrl: String = "",
    val downloadUrl: String = "",
    val triggers: List<String> = emptyList(),
    val requiresEnv: List<String> = emptyList(),
    val source: String = "",
)
