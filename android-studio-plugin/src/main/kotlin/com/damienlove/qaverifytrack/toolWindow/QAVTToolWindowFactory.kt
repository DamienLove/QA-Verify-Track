package com.damienlove.qaverifytrack.toolWindow

import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.util.PropertiesComponent
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
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
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JToggleButton
import javax.swing.JToolBar
import javax.swing.ScrollPaneConstants

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
    private var autoFixProvider: String = DEFAULT_AUTO_FIX_PROVIDER
    private var autoFixAiCommand: String? = null
    private var autoFixToggle: JToggleButton? = null
    private var autoFixQuery: JBCefJSQuery? = null
    private var browserRef: JBCefBrowser? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        projectRef = project
        val props = PropertiesComponent.getInstance(project)
        autoFixEnabled = props.getBoolean(AUTO_FIX_ENABLED_KEY, false)
        val defaultCommand = resolveDefaultAutoFixCommand(project)
        val defaultWorkDir = resolveDefaultAutoFixWorkingDir(project)
        autoFixCommand = props.getValue(AUTO_FIX_COMMAND_KEY, defaultCommand)
        autoFixWorkingDir = props.getValue(AUTO_FIX_WORKDIR_KEY, defaultWorkDir)
        autoFixProvider = normalizeProvider(props.getValue(AUTO_FIX_PROVIDER_KEY, DEFAULT_AUTO_FIX_PROVIDER))
        autoFixAiCommand = props.getValue(AUTO_FIX_AI_COMMAND_KEY)?.trim().takeIf { !it.isNullOrBlank() }

        val panel = JPanel(BorderLayout())
        if (JBCefApp.isSupported()) {
            val browser = JBCefBrowser(LOGIN_URL)
            browserRef = browser
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
                    injectCompactUi(browser)
                    injectAutoFixBridge(browser)
                }
            }
        }, browser.cefBrowser)
        updateRepoContext(extractRepoId(browser.cefBrowser.url))
        injectCompactUi(browser)
        injectAutoFixBridge(browser)
    }

    private fun createToolbar(browser: JBCefBrowser): JComponent {
        val toolbar = JToolBar(JToolBar.HORIZONTAL)
        toolbar.isFloatable = false
        toolbar.layout = FlowLayout(FlowLayout.LEFT, 6, 4)

        fun formatLabel(label: String): String {
            return label
        }

        fun addButton(label: String, action: () -> Unit): JButton {
            val button = JButton(formatLabel(label))
            button.addActionListener { action() }
            button.preferredSize = Dimension(140, 32)
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
            preferredSize = Dimension(120, 32)
        }
        toolbar.add(autoFixToggle)
        addButton("Auto Fix Settings") { showAutoFixSettings() }
        toolbar.addSeparator()
        repoLabel = JLabel("Repo: -").apply {
            preferredSize = Dimension(220, 32)
        }
        toolbar.add(repoLabel)
        toolbar.addSeparator()
        addButton("Open in Browser") { BrowserUtil.browse(HOME_URL) }

        updateRepoContext(lastRepoId)
        val scroll = JScrollPane(toolbar, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        scroll.border = null
        scroll.preferredSize = Dimension(0, 48)
        return scroll
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
        props.setValue(AUTO_FIX_COMMAND_KEY, autoFixCommand ?: resolveDefaultAutoFixCommand(project))
        props.setValue(AUTO_FIX_WORKDIR_KEY, autoFixWorkingDir ?: resolveDefaultAutoFixWorkingDir(project))
        props.setValue(AUTO_FIX_PROVIDER_KEY, autoFixProvider)
        props.setValue(AUTO_FIX_AI_COMMAND_KEY, autoFixAiCommand ?: "")
    }

    private fun showAutoFixSettings() {
        val project = projectRef
        val providerLabels = PROVIDER_LABELS.keys.toTypedArray()
        val providerCombo = JComboBox(providerLabels)
        providerCombo.selectedItem = providerLabelForId(autoFixProvider)

        val apiKeyField = JBPasswordField()
        val clearKeyCheckbox = JBCheckBox("Clear saved key for provider")
        val aiCommandField = JBTextField(autoFixAiCommand ?: "")
        val commandField = JBTextField(autoFixCommand ?: resolveDefaultAutoFixCommand(project))
        val workdirField = JBTextField(autoFixWorkingDir ?: resolveDefaultAutoFixWorkingDir(project))

        val infoLabel = JBLabel(
            "<html><b>Auto Fix</b> uses a local CLI (codex, claude, gemini). " +
                "Select a provider and save the API key. Enable Auto Fix in the toolbar, " +
                "then submit a Quick Issue to run.</html>"
        )

        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 6, 4, 6)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            weightx = 1.0
            gridx = 0
            gridy = 0
            gridwidth = 2
        }
        panel.add(infoLabel, gbc)

        gbc.gridy++
        gbc.gridwidth = 1
        panel.add(JBLabel("Provider"), gbc)
        gbc.gridx = 1
        panel.add(providerCombo, gbc)

        gbc.gridy++
        gbc.gridx = 0
        panel.add(JBLabel("API Key (stored securely)"), gbc)
        gbc.gridx = 1
        panel.add(apiKeyField, gbc)

        gbc.gridy++
        gbc.gridx = 1
        panel.add(clearKeyCheckbox, gbc)

        gbc.gridy++
        gbc.gridx = 0
        panel.add(JBLabel("LLM command template (optional)"), gbc)
        gbc.gridx = 1
        panel.add(aiCommandField, gbc)

        gbc.gridy++
        gbc.gridx = 0
        panel.add(JBLabel("Auto-fix command"), gbc)
        gbc.gridx = 1
        panel.add(commandField, gbc)

        gbc.gridy++
        gbc.gridx = 0
        panel.add(JBLabel("Working directory"), gbc)
        gbc.gridx = 1
        panel.add(workdirField, gbc)

        val builder = DialogBuilder(project)
        builder.setTitle("Auto Fix Settings")
        builder.setCenterPanel(panel)
        builder.setPreferredFocusComponent(providerCombo)
        if (!builder.showAndGet()) return

        val selectedLabel = providerCombo.selectedItem as? String
        autoFixProvider = normalizeProvider(providerIdForLabel(selectedLabel))
        autoFixAiCommand = aiCommandField.text.trim().takeIf { it.isNotEmpty() }
        autoFixCommand = commandField.text.trim().ifBlank { resolveDefaultAutoFixCommand(project) }
        autoFixWorkingDir = workdirField.text.trim().ifBlank { resolveDefaultAutoFixWorkingDir(project) }

        val keyInput = String(apiKeyField.password).trim()
        if (clearKeyCheckbox.isSelected) {
            saveApiKey(autoFixProvider, null)
        } else if (keyInput.isNotEmpty()) {
            saveApiKey(autoFixProvider, keyInput)
        }
        persistAutoFixSettings()
    }

    private fun resolveDefaultAutoFixWorkingDir(project: Project?): String {
        val script = resolveAutoFixScript(project)
        return script?.parentFile?.parentFile?.absolutePath ?: project?.basePath.orEmpty()
    }

    private fun resolveDefaultAutoFixCommand(project: Project?): String {
        val scriptPath = resolveAutoFixScript(project)?.absolutePath ?: DEFAULT_AUTO_FIX_SCRIPT_PATH
        val quotedScript = if (scriptPath.contains(" ")) "\"$scriptPath\"" else scriptPath
        return "powershell -ExecutionPolicy Bypass -File $quotedScript " +
            "-RepoOwner {repoOwner} -RepoName {repoName} -IssueNumber {issueNumber} " +
            "-Title \"{title}\" -Description \"{description}\" -BuildNumber {buildNumber} -IssueUrl {issueUrl}"
    }

    private fun resolveAutoFixScript(project: Project?): File? {
        val basePath = project?.basePath ?: return null
        var current: File? = File(basePath)
        repeat(6) {
            val candidate = File(current, DEFAULT_AUTO_FIX_SCRIPT_PATH)
            if (candidate.exists()) return candidate
            current = current?.parentFile
            if (current == null) return null
        }
        return null
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
        if (!values.containsKey("githubToken")) {
            values["githubToken"] = ""
        }

        val commandTemplate = autoFixCommand ?: resolveDefaultAutoFixCommand(projectRef)
        val commandLine = expandTemplate(commandTemplate, values)
        val workingDir = autoFixWorkingDir?.takeIf { it.isNotBlank() } ?: resolveDefaultAutoFixWorkingDir(projectRef)
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
                val credentialError = validateAutoFixCredentials()
                if (credentialError != null) {
                    showAutoFixMessage("Auto Fix", credentialError, true)
                    return@executeOnPooledThread
                }
                val command = buildShellCommand(commandLine)
                val processBuilder = ProcessBuilder(command)
                if (workingDir.isNotBlank()) {
                    processBuilder.directory(File(workingDir))
                }
                configureAutoFixEnvironment(processBuilder.environment())
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

    private fun configureAutoFixEnvironment(env: MutableMap<String, String>) {
        val providerId = normalizeProvider(autoFixProvider)
        env["QAVT_AI_PROVIDER"] = providerId
        autoFixAiCommand?.takeIf { it.isNotBlank() }?.let { env["QAVT_AI_COMMAND"] = it }

        val apiKey = loadApiKey(providerId)
        if (apiKey.isNullOrBlank()) return
        when (providerId) {
            "openai", "codex" -> env["OPENAI_API_KEY"] = apiKey
            "claude" -> env["ANTHROPIC_API_KEY"] = apiKey
            "gemini" -> {
                env["GEMINI_API_KEY"] = apiKey
                env["GOOGLE_API_KEY"] = apiKey
            }
        }
    }

    private fun validateAutoFixCredentials(): String? {
        if (!autoFixAiCommand.isNullOrBlank()) {
            return null
        }
        val providerId = normalizeProvider(autoFixProvider)
        if (providerId == "custom") return null
        val apiKey = loadApiKey(providerId)
        if (!apiKey.isNullOrBlank()) return null
        val label = providerLabelForId(providerId)
        return "Missing API key for $label. Open Auto Fix Settings and add a key."
    }

    private fun credentialAttributes(providerId: String): CredentialAttributes {
        val key = providerId.ifBlank { DEFAULT_AUTO_FIX_PROVIDER }
        return CredentialAttributes("QAVT Auto Fix", key)
    }

    private fun loadApiKey(providerId: String): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes(providerId))
    }

    private fun saveApiKey(providerId: String, apiKey: String?) {
        val attributes = credentialAttributes(providerId)
        if (apiKey.isNullOrBlank()) {
            PasswordSafe.instance.set(attributes, null)
        } else {
            PasswordSafe.instance.set(attributes, Credentials("api-key", apiKey))
        }
    }

    private fun normalizeProvider(value: String?): String {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return if (PROVIDER_LABELS.containsValue(normalized)) normalized else DEFAULT_AUTO_FIX_PROVIDER
    }

    private fun providerLabelForId(providerId: String?): String {
        val normalized = normalizeProvider(providerId)
        return PROVIDER_LABELS.entries.firstOrNull { it.value == normalized }?.key
            ?: PROVIDER_LABELS.keys.first()
    }

    private fun providerIdForLabel(label: String?): String {
        return label?.let { PROVIDER_LABELS[it] } ?: DEFAULT_AUTO_FIX_PROVIDER
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
        const val AUTO_FIX_PROVIDER_KEY = "qavt.autoFix.provider"
        const val AUTO_FIX_AI_COMMAND_KEY = "qavt.autoFix.aiCommand"
        const val DEFAULT_AUTO_FIX_SCRIPT_PATH = "scripts/auto_fix_and_publish.ps1"
        const val DEFAULT_AUTO_FIX_PROVIDER = "openai"
        val PROVIDER_LABELS = linkedMapOf(
            "OpenAI (Codex)" to "openai",
            "Claude" to "claude",
            "Gemini" to "gemini",
            "Custom" to "custom"
        )
    }
}
