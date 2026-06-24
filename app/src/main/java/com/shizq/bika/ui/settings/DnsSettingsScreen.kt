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
    val domain: String,
    val latency: Long? = null, // null means untested, Long.MAX_VALUE means timeout, otherwise ms
    val isSelected: Boolean = false
)

data class DnsSettingsUiState(
    val isFetching: Boolean = false,
    val isTesting: Boolean = false,
    val error: String? = null,
    val lines: Map<String, List<IpTestResult>> = emptyMap(),
    val currentApiDnsSet: Set<String> = emptySet(),
    val currentImageDnsSet: Set<String> = emptySet()
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
                val deferred1 = async { fetchIpsForDomain("https://macapi1.com/app/picacomic/dns/resolve?domain=picacomic.com", "picacomic.com") }
                val deferred2 = async { fetchIpsForDomain("https://macapi2.com/app/picacomic/dns/resolve?domain=picaapi.picacomic.com", "picaapi.picacomic.com") }

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

                val userData = userPreferencesDataSource.userData.first()
                val currentApiDns = userData.apiDns
                val currentImageDns = userData.imageDns

                val grouped = combined.groupBy({ it.second }) { (ip, line, domain) ->
                    IpTestResult(
                        ip = ip,
                        lineName = line,
                        domain = domain,
                        latency = null,
                        isSelected = if (domain == "picacomic.com") {
                            currentImageDns.contains(ip)
                        } else {
                            currentApiDns.contains(ip)
                        }
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    lines = grouped,
                    currentApiDnsSet = currentApiDns,
                    currentImageDnsSet = currentImageDns
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

    private suspend fun fetchIpsForDomain(url: String, domain: String): List<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body.string()
                val parsed = json.decodeFromString<DnsResolveResponse>(body)
                val resultList = mutableListOf<Triple<String, String, String>>()
                parsed.data?.lines?.forEach { (lineName, dnsLine) ->
                    dnsLine.ips.forEach { ip ->
                        resultList.add(Triple(ip, lineName, domain))
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

    fun selectIp(ip: String, domain: String, lineName: String) {
        viewModelScope.launch {
            val selectedIp = ip
            val otherDomain = if (domain == "picacomic.com") "picaapi.picacomic.com" else "picacomic.com"

            // 寻找同线路下另一个域名的 IP 列表
            val otherDomainIpsInSameLine = _uiState.value.lines[lineName]
                ?.filter { it.domain == otherDomain }
                ?: emptyList()

            // 挑选另一个域名的最佳（延迟低/有效）IP，或者默认取第一个
            val bestOtherIp = otherDomainIpsInSameLine
                .filter { it.latency != null && it.latency != Long.MAX_VALUE }
                .minByOrNull { it.latency!! }
                ?.ip
                ?: otherDomainIpsInSameLine.firstOrNull()?.ip

            val finalApiDns: Set<String>
            val finalImageDns: Set<String>

            if (domain == "picacomic.com") {
                finalImageDns = setOf(selectedIp)
                finalApiDns = if (bestOtherIp != null) setOf(bestOtherIp) else userPreferencesDataSource.userData.first().apiDns
            } else {
                finalApiDns = setOf(selectedIp)
                finalImageDns = if (bestOtherIp != null) setOf(bestOtherIp) else userPreferencesDataSource.userData.first().imageDns
            }

            userPreferencesDataSource.updateDnsSettings(
                apiDns = finalApiDns,
                imageDns = finalImageDns,
                activeDnsLine = lineName
            )

            val updatedLines = _uiState.value.lines.mapValues { (_, list) ->
                list.map { item ->
                    val isSel = if (item.domain == "picacomic.com") {
                        finalImageDns.contains(item.ip)
                    } else {
                        finalApiDns.contains(item.ip)
                    }
                    item.copy(isSelected = isSel)
                }
            }

            _uiState.value = _uiState.value.copy(
                lines = updatedLines,
                currentApiDnsSet = finalApiDns,
                currentImageDnsSet = finalImageDns
            )
        }
    }

    fun applyLowestLatencyIp() {
        val allResults = _uiState.value.lines.values.flatten()
        val lowestApiResult = allResults
            .filter { it.domain == "picaapi.picacomic.com" && it.latency != null && it.latency != Long.MAX_VALUE }
            .minByOrNull { it.latency!! }
        val lowestApi = lowestApiResult?.ip

        val lowestImageResult = allResults
            .filter { it.domain == "picacomic.com" && it.latency != null && it.latency != Long.MAX_VALUE }
            .minByOrNull { it.latency!! }
        val lowestImage = lowestImageResult?.ip

        viewModelScope.launch {
            val currentData = userPreferencesDataSource.userData.first()
            val finalApiDns = if (lowestApi != null) setOf(lowestApi) else currentData.apiDns
            val finalImageDns = if (lowestImage != null) setOf(lowestImage) else currentData.imageDns

            val finalLineName = lowestApiResult?.lineName 
                ?: lowestImageResult?.lineName 
                ?: currentData.activeDnsLine

            userPreferencesDataSource.updateDnsSettings(
                apiDns = finalApiDns,
                imageDns = finalImageDns,
                activeDnsLine = finalLineName
            )

            val updatedLines = _uiState.value.lines.mapValues { (_, list) ->
                list.map { item ->
                    val isSel = if (item.domain == "picacomic.com") {
                        finalImageDns.contains(item.ip)
                    } else {
                        finalApiDns.contains(item.ip)
                    }
                    item.copy(isSelected = isSel)
                }
            }

            _uiState.value = _uiState.value.copy(
                lines = updatedLines,
                currentApiDnsSet = finalApiDns,
                currentImageDnsSet = finalImageDns
            )
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val defaultDns = setOf("104.21.20.188")
            userPreferencesDataSource.updateDnsSettings(defaultDns, defaultDns, "telecom")
            val updatedLines = _uiState.value.lines.mapValues { (_, list) ->
                list.map { item ->
                    item.copy(isSelected = defaultDns.contains(item.ip))
                }
            }
            _uiState.value = _uiState.value.copy(
                lines = updatedLines,
                currentApiDnsSet = defaultDns,
                currentImageDnsSet = defaultDns
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

            // 当前激活 IP 展示区（区分 API 和 图片展示）
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "API 线路",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.currentApiDnsSet.joinToString(", ").ifBlank { "默认系统解析" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "图片存储",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.currentImageDnsSet.joinToString(", ").ifBlank { "默认系统解析" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
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
                            onClick = { viewModel.selectIp(result.ip, result.domain, result.lineName) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.ip,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 显示域名类型标签
                    val tagText = if (result.domain == "picaapi.picacomic.com") "API" else "图片"
                    val tagBgColor = if (result.domain == "picaapi.picacomic.com") {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                    val tagTextColor = if (result.domain == "picaapi.picacomic.com") {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                    Box(
                        modifier = Modifier
                            .background(tagBgColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tagText,
                            style = MaterialTheme.typography.labelSmall,
                            color = tagTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
