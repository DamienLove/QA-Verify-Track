package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TodoItem(
    val repoId: String = "",
    val repoName: String = "",
    val openIssueCount: Int? = null,
    val openPrCount: Int? = null
) : Parcelable
