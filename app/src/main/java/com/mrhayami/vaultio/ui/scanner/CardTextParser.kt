package com.mrhayami.vaultio.ui.scanner

import com.mrhayami.vaultio.data.VintageSets
import java.text.Normalizer

enum class CardEra {
    MODERN,
    VINTAGE
}

data class ParsedFrame(
    val number: String?,
    val total: String?,
    val name: String?,
    val era: CardEra
)

/** Zone fractions for a line, so parsing can be tested without Android rects. */
data class OcrLine(
    val text: String,
    val topFraction: Float?,
    val centerYFraction: Float?
)

/**
 * Turns one OCR frame into a collector number, set total, name, and card era.
 * Modern rules stay the existing priority (number/total, then set prefix, then a
 * standalone number). Vintage rules accept only number/total.
 */
object CardTextParser {
    const val NAME_ZONE_MAX_FRACTION = 0.30f
    const val NUMBER_ZONE_MIN_FRACTION = 0.70f

    private val numberTotalRegex = Regex("""([A-Z0-9]{1,5})\s*/\s*([A-Z0-9]{1,5})""")
    private val setNumberRegex = Regex("""([A-Z]{1,4})\s*(\d{1,4})""")
    private val standaloneNumberRegex = Regex("""(\d{3,5})""")
    private val yearRegex = Regex("""(?:19|20)\d{2}""")
    private val trailingHpRegex = Regex("""\s+\d+\s*HP$""")

    private val digitConfusables = setOf('O', 'I', 'L', 'S', 'B', 'G', 'D', 'Z', 'Q')
    private val nameSkipFragments = listOf("EVOLVES", "PUT ", "POKEMON", "STAGE")

    private val noiseWords = setOf(
        "HP", "STAGE", "BASIC", "LEVEL", "WEAKNESS", "RESISTANCE", "RETREAT",
        "POKEMON", "DRAGON", "PULSE", "SPIRAL", "BURST", "RAPID", "STRIKE",
        "DISCARD", "ENERGY", "DAMAGE", "RULE", "ABILITY", "TRAINER", "ITEM",
        "SUPPORTER", "STADIUM", "ATTACK", "VRULE", "KNOCKED", "VMAX", "VSTAR",
        "EX", "GX", "BREAK", "MEGA", "TAG TEAM", "PRISM", "RADIANT", "TERA"
    )

    fun parse(
        lines: List<DetectedLine>,
        eraOverride: ScannerEraOverride = ScannerEraOverride.AUTO
    ): ParsedFrame = parseLines(lines.map { it.toOcrLine() }, eraOverride)

    fun parseLines(
        lines: List<OcrLine>,
        eraOverride: ScannerEraOverride = ScannerEraOverride.AUTO
    ): ParsedFrame {
        val name = extractName(lines)
        val bottomRaw = lines
            .filter { (it.centerYFraction ?: 0f) > NUMBER_ZONE_MIN_FRACTION }
            .joinToString(" ") { it.text }

        val modernNumber = readNumber(bottomRaw)
        val era = when (eraOverride) {
            ScannerEraOverride.VINTAGE -> CardEra.VINTAGE
            ScannerEraOverride.MODERN -> CardEra.MODERN
            ScannerEraOverride.AUTO -> detectEra(bottomRaw, modernNumber.total)
        }
        val number = if (era == CardEra.VINTAGE) {
            readVintageNumber(bottomRaw)
        } else {
            modernNumber
        }
        return ParsedFrame(
            number = number.number,
            total = number.total,
            name = name,
            era = era
        )
    }

