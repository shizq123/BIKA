package com.shizq.bika.navigation

import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.navigation.deeplink.DeepLinkPattern

/**
 * String resources
 */
internal const val STRING_LITERAL_SEARCH = "search"
internal const val PATH_BASE = "https://picaapi.picacomic.com"

internal val URL_SEARCH = "$PATH_BASE/" +
        "?c={${SearchKey::topic.name}}" +
        "&t={${SearchKey::tag.name}}" +
        "&a={${SearchKey::authorName.name}}" +
        "&ca={${SearchKey::knightId.name}}" +
        "&ct={${SearchKey::translationTeam.name}}"
internal val deepLinkPatterns: List<DeepLinkPattern<out NavKey>> = listOf(
    DeepLinkPattern(SearchKey.serializer(), (URL_SEARCH).toUri()),
)
