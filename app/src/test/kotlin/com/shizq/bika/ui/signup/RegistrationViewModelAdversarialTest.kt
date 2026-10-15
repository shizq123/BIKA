package com.shizq.bika.ui.signup

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.CredentialsCipher
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.model.UserCredentials
import com.shizq.bika.core.network.BikaDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelAdversarialTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class InMemoryDataStore(
        initial: UserCredentials = UserCredentials()
    ) : DataStore<UserCredentials> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UserCredentials> = state
        override suspend fun updateData(transform: suspend (UserCredentials) -> UserCredentials): UserCredentials {
            return transform(state.value).also { state.value = it }
        }
    }

    private class FakeCredentialsCipher : CredentialsCipher() {
        override fun encrypt(plain: String): String = plain
        override fun decrypt(value: String): String = value
        override fun isEncrypted(value: String): Boolean = false
    }

    private class FakeUserCredentialsDataSource(
        dataStore: DataStore<UserCredentials> = InMemoryDataStore(),
        cipher: CredentialsCipher = FakeCredentialsCipher(),
    ) : UserCredentialsDataSource(dataStore, cipher)

    private fun createTestClient(handler: MockRequestHandler): HttpClient {
        val config = MockEngineConfig().apply {
            dispatcher = testDispatcher
            requestHandlers.add(handler)
        }
        val engine = MockEngine(config)
        return HttpClient(engine) {
            defaultRequest {
                url("https://picaapi.picacomic.com/")
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setQuestionsReflectively(viewModel: RegistrationViewModel, questions: List<SecurityQuestion>) {
        val questionsField = viewModel.securityQuestionsState.javaClass.getDeclaredField("_questions")
        questionsField.isAccessible = true
        val list = questionsField.get(viewModel.securityQuestionsState) as MutableList<SecurityQuestion>
        list.clear()
        list.addAll(questions)
    }

    @Test
    fun testMalformedQuestions_exactlyZeroQuestions_gracefulRejection() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        setQuestionsReflectively(viewModel, emptyList())

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)
    }

    @Test
    fun testMalformedQuestions_exactlyOneQuestion_gracefulRejection() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        setQuestionsReflectively(viewModel, listOf(SecurityQuestion("Q1", "A1")))

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)
    }

    @Test
    fun testMalformedQuestions_exactlyTwoQuestions_gracefulRejection() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        setQuestionsReflectively(
            viewModel,
            listOf(
                SecurityQuestion("Q1", "A1"),
                SecurityQuestion("Q2", "A2")
            )
        )

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)
    }

    @Test
    fun testMalformedQuestions_fourQuestionsWithThirdQuestionEmpty_gracefulRejection() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        // 4 项题目，前两项有效，第 3 项为空，第 4 项有效
        setQuestionsReflectively(
            viewModel,
            listOf(
                SecurityQuestion("Q1", "A1"),
                SecurityQuestion("Q2", "A2"),
                SecurityQuestion("", ""), // 第 3 项空
                SecurityQuestion("Q4", "A4")
            )
        )

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get(), "即使总数超过3，第3项为空时必须严格拦截")
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)
    }

    @Test
    fun testMalformedQuestions_whitespaceVariants_gracefulRejection() = runTest(testDispatcher) {
        val whitespaceVariants = listOf(
            "\n\r\t",
            "   \t\n",
            "\u3000\u3000", // 全角中文空格
            "\u00A0\u00A0", // 不换行空格
        )

        for (variant in whitespaceVariants) {
            val requestCount = AtomicInteger(0)
            val api = BikaDataSource(createTestClient {
                requestCount.incrementAndGet()
                respondOk()
            })
            val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

            setQuestionsReflectively(
                viewModel,
                listOf(
                    SecurityQuestion("Q1", "A1"),
                    SecurityQuestion("Q2", "A2"),
                    SecurityQuestion("ValidQ3", variant) // 答案仅由空白字符组成
                )
            )

            viewModel.registerUser()
            advanceUntilIdle()

            assertEquals(0, requestCount.get(), "空白字符变体应被 isBlank() 判定并拦截")
            val state = viewModel.registrationState.value
            assertIs<RegistrationUiState.Error>(state)
            assertEquals("请完整设置 3 个安全问题及答案", state.message)
        }
    }

    @Test
    fun testMalformedQuestions_onlyQuestionOrOnlyAnswer_gracefulRejection() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        // 仅有题目无答案
        setQuestionsReflectively(
            viewModel,
            listOf(
                SecurityQuestion("Q1", ""),
                SecurityQuestion("Q2", ""),
                SecurityQuestion("Q3", "")
            )
        )

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        assertIs<RegistrationUiState.Error>(viewModel.registrationState.value)

        // 仅有答案无题目
        setQuestionsReflectively(
            viewModel,
            listOf(
                SecurityQuestion("", "A1"),
                SecurityQuestion("", "A2"),
                SecurityQuestion("", "A3")
            )
        )

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        assertIs<RegistrationUiState.Error>(viewModel.registrationState.value)
    }

    @Test
    fun testMalformedQuestions_unicodeSpecialCharactersAndEmoji_passThroughSuccessfully() = runTest(testDispatcher) {
        var recordedBody: String? = null
        val api = BikaDataSource(createTestClient { request ->
            recordedBody = request.body.toString()
            respond(
                content = """{"code":200,"message":"success"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        viewModel.basicInfoState.email = "adv@test.com"
        viewModel.basicInfoState.password = "pass123"
        viewModel.basicInfoState.nickname = "tester"

        // 特殊字符：Emoji, 换行, JSON 注入字符
        val specialQ1 = "问题1: 🐱🐶🐼 🔥"
        val specialA1 = "答案1: <script>alert('xss')</script>"
        val specialQ2 = "问题2: {\"sql\":\"DROP TABLE users;\"}"
        val specialA2 = "答案2: 混合特殊字符\t\nLine2"
        val specialQ3 = "问题3: العربية Русский 日本語"
        val specialA3 = "答案3: 100%_PASS!"

        setQuestionsReflectively(
            viewModel,
            listOf(
                SecurityQuestion(specialQ1, specialA1),
                SecurityQuestion(specialQ2, specialA2),
                SecurityQuestion(specialQ3, specialA3)
            )
        )

        viewModel.registerUser()
        advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertEquals(RegistrationUiState.Success, state)
    }

    @Test
    fun testCancellationException_notSwallowedByViewModel() = runTest(testDispatcher) {
        val api = BikaDataSource(createTestClient {
            throw CancellationException("Job was cancelled externally")
        })
        val viewModel = RegistrationViewModel(api, FakeUserCredentialsDataSource())

        viewModel.securityQuestionsState.updateQuestion(0, "Q1")
        viewModel.securityQuestionsState.updateAnswer(0, "A1")
        viewModel.securityQuestionsState.updateQuestion(1, "Q2")
        viewModel.securityQuestionsState.updateAnswer(1, "A2")
        viewModel.securityQuestionsState.updateQuestion(2, "Q3")
        viewModel.securityQuestionsState.updateAnswer(2, "A3")

        viewModel.registerUser()
        advanceUntilIdle()

        // 验证：CancellationException 必须被重新抛出以协同协程取消，严禁被当作一般异常吞掉并设置为 Error 状态
        val state = viewModel.registrationState.value
        assertTrue(state !is RegistrationUiState.Error, "CancellationException 严禁被 catch(Exception) 吞噬转换为 Error 状态")
    }
}
