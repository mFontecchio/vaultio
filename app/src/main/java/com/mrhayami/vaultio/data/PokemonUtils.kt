package com.mrhayami.vaultio.data

object PokemonUtils {
    fun extractPokemonName(fullName: String): String {
        var name = fullName

        // 1. Remove common suffixes (Pokemon variants/types)
        val suffixes = listOf(
            "VMAX", "VSTAR", "GX", "EX", "ex", "BREAK", "Star", "LV.X", "Level X",
            "Prism Star", "Prime", "LEGEND", "Tag Team", "V", "V-UNION", "V-MAX",
            "Delta Species", "Holon's"
        )
        suffixes.forEach {
            name = name.replace(Regex("(?i)\\b$it\\b"), "")
        }

        // 2. Remove common prefixes (Owners/Regional Forms/States)
        val prefixes = listOf(
            "Team Rocket's", "Brock's", "Misty's", "Lt. Surge's", "Erika's", "Koga's",
            "Sabrina's", "Blaine's", "Giovanni's", "Dark", "Light", "Shining",
            "Alolan", "Galarian", "Hisuian", "Paldean", "Origin Forme", "Therian Forme",
            "Mega", "Primal"
        )
        prefixes.forEach { prefix ->
            // Use Regex.escape to handle ' and other special chars
            // For prefixes like "Team Rocket's", we don't want a trailing \b if it ends in 's
            val pattern = if (prefix.contains("'")) {
                Regex("(?i)\\b${Regex.escape(prefix)}\\s*")
            } else {
                Regex("(?i)\\b${Regex.escape(prefix)}\\b\\s*")
            }
            name = name.replace(pattern, "")
        }
        
        // 3. Handle "M " prefix for Mega Pokemon
        name = name.replace(Regex("^M\\s", RegexOption.IGNORE_CASE), "")

        // 4. Clean up punctuation and extra spaces
        name = name.split("(", "[", "{").first()
            .trim()
            .replace(Regex("\\s+"), " ")

        // Fallback to first word if normalization results in something too short or empty
        return if (name.length < 2) {
            fullName.split(" ").first()
        } else {
            name
        }
    }
}
