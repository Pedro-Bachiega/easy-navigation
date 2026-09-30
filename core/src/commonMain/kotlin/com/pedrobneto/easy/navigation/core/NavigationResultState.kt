package com.pedrobneto.easy.navigation.core

import kotlinx.serialization.Serializable

@Serializable
internal data class NavigationResultState(
    val entries: List<NavigationResultStackEntry>,
    val nextEntryId: Long,
    val nextLauncherId: Long,
    val completions: List<NavigationResultCompletion> = emptyList(),
) {
    companion object {
        fun initial(entryCount: Int) = NavigationResultState(
            entries = List(entryCount) { index -> NavigationResultStackEntry(index.toLong()) },
            nextEntryId = entryCount.toLong(),
            nextLauncherId = 0,
        )
    }
}

@Serializable
internal data class NavigationResultStackEntry(
    val id: Long,
    val request: NavigationResultRequest? = null,
)

@Serializable
internal data class NavigationResultRequest(
    val launcherId: Long,
)

@Serializable
internal data class NavigationResultCompletion(
    val launcherId: Long,
    val valueJson: String? = null,
    val cancelled: Boolean = false,
)
