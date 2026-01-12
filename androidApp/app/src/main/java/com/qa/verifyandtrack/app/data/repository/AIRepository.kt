package com.qa.verifyandtrack.app.data.repository

import com.qa.verifyandtrack.app.data.service.AIService

class AIRepository(private val aiService: AIService) {
    suspend fun analyzeIssue(title: String, description: String): Result<String> =
        aiService.analyzeIssue(title, description)
}
