package com.mrhayami.vaultio.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
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
    val name: String,
    val printing: String,
    val condition: String,
    val prices: JustTcgPrice?
)

@JsonClass(generateAdapter = true)
data class JustTcgCard(
    val id: String,
    val name: String,
    val tcgplayerId: String?,
    val variants: List<JustTcgVariant> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JustTcgResponse<T>(
    val data: T
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
        @Query("q") query: String
    ): JustTcgResponse<List<JustTcgCard>>
}
