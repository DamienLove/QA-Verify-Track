package com.qa.verifyandtrack.app.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class AppConfig(
    val id: String = "",
    val name: String = "",
    val platform: String = "android",
    val playStoreUrl: String? = null,
    val buildNumber: String = "1"
)

@Keep
data class GlobalSettings(
    val globalGithubToken: String? = null,
    val defaultBranch: String = "main",
    val theme: String = "light"
)

@Keep
data class Repo(
    @DocumentId val id: String = "",
    val owner: String = "",
    val name: String = "",
    val displayName: String? = null,
    val apiEndpoint: String? = null,
    val githubToken: String? = null,
    val useCustomToken: Boolean = false,
    val avatarUrl: String? = null,
    val apps: List<AppConfig> = emptyList(),
    val isConnected: Boolean = true,
    val projects: List<String> = emptyList(),
    val templates: List<String> = emptyList(),
    val branch: String = "main"
) {
    val displayLabel: String get() = displayName ?: "${owner.ifBlank { "org" }}/${name.ifBlank { "repo" }}"
}
