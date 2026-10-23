package com.mrhayami.vaultio.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mrhayami.vaultio.data.VintageSets
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Local-only check of the vintage corpus. Push it before running:
 * adb push .research/legacy-cards /sdcard/Android/data/com.mrhayami.vaultio.debug/files/legacy-cards
 *
 * Skips when that folder is absent so CI is unaffected. A modern sample downloaded
 * into the same tree (sets that are not in [VintageSets]) is checked the same way.
 */
@RunWith(AndroidJUnit4::class)
class VintageCorpusOcrTest {

    private val recognizer = when (val state = TextRecognizerProvider.getOrCreate()) {
        is TextRecognizerState.Available -> state.client
        TextRecognizerState.Unavailable -> null
    }

    @Before
    fun requireRecognizer() {
        Log.i(TAG, "recognizer available=${recognizer != null}")
        assumeTrue("ML Kit text recognizer is unavailable on this device", recognizer != null)
    }

    @After
    fun releaseRecognizer() {
        TextRecognizerProvider.close()
    }

    @Test
    fun corpusParsesCollectorNumbers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = context.getExternalFilesDir(null)?.let { File(it, "legacy-cards") }
        Log.i(TAG, "corpus root=${root?.absolutePath} exists=${root?.isDirectory}")
        assumeTrue(
            "Push .research/legacy-cards to external files before running this test",
            root != null && root.isDirectory
        )

        val camera = evaluate(root!!, targetWidth = 918)
        val degraded = evaluate(root, targetWidth = 640)
        Log.i(TAG, "918px $camera")
        Log.i(TAG, "640px $degraded")
        println("918px $camera")
        println("640px $degraded")

        assumeTrue("No card images under ${root.absolutePath}", camera.vintageChecked > 0)
        val vintageRate = camera.vintagePassed.toFloat() / camera.vintageChecked
        assertTrue(
            "Vintage number/total/era ${(vintageRate * 100).toInt()}% " +
                "(${camera.vintagePassed}/${camera.vintageChecked}). " +
                "Names pokemon ${camera.pokemonNames} energy/trainer ${camera.energyNames}. " +
                "640px vintage ${degraded.vintagePassed}/${degraded.vintageChecked}. " +
                "Failures: ${camera.failures.joinToString("; ")}",
            // Measured on the flat TCGDex renders: about 92% at 918px and about 94% at 640px.
            // The misses are mostly a dropped "1" that this pass does not guess.
            vintageRate >= 0.90f
        )

