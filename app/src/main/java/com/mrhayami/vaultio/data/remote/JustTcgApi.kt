package com.mrhayami.vaultio.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class JustTcgPrice(
    val market: Double?,
    val low: Double?,
    val mid: Double?,
    val high: Double?
)

@JsonClass(generateAdapter = true)
data class JustTcgVariant(
    val id: String,
    val name: String? = null,
    val printing: String,
    val condition: String,
    val price: Double? = null,
    val avgPrice: Double? = null,
    val minPrice7d: Double? = null,
    val maxPrice7d: Double? = null,
    val prices: JustTcgPrice? = null
)

@JsonClass(generateAdapter = true)
data class JustTcgCard(
    val id: String,
    val name: String,
    val tcgplayerId: String?,
    val variants: List<JustTcgVariant> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JustTcgMetadata(
    val apiRequestLimit: Int,
    val apiDailyLimit: Int,
    val apiRateLimit: Int,
    val apiRequestsUsed: Int,
    val apiDailyRequestsUsed: Int,
    val apiRequestsRemaining: Int,
    val apiDailyRequestsRemaining: Int,
    val apiPlan: String
)

@JsonClass(generateAdapter = true)
data class JustTcgResponse<T>(
    val data: T,
    @Json(name = "_metadata") val metadata: JustTcgMetadata? = null
)

@JsonClass(generateAdapter = true)
data class JustTcgBatchRequestItem(
    val tcgplayerId: String,
    val printing: String? = null,
    val condition: String? = null
)

interface JustTcgApi {
    @GET("cards")
    suspend fun getCardByTcgPlayerId(
        @Header("x-api-key") apiKey: String,
        @Query("tcgplayerId") tcgplayerId: String
    ): JustTcgResponse<List<JustTcgCard>>

    @GET("cards")
    suspend fun searchCards(
        @Header("x-api-key") apiKey: String,
        @Query("game") game: String = "pokemon",
        @Query("q") query: String,
        @Query("number") number: String? = null,
        @Query("set") set: String? = null,
        @Query("limit") limit: Int? = 5
    ): JustTcgResponse<List<JustTcgCard>>

    @POST("cards")
    suspend fun getCardsBatch(
        @Header("x-api-key") apiKey: String,
        @Body items: List<JustTcgBatchRequestItem>
    ): JustTcgResponse<List<JustTcgCard>>
}
