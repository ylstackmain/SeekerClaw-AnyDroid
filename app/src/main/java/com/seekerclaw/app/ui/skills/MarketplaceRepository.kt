package com.seekerclaw.app.ui.skills

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MarketplaceRepository {
    private const val BASE_URL = "https://api.clawhub.ai/v1"

    suspend fun searchSkills(query: String): Result<List<MarketplaceSkill>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = if (query.isBlank()) {
                "$BASE_URL/skills?limit=50&sort=createdAt"
            } else {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                "$BASE_URL/skills?q=$encodedQuery"
            }
            val (status, body) = httpGet(url)
            if (status !in 200..299) {
                error("Marketplace search failed ($status)")
            }
            val responseObj = JSONObject(body)
            // Handle both direct array responses and wrapped objects with "items" or "skills" keys
            val arr = when {
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
            val skills = mutableListOf<MarketplaceSkill>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                skills.add(parseSkill(obj))
            }
            skills
        }
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

    private fun parseSkill(obj: JSONObject): MarketplaceSkill {
        // ClawHub API v2 fields — nested structure
        val name = obj.optString("name", obj.optString("displayName", ""))
        val description = obj.optString("description", obj.optString("summary", ""))
        val emoji = obj.optString("emoji", "🧩")
        // author may be string or nested object {"name": "..."}
        val authorRaw = obj.opt("author")
        val author = when {
            authorRaw is JSONObject -> authorRaw.optString("name", "")
            authorRaw is String -> authorRaw
            else -> ""
        }
        // image may be string or nested object {"url": "..."}
        val imageRaw = obj.opt("image")
        val imageUrl = when {
            imageRaw is JSONObject -> imageRaw.optString("url", "")
            imageRaw is String -> imageRaw
            else -> ""
        }
        // download may be string or nested object {"url": "..."}
        val downloadRaw = obj.opt("download")
        val downloadUrl = when {
            downloadRaw is JSONObject -> downloadRaw.optString("url", "")
            downloadRaw is String -> downloadRaw
            else -> ""
        }
        // version from latestVersion object or direct version field
        val version = obj.optJSONObject("latestVersion")?.optString("version")
            ?: obj.optString("version", "1.0.0")
        // triggers array
        val triggers = obj.optJSONArray("triggers")?.let { arr ->
            List(arr.length()) { arr.getString(it) }
        } ?: emptyList()
        // requiresEnv array
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
