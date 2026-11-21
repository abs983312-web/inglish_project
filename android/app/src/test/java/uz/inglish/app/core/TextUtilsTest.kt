package uz.inglish.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilsTest {

    @Test
    fun countWords_withEmptyString_returnsZero() {
        assertEquals(0, countWords(""))
    }

    @Test
    fun countWords_withWhitespaceOnly_returnsZero() {
        assertEquals(0, countWords("   "))
        assertEquals(0, countWords("\n\t  \r\n"))
    }

    @Test
    fun countWords_withSingleWord_returnsOne() {
        assertEquals(1, countWords("Hello"))
        assertEquals(1, countWords("  IELTS  "))
    }

    @Test
    fun countWords_withMultipleWords_countsCorrectly() {
        val text = "The quick brown fox jumps over the lazy dog"
        assertEquals(9, countWords(text))
    }

    @Test
    fun countWords_withIrregularSpacingAndNewlines_countsCorrectly() {
        val text = "Task  2    essay\n\nIntroduction paragraph.\n\nBody 1\thas\tpoints."
        // "Task", "2", "essay", "Introduction", "paragraph.", "Body", "1", "has", "points." = 9 words
        assertEquals(9, countWords(text))
    }

    @Test
    fun countWords_withIeltsEssaySample_reachesExpectedCount() {
        // Sample IELTS Academic Task 2 essay segment
        val essay = """
            Some people believe that university education should be free for everyone, 
            while others argue that students should pay for their higher education. 
            In my opinion, although providing free tertiary education imposes a considerable 
            financial burden on governments, it leads to significant long-term societal and economic benefits.
        """.trimIndent()

        val expectedWords = 46
        assertEquals(expectedWords, countWords(essay))
    }

    @Test
    fun countWords_withHyphenatedAndPunctuatedWords_countsTokens() {
        val text = "state-of-the-art AI-powered language-learning platform!"
        // "state-of-the-art", "AI-powered", "language-learning", "platform!" = 4 words
        assertEquals(4, countWords(text))
    }
}
