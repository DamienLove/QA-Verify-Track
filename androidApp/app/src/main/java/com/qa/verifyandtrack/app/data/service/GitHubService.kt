package com.qa.verifyandtrack.app.data.service

import com.google.gson.annotations.SerializedName
import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.data.model.Issue
import com.qa.verifyandtrack.app.data.model.PullRequest
import com.qa.verifyandtrack.app.data.model.PullRequestDetail
import com.qa.verifyandtrack.app.data.model.PullRequestFile
import com.qa.verifyandtrack.app.data.model.Reporter
import com.qa.verifyandtrack.app.data.model.Author
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

private const val GITHUB_BASE_URL = "https://api.github.com/"

class GitHubService {
    @Volatile
    private var authToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "QA-Verify-Track-Android")
        val token = authToken
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "token $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val api: GitHubApi

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GitHubApi::class.java)
    }

    fun initialize(token: String) {
        authToken = token
    }

    private fun requireToken() {
        if (authToken.isNullOrBlank()) {
            throw IllegalStateException("GitHub service not initialized. Configure your token first.")
        }
    }

    suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int, lastUpdated: String? = null): List<Comment> {
        requireToken()
        val cacheKey = "$owner/$repo/$issueNumber"
        if (lastUpdated != null) {
            val cached = commentsCache[cacheKey]
            if (cached != null && cached.timestamp == lastUpdated) {
                return cached.data
            }
        }
        val comments = mutableListOf<Comment>()
        var page = 1
        val perPage = 100
        while (true) {
            val response = api.listIssueComments(owner, repo, issueNumber, page = page, perPage = perPage)
            if (response.isEmpty()) break
            comments.addAll(response.map { Comment(id = it.id.toString(), text = it.body.orEmpty()) })
            if (response.size < perPage) break
            page++
        }
        if (lastUpdated != null) {
            if (commentsCache.size > 500) {
                commentsCache.clear()
            }
            commentsCache[cacheKey] = CachedComments(lastUpdated, comments)
        }
        return comments
    }

    suspend fun getIssues(owner: String, repo: String, state: String = "open"): List<Issue> {
        requireToken()
        return try {
            val allIssues = mutableListOf<IssueResponse>()
            var page = 1
            val perPage = 100
            while (true) {
                val pageItems = api.listIssues(owner, repo, state, page = page, perPage = perPage)
                if (pageItems.isEmpty()) break
                allIssues.addAll(pageItems)
                if (pageItems.size < perPage) break
                page++
            }
            allIssues
                .filter { it.pullRequest == null }
                .map { mapIssue(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPullRequests(owner: String, repo: String, state: String = "open", head: String? = null): List<PullRequest> {
        requireToken()
        return try {
            val allPulls = mutableListOf<PullRequestResponse>()
            var page = 1
            val perPage = 100
            while (true) {
                val pageItems = api.listPulls(owner, repo, state = state, head = head, page = page, perPage = perPage)
                if (pageItems.isEmpty()) break
                allPulls.addAll(pageItems)
                if (pageItems.size < perPage) break
                page++
            }
            allPulls.map { mapPullRequest(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPullRequest(owner: String, repo: String, pullNumber: Int): PullRequestDetail {
        requireToken()
        val response = api.getPull(owner, repo, pullNumber)
        return PullRequestDetail(
            id = response.id ?: 0,
            number = response.number ?: pullNumber,
            title = response.title.orEmpty(),
            nodeId = response.nodeId.orEmpty(),
            mergeable = response.mergeable,
            mergeableState = response.mergeableState,
            isDraft = response.draft ?: false,
            changedFiles = response.changedFiles ?: 0
        )
    }

    suspend fun getPullRequestFiles(owner: String, repo: String, pullNumber: Int): List<PullRequestFile> {
        requireToken()
        return try {
            val files = mutableListOf<PullRequestFileResponse>()
            var page = 1
            val perPage = 100
            while (true) {
                val pageItems = api.listPullFiles(owner, repo, pullNumber, page = page, perPage = perPage)
                if (pageItems.isEmpty()) break
                files.addAll(pageItems)
                if (pageItems.size < perPage) break
                page++
            }
            files.map { mapPullFile(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createIssue(owner: String, repo: String, title: String, body: String, labels: List<String>): Issue {
        requireToken()
        val response = api.createIssue(owner, repo, CreateIssueRequest(title, body, labels))
        return mapIssue(response)
    }

    suspend fun addComment(owner: String, repo: String, issueNumber: Int, body: String): Boolean {
        requireToken()
        api.createComment(owner, repo, issueNumber, CreateCommentRequest(body))
        return true
    }

    suspend fun updateIssueStatus(owner: String, repo: String, issueNumber: Int, state: String): Boolean {
        requireToken()
        api.updateIssue(owner, repo, issueNumber, UpdateIssueRequest(state))
        return true
    }

    suspend fun mergePR(owner: String, repo: String, pullNumber: Int): Result<Unit> {
        requireToken()
        return try {
            val response = api.mergePull(owner, repo, pullNumber)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(responseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markReadyForReview(owner: String, repo: String, pullNumber: Int): Result<Unit> {
        requireToken()
        return try {
            val details = getPullRequest(owner, repo, pullNumber)
            if (!details.isDraft) {
                return Result.success(Unit)
            }
            val nodeId = details.nodeId
            if (nodeId.isBlank()) {
                val response = api.readyForReview(owner, repo, pullNumber)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException(responseErrorMessage(response)))
                }
            } else {
                val mutation = "mutation(${'$'}id: ID!) { markPullRequestReadyForReview(input: {pullRequestId: ${'$'}id}) { clientMutationId } }"
                val response = api.graphql(GraphQlRequest(query = mutation, variables = mapOf("id" to nodeId)))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException(responseErrorMessage(response)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun denyPR(owner: String, repo: String, pullNumber: Int): Boolean {
        requireToken()
        api.updatePull(owner, repo, pullNumber, UpdatePullRequest(state = "closed"))
        return true
    }

    suspend fun updatePR(owner: String, repo: String, pullNumber: Int, updates: Map<String, Any>): Boolean {
        requireToken()
        api.updatePullRaw(owner, repo, pullNumber, updates)
        return true
    }

    suspend fun updateBranch(owner: String, repo: String, pullNumber: Int): Boolean {
        requireToken()
        return try {
            api.updateBranch(owner, repo, pullNumber)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getOpenIssueCount(owner: String, repo: String): Int {
        requireToken()
        val response = api.searchIssues("repo:$owner/$repo is:issue is:open")
        return response.totalCount ?: 0
    }

    suspend fun deleteBranch(owner: String, repo: String, branch: String): Result<Unit> {
        requireToken()
        if (branch.isBlank()) return Result.failure(IllegalArgumentException("Branch name is blank."))
        return try {
            val response = api.deleteBranch(owner, repo, branch)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(responseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapIssue(issue: IssueResponse): Issue {
        val labels = issue.labels?.mapNotNull { it.name } ?: emptyList()
        val priority = mapPriority(labels)
        val type = labels.firstOrNull { label ->
            val normalized = label.lowercase()
            normalized == "bug" || normalized == "feature" || normalized == "ui"
        } ?: "bug"
        return Issue(
            id = issue.id ?: 0,
            number = issue.number ?: 0,
            title = issue.title.orEmpty(),
            description = issue.body.orEmpty(),
            state = issue.state.orEmpty(),
            priority = priority,
            labels = labels,
            type = type,
            createdAt = issue.createdAt.orEmpty(),
            updatedAt = issue.updatedAt.orEmpty(),
            commentsCount = issue.comments ?: 0,
            reporter = Reporter(
                name = issue.user?.login.orEmpty(),
                avatar = issue.user?.avatarUrl.orEmpty()
            ),
            comments = emptyList()
        )
    }

    private fun mapPullRequest(pr: PullRequestResponse): PullRequest {
        return PullRequest(
            id = pr.id ?: 0,
            number = pr.number ?: 0,
            title = pr.title.orEmpty(),
            branch = pr.head?.ref.orEmpty(),
            targetBranch = pr.base?.ref.orEmpty(),
            sourceOwner = pr.head?.repo?.owner?.login.orEmpty(),
            sourceRepo = pr.head?.repo?.name.orEmpty(),
            author = Author(
                name = pr.user?.login.orEmpty(),
                avatar = pr.user?.avatarUrl.orEmpty()
            ),
            hasConflicts = false,
            isDraft = pr.draft ?: false,
            status = if (pr.draft == true) "draft" else "open",
            filesChanged = 0
        )
    }

    private fun mapPullFile(file: PullRequestFileResponse): PullRequestFile {
        return PullRequestFile(
            filename = file.filename.orEmpty(),
            status = file.status.orEmpty(),
            additions = file.additions ?: 0,
            deletions = file.deletions ?: 0,
            changes = file.changes ?: 0,
            patch = file.patch,
            previousFilename = file.previousFilename
        )
    }

    private fun mapPriority(labels: List<String>): String {
        val names = labels.map { it.lowercase() }
        return when {
            names.any { it == "critical" || it.contains("p0") || it.contains("sev: critical") || it.contains("severity: critical") } -> "critical"
            names.any { it == "high" || it.contains("p1") || it.contains("priority: high") || it.contains("severity: high") } -> "high"
            names.any { it == "medium" || it.contains("p2") || it.contains("priority: medium") || it.contains("severity: medium") } -> "medium"
            names.any { it == "low" || it.contains("p3") || it.contains("priority: low") || it.contains("severity: low") || it.contains("minor") } -> "low"
            else -> "medium"
        }
    }

    private data class CachedComments(val timestamp: String, val data: List<Comment>)

    private val commentsCache: MutableMap<String, CachedComments> = mutableMapOf()

    private fun responseErrorMessage(response: Response<*>): String {
        val code = response.code()
        val body = response.errorBody()?.string()
        val message = body?.let {
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1)
        }
        return if (!message.isNullOrBlank()) {
            "GitHub API error ($code): $message"
        } else {
            "GitHub API error ($code)."
        }
    }
}

private interface GitHubApi {
    @GET("repos/{owner}/{repo}/issues")
    suspend fun listIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String,
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<IssueResponse>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun listIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<CommentResponse>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun listPulls(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("head") head: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<PullRequestResponse>

    @GET("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun getPull(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): PullRequestDetailResponse

    @GET("repos/{owner}/{repo}/pulls/{pull_number}/files")
    suspend fun listPullFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<PullRequestFileResponse>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): IssueResponse

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body request: CreateCommentRequest
    ): CommentResponse

    @PATCH("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body request: UpdateIssueRequest
    ): IssueResponse

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    suspend fun mergePull(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/pulls/{pull_number}/ready_for_review")
    suspend fun readyForReview(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): Response<Unit>

    @POST("graphql")
    suspend fun graphql(@Body request: GraphQlRequest): Response<Any>

    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun updatePull(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body request: UpdatePullRequest
    ): PullRequestDetailResponse

    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun updatePullRaw(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body updates: Map<String, Any>
    ): Response<Unit>

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/update-branch")
    @Headers("Accept: application/vnd.github+json")
    suspend fun updateBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): Response<Unit>

    @DELETE("repos/{owner}/{repo}/git/refs/heads/{ref}")
    suspend fun deleteBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String
    ): Response<Unit>

    @GET("search/issues")
    suspend fun searchIssues(@Query("q") query: String): SearchIssuesResponse
}

private data class LabelResponse(
    val name: String? = null
)

private data class UserResponse(
    val login: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

private data class IssueResponse(
    val id: Long? = null,
    val number: Int? = null,
    val title: String? = null,
    @SerializedName("body") val body: String? = null,
    val state: String? = null,
    val labels: List<LabelResponse>? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("comments") val comments: Int? = null,
    val user: UserResponse? = null,
    @SerializedName("pull_request") val pullRequest: Any? = null
)

private data class PullRefResponse(
    val ref: String? = null,
    val repo: PullRepoResponse? = null
)

private data class PullRepoResponse(
    val name: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    val owner: PullRepoOwnerResponse? = null
)

private data class PullRepoOwnerResponse(
    val login: String? = null
)

private data class PullRequestResponse(
    val id: Long? = null,
    val number: Int? = null,
    val title: String? = null,
    val head: PullRefResponse? = null,
    val base: PullRefResponse? = null,
    val user: UserResponse? = null,
    val draft: Boolean? = null
)

private data class PullRequestDetailResponse(
    val id: Long? = null,
    val number: Int? = null,
    val title: String? = null,
    @SerializedName("node_id") val nodeId: String? = null,
    val mergeable: Boolean? = null,
    @SerializedName("mergeable_state") val mergeableState: String? = null,
    val draft: Boolean? = null,
    @SerializedName("changed_files") val changedFiles: Int? = null
)

private data class PullRequestFileResponse(
    val filename: String? = null,
    val status: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val changes: Int? = null,
    val patch: String? = null,
    @SerializedName("previous_filename") val previousFilename: String? = null
)

private data class CommentResponse(
    val id: Long? = null,
    @SerializedName("body") val body: String? = null
)

private data class SearchIssuesResponse(
    @SerializedName("total_count") val totalCount: Int? = null
)

private data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>
)

private data class CreateCommentRequest(
    val body: String
)

private data class UpdateIssueRequest(
    val state: String
)

private data class UpdatePullRequest(
    val state: String? = null,
    val draft: Boolean? = null
)

private data class GraphQlRequest(
    val query: String,
    val variables: Map<String, Any>
)
