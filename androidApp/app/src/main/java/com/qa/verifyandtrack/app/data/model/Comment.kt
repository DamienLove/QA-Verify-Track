package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Comment(
    val id: String = "",
    val text: String = "",
    val buildNumber: String? = null
) : Parcelable
