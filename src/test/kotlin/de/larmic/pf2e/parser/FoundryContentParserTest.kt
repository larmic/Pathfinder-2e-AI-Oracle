package de.larmic.pf2e.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FoundryContentParserTest {

    private val parser = FoundryContentParser()

    @Nested
    inner class CleanUuidTags {

        @Test
        fun `converts UUID with label to label text`() {
            val input = "@UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2}"
            val result = parser.cleanUuidTags(input)
            assertThat(result).isEqualTo("Clumsy 2")
        }

        @Test
        fun `converts UUID without label to extracted name`() {
            val input = "@UUID[Compendium.pf2e.actionspf2e.Item.Refocus]"
            val result = parser.cleanUuidTags(input)
            assertThat(result).isEqualTo("Refocus")
        }

        @Test
        fun `handles multiple UUIDs in text`() {
            val input = "You become @UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2} and @UUID[Compendium.pf2e.conditionitems.Item.Drained]{Drained 2}"
            val result = parser.cleanUuidTags(input)
            assertThat(result).isEqualTo("You become Clumsy 2 and Drained 2")
        }

        @Test
        fun `handles CamelCase names in UUID path`() {
            val input = "@UUID[Compendium.pf2e.actionspf2e.Item.CastSpell]"
            val result = parser.cleanUuidTags(input)
            assertThat(result).isEqualTo("Cast Spell")
        }

        @Test
        fun `handles mixed UUID types`() {
            val input = "See @UUID[Compendium.pf2e.actionspf2e.Item.Refocus] or @UUID[Compendium.pf2e.feats-srd.Item.QiSpells]{Qi Spells}"
            val result = parser.cleanUuidTags(input)
            assertThat(result).isEqualTo("See Refocus or Qi Spells")
        }
    }

    @Nested
    inner class CleanCheckTags {

        @Test
        fun `converts check with DC`() {
            val input = "@Check[fortitude|dc:22]"
            val result = parser.cleanCheckTags(input)
            assertThat(result).isEqualTo("Fortitude DC 22")
        }

        @Test
        fun `converts basic check`() {
            val input = "@Check[fortitude|dc:22|basic]"
            val result = parser.cleanCheckTags(input)
            assertThat(result).isEqualTo("basic Fortitude DC 22")
        }

        @Test
        fun `converts check without DC`() {
            val input = "@Check[perception]"
            val result = parser.cleanCheckTags(input)
            assertThat(result).isEqualTo("Perception")
        }

        @Test
        fun `handles multiple checks in text`() {
            val input = "Make a @Check[fortitude|dc:20] or @Check[reflex|dc:18]"
            val result = parser.cleanCheckTags(input)
            assertThat(result).isEqualTo("Make a Fortitude DC 20 or Reflex DC 18")
        }
    }

    @Nested
    inner class CleanDamageTags {

        @Test
        fun `converts simple damage`() {
            val input = "@Damage[4d6[healing]]"
            val result = parser.cleanDamageTags(input)
            assertThat(result).isEqualTo("4d6 healing damage")
        }

        @Test
        fun `converts damage with modifier`() {
            val input = "@Damage[(2d6+4)[fire]]"
            val result = parser.cleanDamageTags(input)
            assertThat(result).isEqualTo("2d6+4 fire damage")
        }

        @Test
        fun `handles multiple damage tags`() {
            val input = "Deal @Damage[2d6[fire]] plus @Damage[1d6[poison]]"
            val result = parser.cleanDamageTags(input)
            assertThat(result).isEqualTo("Deal 2d6 fire damage plus 1d6 poison damage")
        }
    }

    @Nested
    inner class CleanTemplateTags {

        @Test
        fun `converts cone template`() {
            val input = "@Template[cone|distance:30]"
            val result = parser.cleanTemplateTags(input)
            assertThat(result).isEqualTo("30-foot cone")
        }

        @Test
        fun `converts emanation template`() {
            val input = "@Template[emanation|distance:10]"
            val result = parser.cleanTemplateTags(input)
            assertThat(result).isEqualTo("10-foot emanation")
        }

        @Test
        fun `converts burst template`() {
            val input = "@Template[burst|distance:20]"
            val result = parser.cleanTemplateTags(input)
            assertThat(result).isEqualTo("20-foot burst")
        }
    }

    @Nested
    inner class CleanLocalizeTags {

        @Test
        fun `converts localize tag to last segment`() {
            val input = "@Localize[PF2E.NPC.Abilities.Glossary.Tremorsense]"
            val result = parser.cleanLocalizeTags(input)
            assertThat(result).isEqualTo("Tremorsense")
        }

        @Test
        fun `handles CamelCase in localize`() {
            val input = "@Localize[PF2E.NPC.Abilities.Glossary.AttackOfOpportunity]"
            val result = parser.cleanLocalizeTags(input)
            assertThat(result).isEqualTo("Attack Of Opportunity")
        }

        @Test
        fun `handles negative healing localize`() {
            val input = "@Localize[PF2E.NPC.Abilities.Glossary.NegativeHealing]"
            val result = parser.cleanLocalizeTags(input)
            assertThat(result).isEqualTo("Negative Healing")
        }
    }

    @Nested
    inner class CleanEmbedTags {

        @Test
        fun `removes embed tags`() {
            val input = "@Embed[Compendium.pf2e.actionspf2e.Item.x73LKNcaRwurr0fR inline]"
            val result = parser.cleanEmbedTags(input)
            assertThat(result).isEmpty()
        }

        @Test
        fun `removes embed tags from text`() {
            val input = "See also: @Embed[Compendium.pf2e.actionspf2e.Item.x73LKNcaRwurr0fR inline] for more info"
            val result = parser.cleanEmbedTags(input)
            assertThat(result).isEqualTo("See also:  for more info")
        }
    }

    @Nested
    inner class CleanDiceRolls {

        @Test
        fun `converts simple dice roll`() {
            val input = "[[/r 1d20]]"
            val result = parser.cleanDiceRolls(input)
            assertThat(result).isEqualTo("1d20")
        }

        @Test
        fun `converts dice roll with modifier`() {
            val input = "[[/r 2d6+4]]"
            val result = parser.cleanDiceRolls(input)
            assertThat(result).isEqualTo("2d6+4")
        }

        @Test
        fun `handles dice roll in text`() {
            val input = "Roll [[/r 1d20]] to determine the outcome"
            val result = parser.cleanDiceRolls(input)
            assertThat(result).isEqualTo("Roll 1d20 to determine the outcome")
        }
    }

    @Nested
    inner class CleanHtmlTags {

        @Test
        fun `converts paragraph tags to newlines`() {
            val input = "<p>First paragraph</p><p>Second paragraph</p>"
            val result = parser.cleanHtmlTags(input)
            assertThat(result).isEqualTo("\nFirst paragraph\n\nSecond paragraph\n")
        }

        @Test
        fun `removes strong tags`() {
            val input = "<strong>Bold text</strong>"
            val result = parser.cleanHtmlTags(input)
            assertThat(result).isEqualTo("Bold text")
        }

        @Test
        fun `converts hr to separator`() {
            val input = "Before<hr />After"
            val result = parser.cleanHtmlTags(input)
            assertThat(result).isEqualTo("Before\n---\nAfter")
        }

        @Test
        fun `handles self-closing hr`() {
            val input = "Before<hr/>After"
            val result = parser.cleanHtmlTags(input)
            assertThat(result).isEqualTo("Before\n---\nAfter")
        }

        @Test
        fun `converts list items`() {
            val input = "<ul><li>Item 1</li><li>Item 2</li></ul>"
            val result = parser.cleanHtmlTags(input)
            assertThat(result).contains("- Item 1")
            assertThat(result).contains("- Item 2")
        }
    }

    @Nested
    inner class NormalizeWhitespace {

        @Test
        fun `reduces multiple newlines`() {
            val input = "Line 1\n\n\n\nLine 2"
            val result = parser.normalizeWhitespace(input)
            assertThat(result).isEqualTo("Line 1\n\nLine 2")
        }

        @Test
        fun `reduces multiple spaces`() {
            val input = "Word1    Word2"
            val result = parser.normalizeWhitespace(input)
            assertThat(result).isEqualTo("Word1 Word2")
        }

        @Test
        fun `trims leading and trailing whitespace`() {
            val input = "  Content  "
            val result = parser.normalizeWhitespace(input)
            assertThat(result).isEqualTo("Content")
        }
    }

    @Nested
    inner class CleanContent {

        @Test
        fun `cleans complex content with multiple tag types`() {
            val input = """
                <p>You become @UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2}.</p>
                <p>Make a @Check[fortitude|dc:22] save.</p>
                <p>Deal @Damage[4d6[fire]] in a @Template[cone|distance:30].</p>
            """.trimIndent()

            val result = parser.cleanContent(input)

            assertThat(result).contains("You become Clumsy 2.")
            assertThat(result).contains("Make a Fortitude DC 22 save.")
            assertThat(result).contains("Deal 4d6 fire damage in a 30-foot cone.")
            assertThat(result).doesNotContain("@UUID")
            assertThat(result).doesNotContain("@Check")
            assertThat(result).doesNotContain("@Damage")
            assertThat(result).doesNotContain("@Template")
            assertThat(result).doesNotContain("<p>")
        }

        @Test
        fun `handles real spell description`() {
            val input = """<p>If Pharasma has decided that the target's time has come or the target's soul is trapped or doesn't wish to return, this ritual automatically fails.</p>
<p><strong>Critical Success</strong> You reincarnate the target into a new adult body.</p>
<p><strong>Success</strong> As critical success, except the new body has 1 HP and no spells prepared. The soul takes some time to adjust to their new body, leaving them @UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2}, @UUID[Compendium.pf2e.conditionitems.Item.Drained]{Drained 2}, and @UUID[Compendium.pf2e.conditionitems.Item.Enfeebled]{Enfeebled 2} for 1 week.</p>"""

            val result = parser.cleanContent(input)

            assertThat(result).contains("Clumsy 2")
            assertThat(result).contains("Drained 2")
            assertThat(result).contains("Enfeebled 2")
            assertThat(result).doesNotContain("@UUID")
            assertThat(result).doesNotContain("<p>")
            assertThat(result).doesNotContain("<strong>")
        }

        @Test
        fun `returns empty string for empty input`() {
            val result = parser.cleanContent("")
            assertThat(result).isEmpty()
        }

        @Test
        fun `returns unchanged text without foundry tags`() {
            val input = "This is plain text without any special tags."
            val result = parser.cleanContent(input)
            assertThat(result).isEqualTo(input)
        }
    }
}
