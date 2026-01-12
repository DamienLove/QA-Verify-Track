package com.qa.verifyandtrack.app.data.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.qa.verifyandtrack.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIService {
    suspend fun analyzeIssue(title: String, description: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.success(
                "AI analysis disabled (no Gemini API key configured). Add GEMINI_API_KEY to local.properties to enable."
            )
        }
        return@withContext runCatching {
            val prompt = "You are a QA Lead. Analyze this bug report and provide 3 concise bullet points: " +
                "1) Potential root cause, 2) Key verification step, 3) Severity assessment.\n\n" +
                "Bug: $title\nDetails: $description"
            val model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = GenerationConfig(temperature = 0.2f)
            )
            val response = model.generateContent(prompt)
            response.text ?: "No analysis available."
        }.recover {
            "Unable to analyze issue at this time. Please check your network or API key."
        }
    }
}
