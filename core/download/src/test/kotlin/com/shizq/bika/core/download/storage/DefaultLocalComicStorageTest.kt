package com.shizq.bika.core.download.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okio.Buffer
import okio.Source
import okio.Timeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [DefaultLocalComicStorage] 的落盘行为回归测试。
 *
 * 这个类是下载链路上唯一接触文件系统的地方，它的三个判断直接决定数据正确性：
 * - 「这一页算不算已完成」（`length > 0`），决定续传是否跳过某页
 * - 「临时文件何时可见为最终文件」（rename），决定崩溃后磁盘是否留下半成品
 * - 「comicId 如何变成目录名」（sanitizePathSegment），决定能否越出根目录
 *
 * 需要 Robolectric 的原因有两个，都不是可以绕开的：`getExternalFilesDir` 决定
 * 存储根目录，`Uri.parse` 决定从 URL 猜扩展名。用 android.jar 桩跑会让
 * `Uri.parse` 抛 Stub 异常，而 `extractExtensionFromUrl` 里的 `runCatching`
 * 会把它静默吃掉 —— 测试照样绿，但覆盖的是错误路径。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultLocalComicStorageTest {

    private lateinit var storage: DefaultLocalComicStorage
    private lateinit var rootDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storage = DefaultLocalComicStorage(context)
        // 根目录是实现内部知识，只能从 resolveEpisodeDir 反推：<root>/<comicId>/<order>
        rootDir = storage.resolveEpisodeDir("probe", 1).parentFile!!.parentFile!!
    }

    // ── 文件名与扩展名解析 ──────────────────────────────────────────────

    @Test
    fun `页文件名补齐到三位`() {
        assertEquals("001.jpg", storage.buildPageFileName(1, "jpg"))
        assertEquals("042.jpg", storage.buildPageFileName(42, "jpg"))
        // 超过三位时不截断，否则第 1000 页会和第 100 页撞名
        assertEquals("1000.jpg", storage.buildPageFileName(1000, "jpg"))
    }

    @Test
    fun `扩展名归一化`() {
        assertEquals("jpg", storage.buildPageFileName(1, "jpeg").substringAfterLast('.'))
        assertEquals("jpg", storage.buildPageFileName(1, ".JPEG").substringAfterLast('.'))
        assertEquals("heic", storage.buildPageFileName(1, "heif").substringAfterLast('.'))
        assertEquals("webp", storage.buildPageFileName(1, "WEBP").substringAfterLast('.'))
        // 不认识的扩展名兜底成 jpg，而不是原样写进文件名
        assertEquals("jpg", storage.buildPageFileName(1, "exe").substringAfterLast('.'))
        assertEquals("jpg", storage.buildPageFileName(1, "").substringAfterLast('.'))
    }

    @Test
    fun `Content-Type 优先于 URL 扩展名`() {
        assertEquals(
            "png",
            storage.resolveImageExtension(
                url = "https://example.invalid/a/b.jpg",
                contentType = "image/png",
            ),
        )
    }

    @Test
    fun `Content-Type 带参数时仍能解析`() {
        assertEquals(
            "webp",
            storage.resolveImageExtension(url = null, contentType = "image/webp; charset=binary"),
        )
    }

    @Test
    fun `Content-Type 不可用时回退到 URL 扩展名`() {
        assertEquals(
            "png",
            storage.resolveImageExtension(
                url = "https://example.invalid/a/b.PNG?token=1",
                contentType = "application/octet-stream",
            ),
        )
    }

    @Test
    fun `两者都不可用时兜底 jpg`() {
        assertEquals("jpg", storage.resolveImageExtension(url = null, contentType = null))
        assertEquals(
            "jpg",
            storage.resolveImageExtension(
                url = "https://example.invalid/no-ext",
                contentType = null
            ),
        )
    }

    // ── 目录准备与路径安全 ──────────────────────────────────────────────

    @Test
    fun `prepareEpisodeDir 创建目录并清理残留临时文件`() {
        val dir = storage.prepareEpisodeDir("comic-a", 3)
        assertTrue(dir.isDirectory)

        val stale = File(dir, "001.jpg.download").apply { writeText("残留") }
        assertTrue(stale.exists())

        // 再次 prepare（模拟任务重启）应该清掉上一次没写完的临时文件
        storage.prepareEpisodeDir("comic-a", 3)
        assertFalse(stale.exists(), "上一次中断留下的 .download 文件必须被清理")
    }

    @Test
    fun `comicId 含路径穿越片段时不会逃出根目录`() {
        val traversal = listOf(
            "../../../etc",
            "..\\..\\windows",
            "/absolute/path",
            "a/b/c",
            "..",
            ".",
            "   ",
            "",
        )

        val rootPath = rootDir.canonicalPath
        for (comicId in traversal) {
            val dir = storage.resolveEpisodeDir(comicId, 1)
            assertTrue(
                dir.canonicalPath.startsWith(rootPath + File.separator),
                "comicId=<$comicId> 解析出的目录逃出了根目录: ${dir.canonicalPath}",
            )
        }
    }

    @Test
    fun `空白 comicId 退化为占位目录而不是根目录本身`() {
        val dir = storage.resolveEpisodeDir("   ", 1)
        // 目录名不能为空，否则 episode 目录会直接挂在 root 下、与其它漫画混在一起
        assertEquals("_", dir.parentFile!!.name)
    }

    // ── 原子写入 ────────────────────────────────────────────────────────

    @Test
    fun `writePageAtomically 写入内容并清理临时文件`() {
        val dir = storage.prepareEpisodeDir("comic-b", 1)
        val target = File(dir, "001.jpg")

        storage.writePageAtomically(target, sourceOf("hello"))

        assertEquals("hello", target.readText())
        assertFalse(File(dir, "001.jpg.download").exists(), "成功后不应残留临时文件")
    }

    @Test
    fun `writePageAtomically 覆盖已存在的目标文件`() {
        val dir = storage.prepareEpisodeDir("comic-c", 1)
        val target = File(dir, "001.jpg").apply { writeText("旧内容") }

        storage.writePageAtomically(target, sourceOf("新内容"))

        assertEquals("新内容", target.readText())
    }

    @Test
    fun `写入中途失败时不产生最终文件且清理临时文件`() {
        val dir = storage.prepareEpisodeDir("comic-d", 1)
        val target = File(dir, "001.jpg")

        assertFailsWith<IOException> {
            storage.writePageAtomically(target, FailingSource(bytesBeforeFailure = 3))
        }

        assertFalse(target.exists(), "半成品不能出现在最终文件名上 —— 否则续传会认为该页已完成")
        assertFalse(File(dir, "001.jpg.download").exists(), "失败后必须清理临时文件")
    }

    @Test
    fun `写入中途失败不会破坏已存在的旧文件`() {
        val dir = storage.prepareEpisodeDir("comic-e", 1)
        val target = File(dir, "001.jpg").apply { writeText("完好的旧页") }

        assertFailsWith<IOException> {
            storage.writePageAtomically(target, FailingSource(bytesBeforeFailure = 2))
        }

        assertTrue(target.exists(), "重下失败不应把已有的好页删掉")
        assertEquals("完好的旧页", target.readText())
    }

    @Test
    fun `空响应体被拒绝`() {
        val dir = storage.prepareEpisodeDir("comic-f", 1)
        val target = File(dir, "001.jpg")

        assertFailsWith<IOException> {
            storage.writePageAtomically(target, sourceOf(""))
        }
        assertFalse(target.exists())
    }

    @Test
    fun `写入新格式时清掉同页的其它格式`() {
        val dir = storage.prepareEpisodeDir("comic-g", 1)
        val oldVariant = File(dir, "001.png").apply { writeText("旧 png") }

        storage.writePageAtomically(File(dir, "001.jpg"), sourceOf("新 jpg"))

        assertFalse(
            oldVariant.exists(),
            "同页不应同时留下多种格式，否则 listPageFiles 会读到歧义文件"
        )
        assertEquals("新 jpg", File(dir, "001.jpg").readText())
    }

    // ── 已完成页码扫描 ──────────────────────────────────────────────────

    @Test
    fun `findExistingPageNumbers 忽略临时文件与非图片文件`() {
        val dir = storage.prepareEpisodeDir("comic-h", 1)
        File(dir, "001.jpg").writeText("a")
        File(dir, "002.jpg.download").writeText("未完成")
        File(dir, "003.txt").writeText("不是图片")
        File(dir, "notanumber.jpg").writeText("页码解析不出来")

        assertEquals(setOf(1), storage.findExistingPageNumbers(dir))
    }

    @Test
    fun `零字节文件不算已完成并被清理`() {
        val dir = storage.prepareEpisodeDir("comic-i", 1)
        File(dir, "001.jpg").writeText("ok")
        val broken = File(dir, "002.jpg").apply { createNewFile() }

        assertEquals(
            setOf(1),
            storage.findExistingPageNumbers(dir),
            "零字节文件必须排除，否则续传会永久跳过这一页",
        )
        assertFalse(broken.exists(), "顺手清掉损坏文件，让下次续传能重下")
    }

    @Test
    fun `同页多格式时只保留一个`() {
        val dir = storage.prepareEpisodeDir("comic-j", 1)
        File(dir, "001.jpg").writeText("jpg")
        File(dir, "001.png").writeText("png")
        File(dir, "001.webp").writeText("webp")

        assertEquals(setOf(1), storage.findExistingPageNumbers(dir))

        val remaining = dir.listFiles()!!.filter { it.isFile }
        assertEquals(
            1,
            remaining.size,
            "同页应收敛到单个文件，实际剩余: ${remaining.map { it.name }}"
        )
    }

    @Test
    fun `findExistingPageFile 找不到时返回 null`() {
        val dir = storage.prepareEpisodeDir("comic-k", 1)
        assertNull(storage.findExistingPageFile(dir, 1))
    }

    @Test
    fun `findExistingPageFile 不把临时文件当成已完成`() {
        val dir = storage.prepareEpisodeDir("comic-l", 1)
        File(dir, "001.jpg.download").writeText("未完成")

        assertNull(storage.findExistingPageFile(dir, 1))
    }

    @Test
    fun `目录不存在时扫描返回空而不抛异常`() {
        val missing = storage.resolveEpisodeDir("never-created", 99)
        assertEquals(emptySet(), storage.findExistingPageNumbers(missing))
        assertEquals(emptyList(), storage.listPageFiles(missing))
        assertNull(storage.findExistingPageFile(missing, 1))
    }

    // ── 页序 ────────────────────────────────────────────────────────────

    @Test
    fun `listPageFiles 按页码数字排序而非字典序`() {
        val dir = storage.prepareEpisodeDir("comic-m", 1)
        listOf("010.jpg", "002.jpg", "001.jpg", "100.jpg", "021.jpg").forEach {
            File(dir, it).writeText("x")
        }

        assertEquals(
            listOf("001.jpg", "002.jpg", "010.jpg", "021.jpg", "100.jpg"),
            storage.listPageFiles(dir).map { it.name },
        )
    }

    @Test
    fun `listPageFiles 排除零字节与临时文件`() {
        val dir = storage.prepareEpisodeDir("comic-n", 1)
        File(dir, "001.jpg").writeText("x")
        File(dir, "002.jpg").createNewFile()
        File(dir, "003.jpg.download").writeText("x")

        assertEquals(listOf("001.jpg"), storage.listPageFiles(dir).map { it.name })
    }

    // ── 删除与清理 ──────────────────────────────────────────────────────

    @Test
    fun `deleteEpisodeDir 删除整章并回收空的漫画目录`() {
        val dir = storage.prepareEpisodeDir("comic-o", 1)
        File(dir, "001.jpg").writeText("x")
        val comicDir = dir.parentFile!!

        assertTrue(storage.deleteEpisodeDir(dir))

        assertFalse(dir.exists())
        assertFalse(comicDir.exists(), "漫画目录空了应一并回收")
        assertTrue(rootDir.exists(), "根目录必须保留")
    }

    @Test
    fun `deleteEpisodeDir 保留仍有其它章节的漫画目录`() {
        val ep1 = storage.prepareEpisodeDir("comic-p", 1)
        val ep2 = storage.prepareEpisodeDir("comic-p", 2)
        File(ep2, "001.jpg").writeText("x")

        assertTrue(storage.deleteEpisodeDir(ep1))

        assertFalse(ep1.exists())
        assertTrue(ep2.exists(), "同一漫画的其它章节不能被连带删除")
    }

    @Test
    fun `删除不存在的目录视为成功`() {
        assertTrue(storage.deleteEpisodeDir(storage.resolveEpisodeDir("ghost", 1)))
    }

    @Test
    fun `deleteComicDir 删除整本`() {
        storage.prepareEpisodeDir("comic-q", 1).also { File(it, "001.jpg").writeText("x") }
        storage.prepareEpisodeDir("comic-q", 2).also { File(it, "001.jpg").writeText("x") }

        assertTrue(storage.deleteComicDir("comic-q"))
        assertFalse(storage.resolveEpisodeDir("comic-q", 1).parentFile!!.exists())
    }

    @Test
    fun `listDownloadedComics 统计目录占用`() {
        val dir = storage.prepareEpisodeDir("comic-r", 1)
        File(dir, "001.jpg").writeText("1234567890") // 10 字节

        val usage = storage.listDownloadedComics().single { it.comicId == "comic-r" }
        assertEquals(10L, usage.sizeInBytes)
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun sourceOf(content: String): Source =
        Buffer().writeUtf8(content)

    /** 先吐出若干字节再抛 IOException，模拟连接中途断开。 */
    private class FailingSource(private val bytesBeforeFailure: Int) : Source {
        private var emitted = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (emitted >= bytesBeforeFailure) throw IOException("连接被对端重置")
            val n = minOf(byteCount, (bytesBeforeFailure - emitted).toLong())
            sink.writeUtf8("x".repeat(n.toInt()))
            emitted += n.toInt()
            return n
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }
}
