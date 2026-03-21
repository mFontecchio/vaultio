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
data class TcgDexPriceItem(
    val low: Double? = null,
    val mid: Double? = null,
    val high: Double? = null,
    val market: Double? = null,
    val directLow: Double? = null,
    @param:Json(name = "lowPrice") val lowPrice: Double? = null,
    @param:Json(name = "midPrice") val midPrice: Double? = null,
    @param:Json(name = "highPrice") val highPrice: Double? = null,
    @param:Json(name = "marketPrice") val marketPrice: Double? = null
) {
    fun resolveLow() = low ?: lowPrice
    fun resolveMid() = mid ?: midPrice
    fun resolveHigh() = high ?: highPrice
    fun resolveMarket() = market ?: marketPrice
}

@JsonClass(generateAdapter = true)
data class TcgDexTcgPlayerPricing(
    val url: String? = null,
    val updated: String? = null,
    val normal: TcgDexPriceItem? = null,
    val holofoil: TcgDexPriceItem? = null,
    val reverse: TcgDexPriceItem? = null,
    @param:Json(name = "firstEdition") val firstEdition: TcgDexPriceItem? = null
)

@JsonClass(generateAdapter = true)
data class TcgDexPricing(
    val tcgplayer: TcgDexTcgPlayerPricing? = null
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
    val hp: Int? = null,
    val types: List<String>? = null,
    val stage: String? = null,
    val variants: TcgDexVariants? = null,
    val pricing: TcgDexPricing? = null
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
