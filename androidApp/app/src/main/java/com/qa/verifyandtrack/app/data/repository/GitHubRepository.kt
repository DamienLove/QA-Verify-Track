package com.qa.verifyandtrack.app.data.repository

import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.data.model.Issue
import com.qa.verifyandtrack.app.data.model.PullRequest
import com.qa.verifyandtrack.app.data.model.PullRequestDetail
import com.qa.verifyandtrack.app.data.service.GitHubService

class GitHubRepository(private val gitHubService: GitHubService) {
    fun initialize(token: String) = gitHubService.initialize(token)

    suspend fun getIssues(owner: String, repo: String, state: String = "open"): List<Issue> =
        gitHubService.getIssues(owner, repo, state)

    suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int, lastUpdated: String? = null): List<Comment> =
        gitHubService.getIssueComments(owner, repo, issueNumber, lastUpdated)

    suspend fun getPullRequests(owner: String, repo: String): List<PullRequest> =
        gitHubService.getPullRequests(owner, repo)

    suspend fun getPullRequest(owner: String, repo: String, pullNumber: Int): PullRequestDetail =
        gitHubService.getPullRequest(owner, repo, pullNumber)

    suspend fun createIssue(owner: String, repo: String, title: String, body: String, labels: List<String>): Issue =
        gitHubService.createIssue(owner, repo, title, body, labels)

    suspend fun addComment(owner: String, repo: String, issueNumber: Int, body: String): Boolean =
        gitHubService.addComment(owner, repo, issueNumber, body)

    suspend fun updateIssueStatus(owner: String, repo: String, issueNumber: Int, state: String): Boolean =
        gitHubService.updateIssueStatus(owner, repo, issueNumber, state)

    suspend fun mergePR(owner: String, repo: String, pullNumber: Int): Boolean =
        gitHubService.mergePR(owner, repo, pullNumber)

    suspend fun denyPR(owner: String, repo: String, pullNumber: Int): Boolean =
        gitHubService.denyPR(owner, repo, pullNumber)

    suspend fun updatePR(owner: String, repo: String, pullNumber: Int, updates: Map<String, Any>): Boolean =
        gitHubService.updatePR(owner, repo, pullNumber, updates)

    suspend fun updateBranch(owner: String, repo: String, pullNumber: Int): Boolean =
        gitHubService.updateBranch(owner, repo, pullNumber)

    suspend fun getOpenIssueCount(owner: String, repo: String): Int =
        gitHubService.getOpenIssueCount(owner, repo)
}
