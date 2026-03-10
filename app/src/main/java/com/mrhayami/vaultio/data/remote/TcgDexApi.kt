package com.mrhayami.vaultio.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class CardCount(
    val total: Int,
    val official: Int
)

@JsonClass(generateAdapter = true)
data class TcgDexSet(
    val id: String,
    val name: String,
    val logo: String?,
    val symbol: String?,
    val cardCount: CardCount,
    val series: String?,
    val releaseDate: String?
)

@JsonClass(generateAdapter = true)
data class TcgDexCard(
    val id: String,
    val localId: String,
    val name: String,
    val image: String?,
    val rarity: String?,
    val category: String?,
    val dexId: List<Int>? = null,
    val variants: TcgDexVariants? = null,
    val tcgplayer: TcgDexMarket? = null,
    val cardmarket: TcgDexMarket? = null
)

@JsonClass(generateAdapter = true)
data class TcgDexVariants(
    val firstEdition: Boolean = false,
    val holo: Boolean = false,
    val normal: Boolean = true,
    val reverse: Boolean = false,
    val wPromo: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TcgDexMarket(
    val url: String?,
    val updatedAt: String?,
    val prices: TcgDexPrices?
)

@JsonClass(generateAdapter = true)
data class TcgDexPrices(
    val low: Double?,
    val average: Double?,
    val high: Double?,
    val market: Double?,
    val reverseHoloLow: Double?,
    val reverseHoloAvg: Double?,
    val reverseHoloHigh: Double?,
    val reverseHoloMarket: Double?,
    val holoLow: Double?,
    val holoAvg: Double?,
    val holoHigh: Double?,
    val holoMarket: Double?,
    val firstEditionLow: Double?,
    val firstEditionAvg: Double?,
    val firstEditionHigh: Double?,
    val firstEditionMarket: Double?
)

@JsonClass(generateAdapter = true)
data class TcgDexSetDetail(
    val id: String,
    val name: String,
    val cards: List<TcgDexCard>
)

interface TcgDexApi {
    @GET("sets")
    suspend fun getSets(): List<TcgDexSet>

    @GET("sets/{setId}")
    suspend fun getSetDetail(@Path("setId") setId: String): TcgDexSetDetail

    @GET("cards")
    suspend fun searchCards(@Query("name") name: String): List<TcgDexCard>

    @GET("cards/{cardId}")
    suspend fun getCardDetail(@Path("cardId") cardId: String): TcgDexCard
    
    @GET("cards")
    suspend fun searchCardsByLocalId(@Query("localId") localId: String): List<TcgDexCard>
}
