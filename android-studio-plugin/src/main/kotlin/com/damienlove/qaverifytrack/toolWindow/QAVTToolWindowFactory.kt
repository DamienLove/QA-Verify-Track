package com.damienlove.qaverifytrack.toolWindow

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.JToolBar

class QAVTToolWindowFactory : ToolWindowFactory {
    private var projectRef: Project? = null
    private var lastRepoId: String? = null
    private var repoDashboardButton: JButton? = null
    private var repoConfigButton: JButton? = null
    private var repoQuickIssueButton: JButton? = null
    private var repoLabel: JLabel? = null
    private var autoFixEnabled: Boolean = false
    private var autoFixCommand: String? = null
    private var autoFixWorkingDir: String? = null
    private var autoFixToggle: JToggleButton? = null
    private var autoFixQuery: JBCefJSQuery? = null
    private var browserRef: JBCefBrowser? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        projectRef = project
        val props = PropertiesComponent.getInstance(project)
        autoFixEnabled = props.getBoolean(AUTO_FIX_ENABLED_KEY, false)
        autoFixCommand = props.getValue(AUTO_FIX_COMMAND_KEY, DEFAULT_AUTO_FIX_COMMAND)
        autoFixWorkingDir = props.getValue(AUTO_FIX_WORKDIR_KEY, project.basePath ?: "")

