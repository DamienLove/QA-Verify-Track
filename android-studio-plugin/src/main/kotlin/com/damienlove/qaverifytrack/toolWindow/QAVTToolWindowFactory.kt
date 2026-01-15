package com.damienlove.qaverifytrack.toolWindow

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToolBar

class QAVTToolWindowFactory : ToolWindowFactory {
    private var lastRepoId: String? = null
    private var repoDashboardButton: JButton? = null
    private var repoConfigButton: JButton? = null
    private var repoQuickIssueButton: JButton? = null
    private var repoLabel: JLabel? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        if (JBCefApp.isSupported()) {
            val browser = JBCefBrowser(HOME_URL)
            panel.add(createToolbar(browser), BorderLayout.NORTH)
            panel.add(browser.component, BorderLayout.CENTER)
            attachRepoTracker(browser)
        } else {
            panel.add(createFallbackPanel(), BorderLayout.CENTER)
        }
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun attachRepoTracker(browser: JBCefBrowser) {
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame != null && frame.isMain) {
                    updateRepoContext(extractRepoId(cefBrowser?.url))
                }
            }
        }, browser.cefBrowser)
        updateRepoContext(extractRepoId(browser.cefBrowser.url))
    }

    private fun createToolbar(browser: JBCefBrowser): JPanel {
        val toolbar = JToolBar()
        toolbar.isFloatable = false

        fun addButton(label: String, action: () -> Unit): JButton {
            val button = JButton(label)
            button.addActionListener { action() }
            toolbar.add(button)
            return button
        }

        addButton("Back") { browser.cefBrowser.goBack() }
        addButton("Forward") { browser.cefBrowser.goForward() }
        addButton("Reload") { browser.cefBrowser.reload() }
        toolbar.addSeparator()
        addButton("Home") { browser.loadURL(HOME_URL) }
        addButton("Dashboard") { browser.loadURL(DASHBOARD_URL) }
        repoDashboardButton = addButton("Repo Dashboard") { openRepoDashboard(browser) }
        addButton("Config") { browser.loadURL(CONFIG_URL) }
        repoConfigButton = addButton("Repo Config") { openRepoConfig(browser) }
        addButton("Quick Issue") { browser.loadURL(QUICK_ISSUE_URL) }
        repoQuickIssueButton = addButton("Repo Quick Issue") { openRepoQuickIssue(browser) }
        toolbar.addSeparator()
        repoLabel = JLabel("Repo: —")
        toolbar.add(repoLabel)
        toolbar.addSeparator()
        addButton("Open in Browser") { BrowserUtil.browse(HOME_URL) }

        updateRepoContext(lastRepoId)

        return JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.CENTER)
        }
    }

    private fun openRepoDashboard(browser: JBCefBrowser) {
        val repoId = lastRepoId ?: return
        browser.loadURL(repoDashboardUrl(repoId))
    }

    private fun openRepoConfig(browser: JBCefBrowser) {
        val repoId = lastRepoId ?: return
        browser.loadURL(repoConfigUrl(repoId))
    }

    private fun openRepoQuickIssue(browser: JBCefBrowser) {
        val repoId = lastRepoId ?: return
        browser.loadURL(repoQuickIssueUrl(repoId))
    }

    private fun updateRepoContext(repoId: String?) {
        lastRepoId = repoId
        val hasRepo = !repoId.isNullOrBlank()
        repoDashboardButton?.isEnabled = hasRepo
        repoConfigButton?.isEnabled = hasRepo
        repoQuickIssueButton?.isEnabled = hasRepo
        repoLabel?.text = if (hasRepo) "Repo: $repoId" else "Repo: —"
    }

    private fun extractRepoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val hashIndex = url.indexOf('#')
        if (hashIndex == -1 || hashIndex == url.length - 1) return null
        val fragment = url.substring(hashIndex + 1)
        val path = fragment.substringBefore('?')
        val query = fragment.substringAfter('?', "")

        val queryRepo = queryParam(query, "repo")
        if (!queryRepo.isNullOrBlank()) return queryRepo

        val segments = path.trimStart('/').split('/')
        if (segments.size >= 2 && segments[0] == "issue") {
            return segments[1]
        }

        return null
    }

    private fun queryParam(query: String, key: String): String? {
        if (query.isBlank()) return null
        for (pair in query.split('&')) {
            if (pair.isBlank()) continue
            val parts = pair.split('=', limit = 2)
            if (parts[0] == key) {
                val value = if (parts.size == 2) parts[1] else ""
                return URLDecoder.decode(value, StandardCharsets.UTF_8)
            }
        }
        return null
    }

    private fun repoDashboardUrl(repoId: String) = "$DASHBOARD_URL?repo=${encode(repoId)}"

    private fun repoConfigUrl(repoId: String) = "$CONFIG_URL?repo=${encode(repoId)}"

    private fun repoQuickIssueUrl(repoId: String) = "$QUICK_ISSUE_URL?repo=${encode(repoId)}"

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun createFallbackPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val label = JLabel(
            "<html>Embedded QA Verify &amp; Track needs JCEF, which is unavailable in this IDE build. " +
                "Open the web app in your browser to access repos, issues, pull requests, and tests.</html>"
        )
        val link = LinkLabel<String>("Open QA Verify & Track", null) { _, _ ->
            BrowserUtil.browse(HOME_URL)
        }

        panel.add(label, BorderLayout.CENTER)
        panel.add(JPanel(FlowLayout(FlowLayout.LEFT)).apply { add(link) }, BorderLayout.SOUTH)
        return panel
    }

    private companion object {
        const val HOME_URL = "https://qa-verify-and-ttack.web.app/#/"
        const val DASHBOARD_URL = "https://qa-verify-and-ttack.web.app/#/dashboard"
        const val CONFIG_URL = "https://qa-verify-and-ttack.web.app/#/config"
        const val QUICK_ISSUE_URL = "https://qa-verify-and-ttack.web.app/#/quick-issue"
    }
}
