package com.shizq.bika.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@Serializable
data class DnsResolveResponse(
    val code: Int,
    val message: String,
    val data: DnsResolveData? = null
)

@Serializable
data class DnsResolveData(
    val domain: String,
    val lines: Map<String, DnsLine>? = null
)

@Serializable
data class DnsLine(
    val ips: List<String> = emptyList(),
    val status: String = ""
)

data class IpTestResult(
    val ip: String,
    val lineName: String,
    val latency: Long? = null, // null means untested, Long.MAX_VALUE means timeout, otherwise ms
    val isSelected: Boolean = false
)

data class DnsSettingsUiState(
    val isFetching: Boolean = false,
    val isTesting: Boolean = false,
    val error: String? = null,
    val lines: Map<String, List<IpTestResult>> = emptyMap(),
    val currentDnsSet: Set<String> = emptySet()
)

@HiltViewModel
class DnsSettingsViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsSettingsUiState())
    val uiState: StateFlow<DnsSettingsUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        loadAndTest()
    }

    fun loadAndTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetching = true, error = null)
            try {
                val deferred1 = async { fetchIpsForDomain("https://macapi1.com/app/picacomic/dns/resolve?domain=picacomic.com") }
                val deferred2 = async { fetchIpsForDomain("https://macapi2.com/app/picacomic/dns/resolve?domain=picaapi.picacomic.com") }

                val ips1 = deferred1.await()
                val ips2 = deferred2.await()

                val combined = (ips1 + ips2).distinctBy { it.first }
                if (combined.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        error = "未能通过 API 获取到任何 IP，请检查您的网络连接。"
                    )
                    return@launch
                }

                val currentDns = userPreferencesDataSource.userData.first().dns
                val grouped = combined.groupBy({ it.second }) { (ip, line) ->
                    IpTestResult(
                        ip = ip,
                        lineName = line,
                        latency = null,
                        isSelected = currentDns.contains(ip)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    lines = grouped,
                    currentDnsSet = currentDns
                )

                startLatencyTest()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    error = e.localizedMessage ?: "拉取 IP 列表失败"
                )
            }
        }
    }

    private suspend fun fetchIpsForDomain(url: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body.string()
                val parsed = json.decodeFromString<DnsResolveResponse>(body)
                val resultList = mutableListOf<Pair<String, String>>()
                parsed.data?.lines?.forEach { (lineName, dnsLine) ->
                    dnsLine.ips.forEach { ip ->
                        resultList.add(ip to lineName)
                    }
                }
                resultList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun startLatencyTest() {
        if (_uiState.value.isTesting) return
        _uiState.value = _uiState.value.copy(isTesting = true)

        viewModelScope.launch {
            val semaphore = Semaphore(6) // limit concurrent test requests
            val allResults = _uiState.value.lines.values.flatten()
            val jobs = allResults.map { result ->
                launch {
                    semaphore.withPermit {
                        val latency = testIpLatency(result.ip)
                        updateIpLatency(result.ip, latency)
                    }
                }
            }
            jobs.joinAll()
            _uiState.value = _uiState.value.copy(isTesting = false)
        }
    }

    private suspend fun testIpLatency(ip: String): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, 443), 2000)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun updateIpLatency(ip: String, latency: Long) {
        val currentLines = _uiState.value.lines.mapValues { (_, list) ->
            list.map { item ->
                if (item.ip == ip) {
                    item.copy(latency = latency)
                } else {
                    item
                }
            }
        }
        _uiState.value = _uiState.value.copy(lines = currentLines)
    }

    fun selectIp(ip: String) {
        viewModelScope.launch {
            val currentDns = setOf(ip)
            userPreferencesDataSource.overwriteDns(currentDns)
            val updatedLines = _uiState.value.lines.mapValues { (_, list) ->
                list.map { item ->
                    item.copy(isSelected = currentDns.contains(item.ip))
                }
            }
            _uiState.value = _uiState.value.copy(
                lines = updatedLines,
                currentDnsSet = currentDns
            )
        }
    }

    fun applyLowestLatencyIp() {
        val allResults = _uiState.value.lines.values.flatten()
        val lowest = allResults
            .filter { it.latency != null && it.latency != Long.MAX_VALUE }
            .minByOrNull { it.latency!! }
        if (lowest != null) {
            selectIp(lowest.ip)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val defaultDns = setOf("104.21.20.188")
            userPreferencesDataSource.overwriteDns(defaultDns)
            val updatedLines = _uiState.value.lines.mapValues { (_, list) ->
                list.map { item ->
                    item.copy(isSelected = defaultDns.contains(item.ip))
                }
            }
            _uiState.value = _uiState.value.copy(
                lines = updatedLines,
                currentDnsSet = defaultDns
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DnsSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS直连与分流优化") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetToDefault() },
                        enabled = !uiState.isFetching && !uiState.isTesting
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = "重置默认"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isFetching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (uiState.isTesting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在测量 IP 握手延迟...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 当前激活 IP 展示区
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "当前生效的直连 IP",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.currentDnsSet.joinToString(", ").ifBlank { "默认系统解析" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 操作控制按钮栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.loadAndTest() },
                    enabled = !uiState.isFetching && !uiState.isTesting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("获取并测速", fontSize = 14.sp)
                }

                Button(
                    onClick = { viewModel.applyLowestLatencyIp() },
                    enabled = !uiState.isFetching && !uiState.isTesting && uiState.lines.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("应用最低延迟", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 错误信息展示
            uiState.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 分流 IP 列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val groupNames = mapOf(
                    "unicom" to "联通分流线路",
                    "telecom" to "电信分流线路",
                    "mobile" to "移动分流线路",
                    "overseas" to "海外分流线路"
                )

                uiState.lines.forEach { (lineKey, ipResults) ->
                    val displayName = groupNames[lineKey.lowercase()] ?: "$lineKey 分流线路"
                    item {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(ipResults.size) { index ->
                        val result = ipResults[index]
                        IpRowItem(
                            result = result,
                            onClick = { viewModel.selectIp(result.ip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IpRowItem(
    result: IpTestResult,
    onClick: () -> Unit
) {
    val borderColor = if (result.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = if (result.isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = result.ip,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText: String
                    val statusColor: Color

                    when (result.latency) {
                        null -> {
                            statusText = "待测速"
                            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Long.MAX_VALUE -> {
                            statusText = "不可达/超时"
                            statusColor = MaterialTheme.colorScheme.error
                        }
                        else -> {
                            statusText = "${result.latency} ms"
                            statusColor = when {
                                result.latency < 100 -> Color(0xFF2E7D32) // 绿色
                                result.latency < 250 -> Color(0xFFF57F17) // 橙色
                                else -> Color(0xFFC62828) // 红色
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            if (result.isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "当前应用",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
