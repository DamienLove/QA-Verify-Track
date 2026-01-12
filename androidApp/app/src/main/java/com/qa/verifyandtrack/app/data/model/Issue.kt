package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Reporter(
    val name: String = "",
    val avatar: String = ""
) : Parcelable

@Parcelize
@IgnoreExtraProperties
data class Issue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val description: String = "",
    val state: String = "open",
    val priority: String = "medium",
    val labels: List<String> = emptyList(),
    val type: String = "bug",
    val createdAt: String = "",
    val updatedAt: String = "",
    val commentsCount: Int = 0,
    val reporter: Reporter = Reporter(),
    val comments: List<Comment> = emptyList()
) : Parcelable