    /**
     * Forms to try against Room. Vintage TCGDex ids are unpadded ("4") while a
     * modern read is often zero-padded ("025").
     */
    fun localIdLookupKeys(localId: String): List<String> {
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

    private data class NumberRead(val number: String?, val total: String?)

    private fun readVintageNumber(bottomRaw: String): NumberRead {
        val normalized = normalizeOcrTextForNumbers(stripCopyrightAndYears(bottomRaw))
        val matches = numberTotalRegex.findAll(normalized).map { match ->
            val number = match.groupValues[1]
            val total = match.groupValues[2]
            Triple(number, total, repairVintageTotal(total))
        }.filter { it.first.all { digit -> digit.isDigit() } }.toList()

        val known = matches.filter { it.third != null }
        val pool = if (known.isNotEmpty()) known else matches
        val chosen = pool.maxWithOrNull(compareBy({ it.first.length }, { pool.indexOf(it) }))
            ?: return NumberRead(null, null)
        val number = chosen.first.takeUnless { noiseWords.contains(it) }
        return NumberRead(number, chosen.third ?: chosen.second)
    }

    /**
     * ML Kit often drops or repeats the leading 1 on totals like 102, 64, and 130
     * ("35/02", "12/164", "47/1102"). Snap back only when one printed vintage total fits.
     */
    private fun repairVintageTotal(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val asInt = digits.toIntOrNull()
        if (asInt != null && VintageSets.isVintagePrintedTotal(asInt)) {
            return asInt.toString()
        }
        val candidates = VintageSets.printedTotals().filter { total ->
            val printed = total.toString()
            digits.endsWith(printed) || digits.startsWith(printed) ||
                printed.endsWith(digits) || printed.startsWith(digits)
        }
        return candidates.singleOrNull()?.toString()
    }

    private fun readNumber(bottomRaw: String): NumberRead {
        val normalized = normalizeOcrTextForNumbers(bottomRaw)

        val totalMatch = numberTotalRegex.find(normalized)
        val read = if (totalMatch != null) {
            NumberRead(totalMatch.groupValues[1], totalMatch.groupValues[2])
        } else {
            val setMatch = setNumberRegex.find(normalized)
            if (setMatch != null) {
                NumberRead("${setMatch.groupValues[1]}${setMatch.groupValues[2]}", null)
            } else {
                val standalone = standaloneNumberRegex.find(normalized)?.groupValues?.get(1)
                val year = standalone?.toIntOrNull()
                if (standalone != null && (year == null || year !in 1995..2035)) {
                    NumberRead(standalone, null)
                } else {
                    NumberRead(null, null)
                }
            }
        }
        val number = read.number?.takeUnless { noiseWords.contains(it.uppercase()) }
        return read.copy(number = number)
    }

    private fun detectEra(bottomRaw: String, total: String?): CardEra {
        val folded = foldAccents(bottomRaw)
        if (Regex("""\bWIZARDS\b""").containsMatchIn(folded)) return CardEra.VINTAGE
        val years = yearRegex.findAll(folded).map { it.value.toInt() }.toList()
        if (years.isNotEmpty() && years.max() <= 2003) return CardEra.VINTAGE
        val totalInt = total?.toIntOrNull()
        if (totalInt != null && VintageSets.isVintagePrintedTotal(totalInt)) return CardEra.VINTAGE
        return CardEra.MODERN
    }

    private fun extractName(lines: List<OcrLine>): String? {
        val candidates = lines
            .filter { (it.topFraction ?: Float.MAX_VALUE) < NAME_ZONE_MAX_FRACTION }
            .sortedBy { it.topFraction ?: Float.MAX_VALUE }
        for (line in candidates) {
            if (isEvolutionOrStageLine(line.text)) continue
            val cleaned = cleanCardName(line.text)
            if (cleaned.length >= 3) return cleaned
        }
        return null
    }

    private fun isEvolutionOrStageLine(text: String): Boolean {
        val folded = foldAccents(text)
        return nameSkipFragments.any { folded.contains(it) }
    }

    private fun stripCopyrightAndYears(text: String): String {
        val withoutMark = text.replace(Regex("""©|\(C\)""", RegexOption.IGNORE_CASE), " ")
        return withoutMark.split(Regex("\\s+"))
            .filter { token ->
                if (token.contains('/')) return@filter true
                val year = token.filter { it.isDigit() }.toIntOrNull()
                year == null || year !in 1995..2035
            }
            .joinToString(" ")
    }

    /**
     * Digit-confusion fixes apply only to a slash-separated piece that is entirely
     * digits or confusable letters. Set prefixes such as TG and SWSH are left intact.
     */
    internal fun normalizeOcrTextForNumbers(text: String): String {
        return text.split(Regex("\\s+")).joinToString(" ") { token ->
            token.uppercase().split("/").joinToString("/") { part ->
                if (part.isNotEmpty() && part.all { it.isDigit() || it in digitConfusables }) {
                    part
                        .replace("O", "0")
                        .replace("I", "1")
                        .replace("L", "1")
                        .replace("S", "5")
                        .replace("B", "8")
                        .replace("G", "6")
                        .replace("D", "0")
                        .replace("Z", "2")
                        .replace("Q", "9")
                } else {
                    part
                }
            }
        }
    }

    internal fun cleanCardName(text: String): String {
        val upper = foldAccents(text)
        if (noiseWords.contains(upper.trim())) return ""
        val prefixes = listOf("BASIC", "STAGE 1", "STAGE 2", "LEVEL")
        var cleaned = upper
        prefixes.forEach { cleaned = cleaned.replace(it, "") }
        val result = cleaned.replace(Regex("[^A-Z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()
        val withoutHp = result.replace(trailingHpRegex, "").trim()
        if (withoutHp.isEmpty() || withoutHp.all { it.isDigit() } || noiseWords.contains(withoutHp)) {
            return ""
        }
        return withoutHp
    }

    private fun foldAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{M}+"), "").uppercase()
    }
}

fun DetectedLine.toOcrLine(): OcrLine {
    val box = boundingBox
    return OcrLine(
        text = text,
        topFraction = box?.let { it.top.toFloat() / imageHeight },
        centerYFraction = box?.let { it.centerY().toFloat() / imageHeight }
    )
}
