package com.qa.verifyandtrack.app.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class AppConfig(
    val id: String = "",
    val name: String = "",
    val platform: String = "android",
    val playStoreUrl: String? = null,
    val buildNumber: String = ""
) : Parcelable