        if (camera.modernChecked > 0) {
            val modernRate = camera.modernPassed.toFloat() / camera.modernChecked
            assertTrue(
                "Modern control ${(modernRate * 100).toInt()}% " +
                    "(${camera.modernPassed}/${camera.modernChecked}). " +
                    "Failures: ${camera.modernFailures.joinToString("; ")}",
                modernRate >= 0.95f
            )
        } else {
            println("No modern control sets in the corpus; skipped that check")
        }
    }

    private fun evaluate(root: File, targetWidth: Int): CorpusStats {
        val stats = CorpusStats()
        root.listFiles { file -> file.isDirectory }?.sortedBy { it.name }?.forEach { setDir ->
            val setId = setDir.name
            val vintage = VintageSets.isVintageSet(setId)
            val printedTotal = VintageSets.printedTotalFor(setId)
            val names = readManifestNames(File(setDir, "manifest.json"))
            setDir.listFiles { file -> file.isFile && file.name.endsWith(".webp") }
                ?.sortedBy { it.name }
                ?.forEach { image ->
                    val expectedId = image.nameWithoutExtension
                        .removeSuffix("_high")
                        .substringAfter("$setId-")
                    val outcome = readCard(image, targetWidth) ?: return@forEach
                    recordName(stats, names[expectedId].orEmpty(), outcome)
                    if (vintage) {
                        stats.vintageChecked++
                        val numberOk = sameNumber(outcome.parsed.number, expectedId)
                        val totalOk = printedTotal != null && outcome.parsed.total == printedTotal.toString()
                        val eraOk = outcome.parsed.era == CardEra.VINTAGE
                        if (numberOk && totalOk && eraOk) {
                            stats.vintagePassed++
                        } else if (stats.failures.size < 25) {
                            stats.failures += "$setId-$expectedId expected $expectedId/$printedTotal " +
                                "got ${outcome.parsed.number}/${outcome.parsed.total} ${outcome.parsed.era}"
                        }
                    } else {
                        stats.modernChecked++
                        val numberOk = sameNumber(outcome.parsed.number, expectedId)
                        val eraOk = outcome.parsed.era == CardEra.MODERN ||
                            outcome.parsed.total?.toIntOrNull()?.let { VintageSets.isVintagePrintedTotal(it) } == true
                        if (numberOk && eraOk) {
                            stats.modernPassed++
                        } else if (stats.modernFailures.size < 25) {
                            stats.modernFailures += "$setId-$expectedId expected $expectedId " +
                                "got ${outcome.parsed.number}/${outcome.parsed.total} ${outcome.parsed.era}"
                        }
                    }
                }
        }
        return stats
    }

    private fun recordName(stats: CorpusStats, expectedName: String, outcome: ReadOutcome) {
        if (expectedName.isBlank()) return
        val header = outcome.lines.any { line ->
            val text = line.text.trim()
            text.equals("TRAINER", ignoreCase = true) || text.equals("ENERGY", ignoreCase = true)
        }
        val energy = expectedName.contains("energy", ignoreCase = true)
        val matched = outcome.parsed.name == null && (energy || header) ||
            namesMatch(outcome.parsed.name, expectedName)
        if (energy || header) {
            stats.energyNames = bump(stats.energyNames, matched)
        } else {
            stats.pokemonNames = bump(stats.pokemonNames, matched)
        }
    }

    private fun bump(current: String, matched: Boolean): String {
        val parts = current.split("/")
        val hits = parts[0].toInt() + if (matched) 1 else 0
        val total = parts[1].toInt() + 1
        return "$hits/$total"
    }

    private fun readCard(file: File, targetWidth: Int): ReadOutcome? {
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val scaled = scaleToWidth(decoded, targetWidth)
        if (scaled != decoded) decoded.recycle()
        return try {
            val lines = ScannerUtils.recognizeCard(scaled, recognizer!!)
            ReadOutcome(lines, CardTextParser.parse(lines))
        } catch (e: Exception) {
            Log.w(TAG, "OCR failed for ${file.name}: ${e.message}")
            null
        } finally {
            if (!scaled.isRecycled) scaled.recycle()
        }
    }

    private fun scaleToWidth(src: Bitmap, width: Int): Bitmap {
        if (src.width == width) return src
        val height = (src.height * (width.toFloat() / src.width)).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    private fun readManifestNames(manifest: File): Map<String, String> {
        if (!manifest.isFile) return emptyMap()
        val cards = JSONObject(manifest.readText()).optJSONArray("cards") ?: return emptyMap()
        val names = mutableMapOf<String, String>()
        for (index in 0 until cards.length()) {
            val card = cards.getJSONObject(index)
            names[card.optString("localId")] = card.optString("name")
        }
        return names
    }

    private fun sameNumber(actual: String?, expected: String): Boolean {
        if (actual == null) return false
        if (actual.equals(expected, ignoreCase = true)) return true
        if (actual.all { it.isDigit() } && expected.all { it.isDigit() }) {
            val left = actual.trimStart('0').ifEmpty { "0" }
            val right = expected.trimStart('0').ifEmpty { "0" }
            return left == right
        }
        return false
    }

    private fun namesMatch(actual: String?, expected: String): Boolean {
        if (actual.isNullOrBlank() || expected.isBlank()) return false
        val left = normalizeName(actual)
        val right = normalizeName(expected)
        if (left.isEmpty() || right.isEmpty()) return false
        return left == right || left.contains(right) || right.contains(left)
    }

    private fun normalizeName(value: String): String {
        return value.uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class ReadOutcome(val lines: List<DetectedLine>, val parsed: ParsedFrame)

    private data class CorpusStats(
        var vintageChecked: Int = 0,
        var vintagePassed: Int = 0,
        var modernChecked: Int = 0,
        var modernPassed: Int = 0,
        var pokemonNames: String = "0/0",
        var energyNames: String = "0/0",
        val failures: MutableList<String> = mutableListOf(),
        val modernFailures: MutableList<String> = mutableListOf()
    ) {
        override fun toString(): String {
            return "vintage $vintagePassed/$vintageChecked modern $modernPassed/$modernChecked " +
                "pokemonNames $pokemonNames energyNames $energyNames"
        }
    }

    private companion object {
        const val TAG = "VintageCorpusOcr"
    }
}
