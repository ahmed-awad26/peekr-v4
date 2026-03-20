package com.peekr.data.remote.youtube

import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.entities.PostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeClient @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val postDao: PostDao,
    private val accountDao: AccountDao,
    private val logger: AppLogger
) {
    private val BASE_URL = "https://www.googleapis.com/youtube/v3/"

    private val api: YoutubeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApi::class.java)
    }

    suspend fun syncChannels(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyDao.getApiKeyByPlatform("youtube")?.keyValue
            if (apiKey.isNullOrEmpty()) {
                logger.warning("YouTube API Key غير موجود", "youtube")
                return@withContext Result.failure(Exception("YouTube API Key غير موجود"))
            }

            val channelAccounts = accountDao.getAllAccountsByPlatformSync("youtube")
            if (channelAccounts.isEmpty()) {
                logger.warning("يوتيوب: لا توجد قنوات مضافة", "youtube")
                return@withContext Result.success(0)
            }

            var totalNewPosts = 0
            channelAccounts.forEach { account ->
                try {
                    val channelId = resolveChannelId(account.accountName.trim(), apiKey)
                    if (channelId != null) {
                        val newPosts = fetchChannelVideos(channelId, apiKey)
                        totalNewPosts += newPosts
                    }
                } catch (e: Exception) {
                    logger.error("فشل جلب فيديوهات القناة: ${account.accountName}", "youtube", e)
                }
            }

            logger.info("يوتيوب: تم جلب $totalNewPosts منشور جديد", "youtube")
            Result.success(totalNewPosts)
        } catch (e: Exception) {
            logger.error("خطأ في مزامنة يوتيوب", "youtube", e)
            Result.failure(e)
        }
    }

    private suspend fun resolveChannelId(channelUrl: String, apiKey: String): String? {
        return try {
            // لو كان Channel ID مباشرة
            if (channelUrl.startsWith("UC")) return channelUrl

            // لو كان @handle
            val handle = when {
                channelUrl.contains("@") -> channelUrl.substringAfterLast("@").substringBefore("/")
                channelUrl.contains("youtube.com/c/") -> channelUrl.substringAfterLast("/c/").substringBefore("/")
                else -> channelUrl
            }

            val response = api.getChannel(forHandle = "@$handle", apiKey = apiKey)
            response.items.firstOrNull()?.id
        } catch (e: Exception) {
            logger.error("فشل في إيجاد Channel ID: $channelUrl", "youtube", e)
            null
        }
    }

    private suspend fun fetchChannelVideos(channelId: String, apiKey: String): Int {
        val response = api.getLatestVideos(channelId = channelId, apiKey = apiKey)
        var count = 0

        response.items.forEach { item ->
            val videoId = item.id.videoId ?: return@forEach
            val publishedAt = parseDate(item.snippet.publishedAt)
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val thumbnailUrl = item.snippet.thumbnails.high?.url
                ?: item.snippet.thumbnails.medium?.url
                ?: item.snippet.thumbnails.default?.url

            val post = PostEntity(
                platformId = "youtube",
                sourceId = channelId,
                sourceName = item.snippet.channelTitle,
                content = item.snippet.title + "\n\n" + item.snippet.description.take(200),
                mediaUrl = thumbnailUrl,
                postUrl = videoUrl,
                timestamp = publishedAt
            )
            postDao.insertPost(post)
            count++
        }
        return count
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
