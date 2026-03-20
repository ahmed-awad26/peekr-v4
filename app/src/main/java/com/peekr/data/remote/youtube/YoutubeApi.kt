package com.peekr.data.remote.youtube

import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApi {

    // جلب معلومات القناة
    @GET("channels")
    suspend fun getChannel(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("forHandle") forHandle: String? = null,
        @Query("id") id: String? = null,
        @Query("key") apiKey: String
    ): ChannelResponse

    // جلب آخر فيديوهات قناة
    @GET("search")
    suspend fun getLatestVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("order") order: String = "date",
        @Query("maxResults") maxResults: Int = 20,
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): SearchResponse

    // جلب تفاصيل فيديو
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,statistics",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): VideoDetailsResponse
}

// ==============================
// Data Classes
// ==============================

data class ChannelResponse(
    val items: List<ChannelItem> = emptyList()
)

data class ChannelItem(
    val id: String,
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails? = null
)

data class ChannelSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails
)

data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists
)

data class RelatedPlaylists(
    val uploads: String
)

data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null
)

data class SearchItem(
    val id: SearchItemId,
    val snippet: SearchSnippet
)

data class SearchItemId(
    val videoId: String? = null
)

data class SearchSnippet(
    val title: String,
    val description: String,
    val publishedAt: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val default: ThumbnailItem? = null,
    val medium: ThumbnailItem? = null,
    val high: ThumbnailItem? = null
)

data class ThumbnailItem(
    val url: String
)

data class VideoDetailsResponse(
    val items: List<VideoDetailItem> = emptyList()
)

data class VideoDetailItem(
    val id: String,
    val snippet: SearchSnippet,
    val statistics: VideoStatistics? = null
)

data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null
)
