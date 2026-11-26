package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSummary
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ComicSummaryDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `advanced search response maps likesCount to totalLikes`() {
        val raw = """
            {
                "_id": "abc123",
                "title": "兽人测试",
                "author": "作者A",
                "likesCount": 1280,
                "finished": true,
                "categories": ["短篇", "非人類"],
                "tags": ["兽人"],
                "thumb": {
                    "originalName": "cover.jpg",
                    "path": "path/cover.jpg",
                    "fileServer": "https://example.com"
                }
            }
        """.trimIndent()

        val comic = json.decodeFromString<ComicSummary>(raw)

        assertEquals("abc123", comic.id)
        assertEquals(1280, comic.totalLikes)
        assertEquals(0, comic.epsCount)
        assertEquals(0, comic.pagesCount)
    }

    @Test
    fun `standard comic list response with both totalLikes and likesCount deserializes cleanly`() {
        val raw = """
            {
                "_id": "def456",
                "title": "普通列表漫画",
                "author": "作者B",
                "totalLikes": 320,
                "likesCount": 320,
                "epsCount": 12,
                "pagesCount": 240,
                "finished": false,
                "categories": ["長篇"],
                "thumb": {
                    "originalName": "cover.jpg",
                    "path": "path/cover.jpg",
                    "fileServer": "https://example.com"
                }
            }
        """.trimIndent()

        val comic = json.decodeFromString<ComicSummary>(raw)

        assertEquals("def456", comic.id)
        assertEquals(320, comic.totalLikes)
        assertEquals(12, comic.epsCount)
        assertEquals(240, comic.pagesCount)
    }

    @Test
    fun `leaderboard response maps leaderboardCount and viewsCount`() {
        val raw = """
            {
                "_id": "lb789",
                "title": "排行榜漫画",
                "author": "作者C",
                "totalLikes": 4041,
                "totalViews": 627710,
                "viewsCount": 3029,
                "leaderboardCount": 3029,
                "epsCount": 1,
                "pagesCount": 92,
                "finished": true,
                "categories": ["短篇"],
                "thumb": {
                    "originalName": "cover.jpg",
                    "path": "path/cover.jpg",
                    "fileServer": "https://example.com"
                }
            }
        """.trimIndent()

        val comic = json.decodeFromString<ComicSummary>(raw)

        assertEquals("lb789", comic.id)
        assertEquals(4041, comic.totalLikes)
        assertEquals(627710, comic.totalViews)
        assertEquals(3029, comic.viewsCount)
        assertEquals(3029, comic.leaderboardCount)
    }
}
