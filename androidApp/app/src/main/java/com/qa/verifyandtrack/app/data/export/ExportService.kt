package com.qa.verifyandtrack.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.qa.verifyandtrack.app.data.model.Issue
import com.qa.verifyandtrack.app.data.model.PullRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportService(private val context: Context) {

    /**
     * Exports issues and pull requests to a CSV file and returns the file for sharing
     */
    fun exportToCSV(
        repoName: String,
        issues: List<Issue>,
        pullRequests: List<PullRequest>
    ): Result<File> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "QA_${repoName.replace("/", "_")}_$timestamp.csv"
            val file = File(context.cacheDir, fileName)

            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("Type,Number,Title,State,Priority,Created,Labels\n")

                // Write issues
                issues.forEach { issue ->
                    val created = issue.createdAt.takeIf { it.isNotBlank() }?.let {
                        try {
                            // Try to parse ISO date format and simplify it
                            it.substringBefore('T')
                        } catch (e: Exception) {
                            it
                        }
                    } ?: "N/A"
                    val labels = issue.labels.joinToString(";")
                    writer.write("Issue,${issue.number},\"${escapeCsv(issue.title)}\",${issue.state},${issue.priority},$created,\"$labels\"\n")
                }

                // Write pull requests
                pullRequests.forEach { pr ->
                    val state = if (pr.isDraft) "draft" else pr.status
                    writer.write("PR,${pr.number},\"${escapeCsv(pr.title)}\",$state,N/A,N/A,\"${pr.branch} -> ${pr.targetBranch}\"\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the CSV file using Android's share sheet
     */
    fun shareCSV(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "QA Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Exports and shares in one operation
     */
    fun exportAndShare(
        repoName: String,
        issues: List<Issue>,
        pullRequests: List<PullRequest>
    ): Result<Intent> {
        val result = exportToCSV(repoName, issues, pullRequests)
        return if (result.isSuccess) {
            val file = result.getOrThrow()
            Result.success(shareCSV(file))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Export failed"))
        }
    }

    /**
     * Escapes CSV special characters (quotes and commas)
     */
    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
