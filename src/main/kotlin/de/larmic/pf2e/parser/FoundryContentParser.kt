package de.larmic.pf2e.parser

import org.springframework.stereotype.Service

/**
 * Stateless service that cleans Foundry VTT specific markup from text content.
 * Converts Foundry tags to human-readable format for AI processing.
 *
 * This parser handles:
 * - @UUID references (links to other compendium items)
 * - @Check tags (skill checks with DC)
 * - @Damage tags (damage rolls)
 * - @Template tags (area templates)
 * - @Localize tags (localization keys)
 * - @Embed tags (embedded content)
 * - Dice roll notation [[/r ...]]
 * - HTML tags (p, strong, hr, etc.)
 */
@Service
class FoundryContentParser {

    companion object {
        // @UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2} → display text
        // @UUID[Compendium.pf2e.actionspf2e.Item.Refocus] → extracted name
        private val UUID_WITH_LABEL = Regex("""@UUID\[[^\]]+]\{([^}]+)}""")
        private val UUID_WITHOUT_LABEL = Regex("""@UUID\[(?:[^.\]]+\.)*([^.\]]+)]""")

        // @Check[fortitude|dc:22] → Fortitude DC 22
        // @Check[fortitude|dc:22|basic] → basic Fortitude DC 22
        private val CHECK_TAG = Regex("""@Check\[([^|\]]+)(?:\|dc:(\d+))?(?:\|([^|\]]+))?(?:\|[^\]]*)?]""")

        // @Damage[4d6[healing]] → 4d6 healing damage
        // @Damage[(2d6+4)[fire]] → 2d6+4 fire damage
        private val DAMAGE_TAG = Regex("""@Damage\[(\(?[\d\w+\-*/]+\)?)\[([^\]]+)]]""")

        // @Template[cone|distance:30] → 30-foot cone
        // @Template[emanation|distance:10] → 10-foot emanation
        // @Template[burst|distance:20] → 20-foot burst
        private val TEMPLATE_TAG = Regex("""@Template\[([^|\]]+)\|distance:(\d+)]""")

        // @Localize[PF2E.NPC.Abilities.Glossary.Tremorsense] → Tremorsense
        private val LOCALIZE_TAG = Regex("""@Localize\[(?:[^.\]]+\.)*([^.\]]+)]""")

        // @Embed[Compendium.pf2e.actionspf2e.Item.x73... inline] → (remove)
        private val EMBED_TAG = Regex("""@Embed\[[^\]]+]""")

        // [[/r 1d20]] → 1d20
        // [[/r 2d6+4]] → 2d6+4
        private val DICE_ROLL = Regex("""\[\[/r\s+([^\]]+)]]""")

        // HTML tag patterns
        private val HTML_PARAGRAPH = Regex("""<p>|</p>""")
        private val HTML_STRONG = Regex("""<strong>|</strong>""")
        private val HTML_EM = Regex("""<em>|</em>""")
        private val HTML_HR = Regex("""<hr\s*/?>""")
        private val HTML_HEADING = Regex("""<h[1-6]>|</h[1-6]>""")
        private val HTML_LIST = Regex("""<ul>|</ul>|<ol>|</ol>""")
        private val HTML_LIST_ITEM = Regex("""<li>|</li>""")
        private val HTML_SPAN = Regex("""<span[^>]*>|</span>""")
        private val HTML_OTHER = Regex("""<[^>]+>""")

        // Whitespace normalization
        private val MULTIPLE_NEWLINES = Regex("""\n{3,}""")
        private val MULTIPLE_SPACES = Regex(""" {2,}""")
    }

    /**
     * Cleans Foundry-specific markup from text content.
     * Converts tags to human-readable format for AI processing.
     */
    fun cleanContent(content: String): String {
        return content
            .let { cleanUuidTags(it) }
            .let { cleanCheckTags(it) }
            .let { cleanDamageTags(it) }
            .let { cleanTemplateTags(it) }
            .let { cleanLocalizeTags(it) }
            .let { cleanEmbedTags(it) }
            .let { cleanDiceRolls(it) }
            .let { cleanHtmlTags(it) }
            .let { normalizeWhitespace(it) }
    }

