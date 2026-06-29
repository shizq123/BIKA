package com.shizq.bika.navigation

import kotlinx.serialization.Serializable

interface DialogNavKey : Connected

@Serializable
data object ChannelSettingsNavKey : DialogNavKey

@Serializable
data object UpdateNavKey : DialogNavKey