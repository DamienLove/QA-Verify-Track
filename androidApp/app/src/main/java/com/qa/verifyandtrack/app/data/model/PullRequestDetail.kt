package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class PullRequestDetail(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val mergeable: Boolean? = null,
    val mergeableState: String? = null,
    val isDraft: Boolean = false,
    val changedFiles: Int = 0
) : Parcelable
