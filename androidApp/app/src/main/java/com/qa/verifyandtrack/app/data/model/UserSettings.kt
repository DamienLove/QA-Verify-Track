package com.qa.verifyandtrack.app.data.model

data class UserSettings(
    val repos: List<Repository> = emptyList(),
    val globalSettings: GlobalSettings? = null
)
