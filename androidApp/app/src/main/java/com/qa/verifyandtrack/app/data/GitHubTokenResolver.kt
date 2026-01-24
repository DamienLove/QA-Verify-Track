package com.qa.verifyandtrack.app.data

import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.Repository

fun resolveGithubToken(repo: Repository, globalSettings: GlobalSettings?): String? {
    val repoToken = repo.githubToken?.trim().orEmpty()
    val globalToken = globalSettings?.globalGithubToken?.trim().orEmpty()
    val useCustom = repo.useCustomToken != false
    val resolved = if (useCustom) {
        if (repoToken.isNotBlank()) repoToken else globalToken
    } else {
        if (globalToken.isNotBlank()) globalToken else repoToken
    }
    return resolved.ifBlank { null }
}
