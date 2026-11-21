package com.futurecode.voicelockscreen.api.wallpaper

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Lock screen themes API (wallpaper categories and wallpapers). */
interface WallpaperApi {

    @GET("api/v1/wallpapers/categories")
    suspend fun categories(@Query("page") page: Int = 1): PagedResponse<WallpaperCategory>

    @GET("api/v1/wallpapers")
    suspend fun wallpapers(
        @Query("category_id") categoryId: Int,
        @Query("page") page: Int = 1
    ): PagedResponse<Wallpaper>

    companion object {
        private const val BASE_URL = "https://app-locker-themes-production.up.railway.app/"
        private const val TIMEOUT_SECONDS = 20L

        /** Separate client: the app's other API (NetworkModule) uses a different host. */
        val instance: WallpaperApi by lazy {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WallpaperApi::class.java)
        }
    }
}

data class PagedResponse<T>(
    @SerializedName("data") val data: List<T>?,
    @SerializedName("meta") val meta: PageMeta?
)

data class PageMeta(
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("has_next") val hasNext: Boolean
)

data class WallpaperCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("thumbnail_path") val thumbnailPath: String?,
    @SerializedName("artwork_count") val artworkCount: Int
)

data class Wallpaper(
    @SerializedName("id") val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("file_path") val fileUrl: String
)
