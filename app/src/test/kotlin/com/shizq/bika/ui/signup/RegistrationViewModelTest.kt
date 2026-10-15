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
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

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
    ) : UserCredentialsDataSource(dataStore, cipher) {
        var savedUsername: String? = null
        var savedPassword: String? = null

        override suspend fun setUsername(username: String?) {
            savedUsername = username
            super.setUsername(username)
        }

        override suspend fun setPassword(password: String?) {
            savedPassword = password
            super.setPassword(password)
        }
    }

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

    @Test
    fun `密保问题数量不足3项时拦截并不发起网络请求`() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val credentialsDataSource = FakeUserCredentialsDataSource()
        val viewModel = RegistrationViewModel(api, credentialsDataSource)

        // 通过反射将 _questions 清空，模拟问题数量少于 3 项
        val questionsField = viewModel.securityQuestionsState.javaClass.getDeclaredField("_questions")
        questionsField.isAccessible = true
        val list = questionsField.get(viewModel.securityQuestionsState) as MutableList<*>
        list.clear()

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get(), "问题数量不足时严禁发起网络请求")
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)
    }

    @Test
    fun `密保问题存在空白题干或答案时拦截`() = runTest(testDispatcher) {
        val requestCount = AtomicInteger(0)
        val api = BikaDataSource(createTestClient {
            requestCount.incrementAndGet()
            respondOk()
        })
        val credentialsDataSource = FakeUserCredentialsDataSource()
        val viewModel = RegistrationViewModel(api, credentialsDataSource)

        // 默认题干为空白，直接点击注册应拦截
        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get(), "题目为空白时严禁发起网络请求")
        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("请完整设置 3 个安全问题及答案", state.message)

        // 设置前两项但第三项答案留白
        viewModel.securityQuestionsState.updateQuestion(0, "问题1")
        viewModel.securityQuestionsState.updateAnswer(0, "答案1")
        viewModel.securityQuestionsState.updateQuestion(1, "问题2")
        viewModel.securityQuestionsState.updateAnswer(1, "答案2")
        viewModel.securityQuestionsState.updateQuestion(2, "问题3")
        viewModel.securityQuestionsState.updateAnswer(2, "   ") // 空白字符串

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(0, requestCount.get())
        assertIs<RegistrationUiState.Error>(viewModel.registrationState.value)
    }

    @Test
    fun `密保问题完整且服务端返回200时保存凭据并成功`() = runTest(testDispatcher) {
        val api = BikaDataSource(createTestClient {
            respond(
                content = """{"code":200,"message":"success"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
        val credentialsDataSource = FakeUserCredentialsDataSource()
        val viewModel = RegistrationViewModel(api, credentialsDataSource)

        viewModel.basicInfoState.email = "test@example.com"
        viewModel.basicInfoState.password = "password123"
        viewModel.basicInfoState.nickname = "tester"

        viewModel.securityQuestionsState.updateQuestion(0, "Q1")
        viewModel.securityQuestionsState.updateAnswer(0, "A1")
        viewModel.securityQuestionsState.updateQuestion(1, "Q2")
        viewModel.securityQuestionsState.updateAnswer(1, "A2")
        viewModel.securityQuestionsState.updateQuestion(2, "Q3")
        viewModel.securityQuestionsState.updateAnswer(2, "A3")

        viewModel.registerUser()
        advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertEquals(RegistrationUiState.Success, state)
        assertEquals("test@example.com", credentialsDataSource.savedUsername)
        assertEquals("password123", credentialsDataSource.savedPassword)
    }

    @Test
    fun `服务端返回非200时更新为服务端错误信息`() = runTest(testDispatcher) {
        val api = BikaDataSource(createTestClient {
            respond(
                content = """{"code":400,"message":"email is already exist"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
        val credentialsDataSource = FakeUserCredentialsDataSource()
        val viewModel = RegistrationViewModel(api, credentialsDataSource)

        viewModel.basicInfoState.email = "test@example.com"
        viewModel.basicInfoState.password = "password123"
        viewModel.basicInfoState.nickname = "tester"

        viewModel.securityQuestionsState.updateQuestion(0, "Q1")
        viewModel.securityQuestionsState.updateAnswer(0, "A1")
        viewModel.securityQuestionsState.updateQuestion(1, "Q2")
        viewModel.securityQuestionsState.updateAnswer(1, "A2")
        viewModel.securityQuestionsState.updateQuestion(2, "Q3")
        viewModel.securityQuestionsState.updateAnswer(2, "A3")

        viewModel.registerUser()
        advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertEquals("email is already exist", state.message)
    }

    @Test
    fun `网络异常时收敛为Error状态而不崩溃`() = runTest(testDispatcher) {
        val api = BikaDataSource(createTestClient {
            throw IOException("Network unavailable")
        })
        val credentialsDataSource = FakeUserCredentialsDataSource()
        val viewModel = RegistrationViewModel(api, credentialsDataSource)

        viewModel.basicInfoState.email = "test@example.com"
        viewModel.basicInfoState.password = "password123"
        viewModel.basicInfoState.nickname = "tester"

        viewModel.securityQuestionsState.updateQuestion(0, "Q1")
        viewModel.securityQuestionsState.updateAnswer(0, "A1")
        viewModel.securityQuestionsState.updateQuestion(1, "Q2")
        viewModel.securityQuestionsState.updateAnswer(1, "A2")
        viewModel.securityQuestionsState.updateQuestion(2, "Q3")
        viewModel.securityQuestionsState.updateAnswer(2, "A3")

        viewModel.registerUser()
        advanceUntilIdle()

        val state = viewModel.registrationState.value
        assertIs<RegistrationUiState.Error>(state)
        assertTrue(state.message.contains("Network unavailable"))
    }
}
