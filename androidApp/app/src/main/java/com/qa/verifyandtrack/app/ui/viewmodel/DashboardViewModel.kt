package com.qa.verifyandtrack.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.export.ExportService
import com.qa.verifyandtrack.app.data.model.Issue
import com.qa.verifyandtrack.app.data.model.PullRequest
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.model.UserProfile
import com.qa.verifyandtrack.app.data.service.AdService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DashboardTab { Issues, PullRequests }

class DashboardViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository
    private val gitHubRepository = AppContainer.gitHubRepository
    private val aiRepository = AppContainer.aiRepository
    private val userProfileRepository = AppContainer.userProfileRepository

    private val _repo = MutableStateFlow<Repository?>(null)
    val repo: StateFlow<Repository?> = _repo

    private val _issues = MutableStateFlow<List<Issue>>(emptyList())
    val issues: StateFlow<List<Issue>> = _issues

    private val _pullRequests = MutableStateFlow<List<PullRequest>>(emptyList())
    val pullRequests: StateFlow<List<PullRequest>> = _pullRequests

    private val _selectedBuild = MutableStateFlow<String?>(null)
    val selectedBuild: StateFlow<String?> = _selectedBuild

    private val _activeTab = MutableStateFlow(DashboardTab.Issues)
    val activeTab: StateFlow<DashboardTab> = _activeTab

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _showPaywall = MutableStateFlow<String?>(null)
    val showPaywall: StateFlow<String?> = _showPaywall

    private var issueActionCount = 0

    init {
        // Observe user profile changes
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) flowOf(null)
                    else userProfileRepository.observeUserProfile(user.uid)
                }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }
    }

    fun setRepoId(repoId: String?) {
        if (repoId.isNullOrBlank()) {
            _error.value = "Missing repository ID."
            return
        }
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.uid
            if (userId == null) {
                _error.value = "No authenticated user."
                return@launch
            }
            val repos = repoRepository.getRepos(userId)
            val matched = repos.firstOrNull { it.id == repoId }
            _repo.value = matched
            if (matched != null) {
                syncGitHub()
            } else {
                _error.value = "Repository not found."
            }
        }
    }

    fun selectBuild(build: String?) {
        _selectedBuild.value = build
    }

    fun setActiveTab(tab: DashboardTab) {
        _activeTab.value = tab
    }

    fun syncGitHub() {
        val repo = _repo.value ?: return
        val token = repo.githubToken
        if (token.isNullOrBlank()) {
            _error.value = "Missing GitHub token for ${repo.displayLabel}."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(token)
                _issues.value = gitHubRepository.getIssues(repo.owner, repo.name, "open")
                _pullRequests.value = gitHubRepository.getPullRequests(repo.owner, repo.name)
            }
            _isLoading.value = false
        }
    }

    fun markIssueFixed(issueNumber: Int, buildNumber: String) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                if (buildNumber.isNotBlank()) {
                    gitHubRepository.addComment(repo.owner, repo.name, issueNumber, "Fixed in build $buildNumber")
                }
                gitHubRepository.updateIssueStatus(repo.owner, repo.name, issueNumber, "closed")
            }
            syncGitHub()
        }
    }

    fun markIssueOpen(issueNumber: Int) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.updateIssueStatus(repo.owner, repo.name, issueNumber, "open")
            }
            syncGitHub()
        }
    }

    fun blockIssue(issueNumber: Int, reason: String) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.addComment(repo.owner, repo.name, issueNumber, "Blocked: $reason")
            }
        }
    }

    fun analyzeIssue(issue: Issue) {
        viewModelScope.launch {
            _analysisResult.value = null
            val result = aiRepository.analyzeIssue(issue.title, issue.description)
            _analysisResult.value = result.getOrElse { it.message ?: "AI analysis failed." }
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = null
    }

    fun mergePR(prNumber: Int) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.mergePR(repo.owner, repo.name, prNumber)
            }
            if (result.isSuccess) {
                syncGitHub()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Merge failed."
            }
        }
    }

    fun denyPR(prNumber: Int) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.denyPR(repo.owner, repo.name, prNumber)
            }
            syncGitHub()
        }
    }

    fun resolveConflict(prNumber: Int) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.updateBranch(repo.owner, repo.name, prNumber)
            }
            syncGitHub()
        }
    }

    fun markReadyForReview(prNumber: Int) {
        val repo = _repo.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken ?: "")
                gitHubRepository.markReadyForReview(repo.owner, repo.name, prNumber)
            }
            if (result.isSuccess) {
                syncGitHub()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to mark PR ready for review."
            }
        }
    }

    fun onIssueAction(activity: Activity?, adService: AdService?) {
        val profile = _userProfile.value ?: return
        if (profile.showAds && activity != null && adService != null) {
            issueActionCount++
            if (issueActionCount >= 5) {
                adService.showInterstitialAd(activity, onAdDismissed = {}, forceShow = false)
                issueActionCount = 0
            }
        }
    }

    fun showPaywallFor(featureName: String) {
        _showPaywall.value = featureName
    }

    fun dismissPaywall() {
        _showPaywall.value = null
    }

    fun showError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun exportData(context: Context) {
        val repo = _repo.value ?: return
        val exportService = ExportService(context)

        viewModelScope.launch {
            val result = exportService.exportAndShare(
                repoName = "${repo.owner}/${repo.name}",
                issues = _issues.value,
                pullRequests = _pullRequests.value
            )

            if (result.isSuccess) {
                val shareIntent = result.getOrThrow()
                context.startActivity(Intent.createChooser(shareIntent, "Export QA Data"))
            } else {
                _error.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun showAdvancedFilters() {
        // TODO: Implement advanced filters sheet
        // Will be added in AdvancedFiltersSheet implementation
        _error.value = "Advanced filters coming soon!"
    }
}
