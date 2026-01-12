package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Repository(
    val id: String = "",
    val owner: String = "",
    val name: String = "",
    val displayName: String? = null,
    val apiEndpoint: String? = null,
    val githubToken: String? = null,
    val avatarUrl: String? = null,
    val apps: List<AppConfig> = emptyList(),
    val isConnected: Boolean = false,
    val projects: List<String>? = null,
    val templates: List<String>? = null
) : Parcelable {
    val fullName: String
        get() = if (owner.isBlank()) name else "$owner/$name"

    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: name
}
