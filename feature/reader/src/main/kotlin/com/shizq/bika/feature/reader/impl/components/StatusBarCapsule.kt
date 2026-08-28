package com.shizq.bika.feature.reader.impl.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Stable
class StatusBarState {
    var clockTime by mutableStateOf(getCurrentTime())
        private set
    var batteryPct by mutableIntStateOf(100)
        private set
    var isCharging by mutableStateOf(false)
        private set

    private fun getCurrentTime(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = now.hour.toString().padStart(2, '0')
        val minute = now.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    fun onTimeTick() {
        clockTime = getCurrentTime()
    }

    fun onBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            batteryPct = (level * 100) / scale
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}

@Composable
private fun rememberStatusBarState(context: Context): StatusBarState {
    val state = remember { StatusBarState() }

    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> state.onBatteryChanged(intent)
                    Intent.ACTION_TIME_TICK -> state.onTimeTick()
                }
            }
        }
        // ACTION_BATTERY_CHANGED 是 sticky broadcast，registerReceiver 会立即回调一次初始状态
        val initialIntent = context.registerReceiver(receiver, filter)
        initialIntent?.let { state.onBatteryChanged(it) }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return state
}

private val ChargingColor = Color(0xFF4CAF50)
private val LowBatteryColor = Color(0xFFF44336)
private val NormalBatteryColor = Color.White.copy(alpha = 0.8f)

@Composable
internal fun StatusBarCapsule(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = rememberStatusBarState(context)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .semantics {
                    contentDescription = "时间 ${state.clockTime}，电量 ${state.batteryPct}%" +
                        if (state.isCharging) "，正在充电" else ""
                }
        ) {
            Text(
                text = state.clockTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )

            val batteryColor = when {
                state.isCharging -> ChargingColor
                state.batteryPct <= 20 -> LowBatteryColor
                else -> NormalBatteryColor
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (state.isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = "${state.batteryPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = batteryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}