        val panel = JPanel(BorderLayout())
        if (JBCefApp.isSupported()) {
            val browser = JBCefBrowser(LOGIN_URL)
            browserRef = browser
            panel.add(createToolbar(browser), BorderLayout.WEST)
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
                    injectCompactUi(browser)
                    injectAutoFixBridge(browser)
                }
            }
        }, browser.cefBrowser)
        updateRepoContext(extractRepoId(browser.cefBrowser.url))
        injectCompactUi(browser)
        injectAutoFixBridge(browser)
    }

    private fun createToolbar(browser: JBCefBrowser): JToolBar {
        val toolbar = JToolBar(JToolBar.VERTICAL)
        toolbar.isFloatable = false
        toolbar.layout = BoxLayout(toolbar, BoxLayout.Y_AXIS)

        fun formatLabel(label: String): String {
            if (!label.contains(" ")) return label
            return "<html><center>${label.split(" ").joinToString("<br>")}</center></html>"
        }

        fun addButton(label: String, action: () -> Unit): JButton {
            val button = JButton(formatLabel(label))
            button.addActionListener { action() }
            button.maximumSize = Dimension(160, 44)
            button.alignmentX = 0.5f
            toolbar.add(button)
            return button
        }

        addButton("Back") { browser.cefBrowser.goBack() }
        addButton("Forward") { browser.cefBrowser.goForward() }
        addButton("Reload") { browser.cefBrowser.reload() }
        toolbar.addSeparator()
        addButton("Login") { browser.loadURL(LOGIN_URL) }
        addButton("Home") { browser.loadURL(HOME_URL) }
        addButton("Dashboard") { browser.loadURL(DASHBOARD_URL) }
        repoDashboardButton = addButton("Repo Dashboard") { openRepoDashboard(browser) }
        addButton("Config") { browser.loadURL(CONFIG_URL) }
        repoConfigButton = addButton("Repo Config") { openRepoConfig(browser) }
        addButton("Quick Issue") { browser.loadURL(QUICK_ISSUE_URL) }
        repoQuickIssueButton = addButton("Repo Quick Issue") { openRepoQuickIssue(browser) }
        toolbar.addSeparator()
        autoFixToggle = JToggleButton(formatLabel("Auto Fix")).apply {
            isSelected = autoFixEnabled
            addActionListener {
                autoFixEnabled = isSelected
                persistAutoFixSettings()
                injectAutoFixBridge(browser)
            }
            maximumSize = Dimension(160, 44)
            alignmentX = 0.5f
        }
        toolbar.add(autoFixToggle)
        addButton("Auto Fix Settings") { showAutoFixSettings() }
        toolbar.addSeparator()
        repoLabel = JLabel("Repo: -").apply {
            maximumSize = Dimension(160, 60)
            alignmentX = 0.5f
        }
        toolbar.add(repoLabel)
        toolbar.addSeparator()
        addButton("Open in Browser") { BrowserUtil.browse(HOME_URL) }

        updateRepoContext(lastRepoId)

        return toolbar
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
        val displayId = if (hasRepo) {
            val id = repoId.orEmpty()
            if (id.length > 24) "${id.take(10)}...${id.takeLast(10)}" else id
        } else {
            "-"
        }
        repoLabel?.text = "<html><center>Repo:<br>$displayId</center></html>"
    }

    private fun persistAutoFixSettings() {
        val project = projectRef ?: return
        val props = PropertiesComponent.getInstance(project)
        props.setValue(AUTO_FIX_ENABLED_KEY, autoFixEnabled)
        props.setValue(AUTO_FIX_COMMAND_KEY, autoFixCommand ?: DEFAULT_AUTO_FIX_COMMAND)
        props.setValue(AUTO_FIX_WORKDIR_KEY, autoFixWorkingDir ?: project.basePath.orEmpty())
    }

    private fun showAutoFixSettings() {
        val project = projectRef
        val currentCommand = autoFixCommand ?: DEFAULT_AUTO_FIX_COMMAND
        val updatedCommand = Messages.showInputDialog(
            project,
            "Command template for auto-fixing issues.\nUse placeholders like {repoOwner}, {repoName}, {issueNumber}, {title}, {description}, {buildNumber}, {issueUrl}.",
            "Auto Fix Command",
            null,
            currentCommand,
            null
        )
        if (updatedCommand != null) {
            autoFixCommand = updatedCommand.trim()
        }

        val defaultWorkDir = autoFixWorkingDir?.takeIf { it.isNotBlank() } ?: project?.basePath.orEmpty()
        val updatedWorkDir = Messages.showInputDialog(
            project,
            "Working directory for auto-fix command execution.",
            "Auto Fix Working Directory",
            null,
            defaultWorkDir,
            null
        )
        if (updatedWorkDir != null) {
            autoFixWorkingDir = updatedWorkDir.trim()
        }
        persistAutoFixSettings()
    }

    private fun injectAutoFixBridge(browser: JBCefBrowser) {
        val query = autoFixQuery ?: JBCefJSQuery.create(browser).also { created ->
            created.addHandler { payload ->
                handleAutoFixPayload(payload)
                null
            }
            autoFixQuery = created
        }
        val enabledFlag = if (autoFixEnabled) "true" else "false"
        val js = """
            window.QAVT_AUTO_FIX = window.QAVT_AUTO_FIX || {};
            window.QAVT_AUTO_FIX.enabled = $enabledFlag;
            window.QAVT_AUTO_FIX.issueCreated = function(payload) { ${query.inject("payload")}; };
            window.QAVT_AUTO_FIX.setEnabled = function(enabled) {
                var payload = "action=setEnabled&enabled=" + encodeURIComponent(String(!!enabled));
                ${query.inject("payload")};
            };
            window.dispatchEvent(new CustomEvent('qavt-auto-fix-changed', { detail: { enabled: window.QAVT_AUTO_FIX.enabled } }));
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    private fun injectCompactUi(browser: JBCefBrowser) {
        val css = """
            :root {
                font-size: 13px;
            }
            body {
                zoom: 0.85;
                line-height: 1.2;
            }
            .qavt-theme-grid {
                gap: 0.1rem;
                grid-template-columns: repeat(auto-fill, 8px);
                justify-content: start;
            }
            .qavt-theme-swatch {
                width: 8px !important;
                height: 8px !important;
                min-width: 8px !important;
                min-height: 8px !important;
                border-width: 1px;
                border-radius: 1px;
                box-shadow: none;
                transform: none !important;
            }
            .qavt-theme-picker {
                font-size: 10px;
            }
            .qavt-theme-picker label {
                font-size: 10px;
                letter-spacing: 0.04em;
            }
            .qavt-theme-picker p {
                display: none;
            }
            .qavt-theme-picker.qavt-theme-collapsed .qavt-theme-grid {
                display: none;
            }
        """.trimIndent()
        val js = """
            (function() {
                var id = "qavt-compact-style";
                if (document.getElementById(id)) return;
                var style = document.createElement("style");
                style.id = id;
                style.textContent = ${'"'}$css${'"'};
                document.head.appendChild(style);
                function attachToggle() {
                    var picker = document.querySelector(".qavt-theme-picker");
                    if (!picker) {
                        setTimeout(attachToggle, 500);
                        return;
                    }
                    if (picker.dataset.qavtToggleAttached) return;
                    picker.dataset.qavtToggleAttached = "true";
                    var label = picker.querySelector("label");
                    if (!label) return;
                    var toggle = document.createElement("button");
                    toggle.type = "button";
                    toggle.textContent = "Themes";
                    toggle.style.marginLeft = "6px";
                    toggle.style.padding = "1px 4px";
                    toggle.style.fontSize = "9px";
                    toggle.style.borderRadius = "4px";
                    toggle.style.border = "1px solid rgba(148,163,184,0.4)";
                    toggle.style.background = "rgba(15,23,42,0.2)";
                    toggle.style.color = "inherit";
                    toggle.setAttribute("aria-expanded", "false");
                    toggle.onclick = function() {
                        picker.classList.toggle("qavt-theme-collapsed");
                        var expanded = !picker.classList.contains("qavt-theme-collapsed");
                        toggle.setAttribute("aria-expanded", expanded ? "true" : "false");
                        toggle.textContent = expanded ? "Hide" : "Themes";
                    };
                    label.appendChild(toggle);
                    picker.classList.add("qavt-theme-collapsed");
                }
                attachToggle();
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    private fun handleAutoFixPayload(payload: String?) {
        if (payload.isNullOrBlank()) return
        val values = parsePayload(payload).toMutableMap()
        when (values["action"]) {
            "setEnabled" -> {
                val enabled = values["enabled"]?.equals("true", ignoreCase = true) == true
                autoFixEnabled = enabled
                autoFixToggle?.isSelected = enabled
                persistAutoFixSettings()
                browserRef?.let { injectAutoFixBridge(it) }
                return
            }
        }
        if (!autoFixEnabled) return
        val owner = values["repoOwner"].orEmpty()
        val repo = values["repoName"].orEmpty()
        val issueNumber = values["issueNumber"].orEmpty()
        if (owner.isNotBlank() && repo.isNotBlank() && issueNumber.isNotBlank()) {
            values["issueUrl"] = "https://github.com/$owner/$repo/issues/$issueNumber"
        }

        val commandTemplate = autoFixCommand ?: DEFAULT_AUTO_FIX_COMMAND
        val commandLine = expandTemplate(commandTemplate, values)
        val workingDir = autoFixWorkingDir?.takeIf { it.isNotBlank() } ?: projectRef?.basePath.orEmpty()
        runAutoFixCommand(commandLine, workingDir)
    }

    private fun parsePayload(payload: String): Map<String, String> {
        if (payload.isBlank()) return emptyMap()
        return payload.split("&")
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.isEmpty()) return@mapNotNull null
                val key = parts[0]
                val rawValue = if (parts.size > 1) parts[1] else ""
                val value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
                key to value
            }
            .toMap()
    }

    private fun expandTemplate(template: String, values: Map<String, String>): String {
        var output = template
        values.forEach { (key, value) ->
            output = output.replace("{$key}", value)
        }
        return output
    }

    private fun runAutoFixCommand(commandLine: String, workingDir: String) {
        val project = projectRef
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val command = buildShellCommand(commandLine)
                val processBuilder = ProcessBuilder(command)
                if (workingDir.isNotBlank()) {
                    processBuilder.directory(File(workingDir))
                }
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                val summary = if (exitCode == 0) {
                    "Auto-fix command completed successfully."
                } else {
                    "Auto-fix command failed with exit code $exitCode."
                }
                showAutoFixMessage("Auto Fix", "$summary\n\n$output", exitCode != 0)
            } catch (e: Exception) {
                showAutoFixMessage("Auto Fix Failed", e.message ?: "Unknown error.", true)
            }
        }
    }

    private fun buildShellCommand(commandLine: String): List<String> {
        return if (SystemInfo.isWindows) {
            listOf("cmd.exe", "/c", commandLine)
        } else {
            listOf("bash", "-lc", commandLine)
        }
    }

    private fun showAutoFixMessage(title: String, message: String, isError: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            val project = projectRef
            if (isError) {
                Messages.showErrorDialog(project, message, title)
            } else {
                Messages.showInfoMessage(project, message, title)
            }
        }
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
        const val LOGIN_URL = "https://qa-verify-and-ttack.web.app/#/login"
        const val DASHBOARD_URL = "https://qa-verify-and-ttack.web.app/#/dashboard"
        const val CONFIG_URL = "https://qa-verify-and-ttack.web.app/#/config"
        const val QUICK_ISSUE_URL = "https://qa-verify-and-ttack.web.app/#/quick-issue"
        const val AUTO_FIX_ENABLED_KEY = "qavt.autoFix.enabled"
        const val AUTO_FIX_COMMAND_KEY = "qavt.autoFix.command"
        const val AUTO_FIX_WORKDIR_KEY = "qavt.autoFix.workdir"
        const val DEFAULT_AUTO_FIX_COMMAND =
            "powershell -ExecutionPolicy Bypass -File scripts/auto_fix_and_publish.ps1 " +
                "-RepoOwner {repoOwner} -RepoName {repoName} -IssueNumber {issueNumber} " +
                "-Title \"{title}\" -Description \"{description}\" -BuildNumber {buildNumber}"
    }
}
