package com.mrhayami.vaultio.data

import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard

data class PrintedCardNumber(
    val localId: String,
    val total: String
) {
    val totalCount: Int? get() = total.toIntOrNull()

    fun localIdKeys(): List<String> {
        val keys = linkedSetOf<String>()
        keys.addAll(digitLookupKeys(localId))
        val prefixed = prefixRegex.matchEntire(localId) ?: return keys.toList()
        val prefix = prefixed.groupValues[1]
        val digits = prefixed.groupValues[2]
        val stripped = digits.trimStart('0').ifEmpty { "0" }
        keys += prefix + stripped
        if (stripped.length < 2) keys += prefix + stripped.padStart(2, '0')
        return keys.toList()
    }

    companion object {
        private val queryRegex = Regex("""^([A-Za-z0-9]{1,6})\s*/\s*([A-Za-z0-9]{1,6})$""")
        private val prefixRegex = Regex("""^([A-Z]+)(\d+)$""")

        fun parse(query: String): PrintedCardNumber? {
            val match = queryRegex.matchEntire(query.trim()) ?: return null
            val localId = match.groupValues[1].uppercase()
            val total = match.groupValues[2].uppercase()
            if (localId.none { it.isDigit() } || total.none { it.isDigit() }) return null
            return PrintedCardNumber(localId, total)
        }

        /**
         * Forms to try against a catalog id. Vintage ids are unpadded ("4")
         * while a modern read is often zero-padded ("025").
         */
        fun digitLookupKeys(localId: String): List<String> {
            val trimmed = localId.trim()
            if (trimmed.isEmpty()) return emptyList()
            val keys = linkedSetOf(trimmed)
            if (trimmed.all { it.isDigit() }) {
                val stripped = trimmed.trimStart('0').ifEmpty { "0" }
                keys += stripped
                keys += stripped.padStart(3, '0')
            }
            return keys.toList()
        }

        fun isIncompleteNumber(query: String): Boolean =
            query.contains('/') && parse(query) == null
    }
}

data class CollectionSearchClause(
    val sql: String,
    val args: List<Any>
)

fun collectionSearchClause(searchQuery: String): CollectionSearchClause? {
    if (searchQuery.isBlank()) return null
    val printed = PrintedCardNumber.parse(searchQuery)
    if (printed != null) {
        val keys = printed.localIdKeys()
        val placeholders = keys.joinToString { "?" }
        val sql = StringBuilder(" AND UPPER(c.localId) IN ($placeholders) ")
        val args = keys.map<String, Any> { it }.toMutableList()
        val total = printed.totalCount
        if (total != null) {
            sql.append("AND (s.officialCards = ? OR s.totalCards = ?) ")
            args += total
            args += total
        }
        return CollectionSearchClause(sql.toString(), args)
    }
    if (PrintedCardNumber.isIncompleteNumber(searchQuery)) {
        return CollectionSearchClause(" AND 0 ", emptyList())
    }
    val likeQuery = "%$searchQuery%"
    return CollectionSearchClause(
        " AND (c.name LIKE ? OR c.pokemonName LIKE ? OR s.name LIKE ?) ",
        listOf(likeQuery, likeQuery, likeQuery)
    )
}

fun filterCardsByPrintedTotal(
    cards: List<TcgDexCard>,
    total: Int,
    setsById: Map<String, SetEntity>
): List<TcgDexCard> {
    if (cards.isEmpty() || setsById.isEmpty()) return cards
    return cards.filter { card ->
        val set = setsById[card.id.substringBeforeLast('-')] ?: return@filter false
        set.officialCards == total || set.totalCards == total
    }
}