    /**
     * Converts @UUID tags to readable text.
     * - With label: @UUID[...]{Label} → Label
     * - Without label: @UUID[...Item.Name] → Name (extracted from path)
     */
    internal fun cleanUuidTags(content: String): String {
        var result = UUID_WITH_LABEL.replace(content) { match ->
            match.groupValues[1]
        }
        result = UUID_WITHOUT_LABEL.replace(result) { match ->
            // Extract the last segment and convert to readable format
            match.groupValues[1]
                .replace(Regex("([a-z])([A-Z])"), "$1 $2") // CamelCase to spaces
                .replaceFirstChar { it.uppercase() }
        }
        return result
    }

    /**
     * Converts @Check tags to readable format.
     * @Check[fortitude|dc:22] → Fortitude DC 22
     * @Check[fortitude|dc:22|basic] → basic Fortitude DC 22
     */
    internal fun cleanCheckTags(content: String): String {
        return CHECK_TAG.replace(content) { match ->
            val checkType = match.groupValues[1]
                .replaceFirstChar { it.uppercase() }
                .replace("-", " ")
            val dc = match.groupValues[2]
            val modifier = match.groupValues[3]

            buildString {
                if (modifier.isNotBlank()) {
                    append(modifier)
                    append(" ")
                }
                append(checkType)
                if (dc.isNotBlank()) {
                    append(" DC ")
                    append(dc)
                }
            }
        }
    }

    /**
     * Converts @Damage tags to readable format.
     * @Damage[4d6[healing]] → 4d6 healing damage
     */
    internal fun cleanDamageTags(content: String): String {
        return DAMAGE_TAG.replace(content) { match ->
            val dice = match.groupValues[1].trim('(', ')')
            val damageType = match.groupValues[2]
            "$dice $damageType damage"
        }
    }

    /**
     * Converts @Template tags to readable format.
     * @Template[cone|distance:30] → 30-foot cone
     */
    internal fun cleanTemplateTags(content: String): String {
        return TEMPLATE_TAG.replace(content) { match ->
            val shape = match.groupValues[1]
            val distance = match.groupValues[2]
            "$distance-foot $shape"
        }
    }

    /**
     * Converts @Localize tags to readable format.
     * @Localize[PF2E.NPC.Abilities.Glossary.Tremorsense] → Tremorsense
     */
    internal fun cleanLocalizeTags(content: String): String {
        return LOCALIZE_TAG.replace(content) { match ->
            match.groupValues[1]
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        }
    }

    /**
     * Removes @Embed tags completely.
     */
    internal fun cleanEmbedTags(content: String): String {
        return EMBED_TAG.replace(content, "")
    }

    /**
     * Converts dice roll notation to plain text.
     * [[/r 1d20]] → 1d20
     */
    internal fun cleanDiceRolls(content: String): String {
        return DICE_ROLL.replace(content) { match ->
            match.groupValues[1].trim()
        }
    }

    /**
     * Converts HTML tags to plain text or simple formatting.
     */
    internal fun cleanHtmlTags(content: String): String {
        var result = content
        result = HTML_PARAGRAPH.replace(result, "\n")
        result = HTML_STRONG.replace(result, "")
        result = HTML_EM.replace(result, "")
        result = HTML_HR.replace(result, "\n---\n")
        result = HTML_HEADING.replace(result, "\n")
        result = HTML_LIST.replace(result, "\n")
        result = HTML_LIST_ITEM.replace(result) { "\n- " }
        result = HTML_SPAN.replace(result, "")
        result = HTML_OTHER.replace(result, "")
        return result
    }

    /**
     * Normalizes whitespace for cleaner output.
     */
    internal fun normalizeWhitespace(content: String): String {
        return content
            .replace(MULTIPLE_NEWLINES, "\n\n")
            .replace(MULTIPLE_SPACES, " ")
            .trim()
    }
}
