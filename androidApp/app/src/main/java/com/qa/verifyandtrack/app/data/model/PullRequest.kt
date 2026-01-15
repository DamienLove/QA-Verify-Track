package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Author(
    val name: String = "",
    val avatar: String = ""
) : Parcelable

@Parcelize
@IgnoreExtraProperties
data class PullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val branch: String = "",
    val targetBranch: String = "",
    val sourceOwner: String = "",
    val sourceRepo: String = "",
    val author: Author = Author(),
    val hasConflicts: Boolean = false,
    val isDraft: Boolean = false,
    val status: String = "open",
    val filesChanged: Int = 0,
    val conflictingFiles: List<String>? = null
) : Parcelable
