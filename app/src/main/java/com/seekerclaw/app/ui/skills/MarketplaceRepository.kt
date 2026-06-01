package com.seekerclaw.app.ui.skills

import android.content.Context
import com.seekerclaw.app.config.ConfigManager
import com.seekerclaw.app.config.SkillSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MarketplaceRepository {

    suspend fun searchSkills(query: String, context: Context, limit: Int = 50, offset: Int = 0): Result<List<MarketplaceSkill>> = withContext(Dispatchers.IO) {
        runCatching {
            val sources = ConfigManager.loadSkillSources(context).filter { it.enabled }
            if (sources.isEmpty()) return@runCatching emptyList()

            // Search all enabled sources in parallel
            val deferredResults = sources.map { source ->
                async {
                    searchSource(source, query, limit, offset)
                }
            }
            val allResults = deferredResults.awaitAll().flatten()
            // Deduplicate by skill ID (last source wins on conflict)
            allResults.associateBy { it.id }.values.toList()
        }
    }

    private suspend fun searchSource(source: SkillSource, query: String, limit: Int, offset: Int): List<MarketplaceSkill> {
        return runCatching {
            val cleanUrl = source.url.trimEnd('/')
            val url = if (query.isBlank()) {
                "$cleanUrl/skills?limit=$limit&offset=$offset&sort=createdAt"
            } else {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                "$cleanUrl/skills?q=$encodedQuery&limit=$limit&offset=$offset"
            }
            val (status, body) = httpGet(url)
            if (status !in 200..299) {
                return@runCatching emptyList()
            }
            
            // Handle both JSONArray and JSONObject response shapes
            val responseText = body.trim()
            val arr = if (responseText.startsWith("[")) {
                JSONArray(responseText)
            } else {
                val responseObj = JSONObject(responseText)
                when {
                    responseObj.has("items") -> responseObj.getJSONArray("items")
                    responseObj.has("skills") -> responseObj.getJSONArray("skills")
                    responseObj.has("data") -> {
                        val data = responseObj.get("data")
                        if (data is JSONArray) data
                        else if (data is JSONObject && data.has("items")) data.getJSONArray("items")
                        else if (data is JSONObject && data.has("skills")) data.getJSONArray("skills")
                        else JSONArray()
                    }
                    else -> JSONArray()
                }
            }
            
            val skills = mutableListOf<MarketplaceSkill>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                skills.add(parseSkill(obj, source.name))
            }
            skills
        }.getOrDefault(emptyList())
    }

    suspend fun downloadSkill(downloadUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = httpGet(downloadUrl)
            if (status !in 200..299) {
                error("Skill download failed ($status)")
            }
            body
        }
    }

    private fun parseSkill(obj: JSONObject, sourceName: String): MarketplaceSkill {
        val name = obj.optString("name", obj.optString("displayName", ""))
        val description = obj.optString("description", obj.optString("summary", ""))
        val emoji = obj.optString("emoji", "🧩")
        val authorRaw = obj.opt("author")
        val author = when {
            authorRaw is JSONObject -> authorRaw.optString("name", "")
            authorRaw is String -> authorRaw
            else -> ""
        }
        val imageRaw = obj.opt("image")
        val imageUrl = when {
            imageRaw is JSONObject -> imageRaw.optString("url", "")
            imageRaw is String -> imageRaw
            else -> ""
        }
        val downloadRaw = obj.opt("download")
        val downloadUrl = when {
            downloadRaw is JSONObject -> downloadRaw.optString("url", "")
            downloadRaw is String -> downloadRaw
            else -> ""
        }
        val version = obj.optJSONObject("latestVersion")?.optString("version")
            ?: obj.optString("version", "1.0.0")
        val triggers = obj.optJSONArray("triggers")?.let { arr ->
            List(arr.length()) { arr.getString(it) }
        } ?: emptyList()
        val requiresEnv = obj.optJSONArray("requiresEnv")?.let { arr ->
            List(arr.length()) { arr.getString(it) }
        } ?: emptyList()
        return MarketplaceSkill(
            id = obj.optString("slug", obj.optString("id", "")),
            name = name,
            description = description,
            version = version,
            emoji = emoji,
            author = author,
            imageUrl = imageUrl,
            downloadUrl = downloadUrl,
            triggers = triggers,
            requiresEnv = requiresEnv,
            source = sourceName,
        )
    }

    private fun httpGet(url: String): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SeekerClaw/Android")
        }

        return try {
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            status to body
        } catch (e: Exception) {
            -1 to (e.message ?: "Unknown error")
        } finally {
            conn.disconnect()
        }
    }
}
