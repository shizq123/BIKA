package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface DialogNavKey : NavKey

@Serializable
object ChannelSettingsNavKey : DialogNavKey