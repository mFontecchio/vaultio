package com.mrhayami.vaultio.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    val id: Long,
    @param:Json(name = "tag_name") val tagName: String,
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @param:Json(name = "published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    val id: Long,
    val name: String,
    val size: Long = 0L,
    @param:Json(name = "browser_download_url") val browserDownloadUrl: String,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "content_type") val contentType: String? = null
)

interface GitHubReleasesApi {

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("User-Agent") userAgent: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<GitHubRelease>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String,
        @Header("User-Agent") userAgent: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<GitHubRelease>
}
