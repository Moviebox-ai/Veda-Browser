package com.example.domain

import com.example.domain.model.Tab
import com.example.webview.TabWebViewManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

/**
 * Orchestrates multi-tab operations, active tab selection, and recently closed tabs.
 */
class TabManager(
    private val webViewManager: TabWebViewManager
) {

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val recentlyClosed = Stack<Tab>()

    init {
        // Create initial default tab
        createTab(url = "velocity:home", isPrivate = false, select = true)
    }

    val activeTab: Tab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    fun createTab(url: String = "velocity:home", isPrivate: Boolean = false, select: Boolean = true): Tab {
        val newTab = Tab(
            url = url,
            title = if (url == "velocity:home") "New Tab" else url,
            isPrivate = isPrivate
        )
        val updated = _tabs.value + newTab
        _tabs.value = updated
        if (select || _activeTabId.value == null) {
            _activeTabId.value = newTab.id
        }
        return newTab
    }

    fun selectTab(tabId: String) {
        val found = _tabs.value.find { it.id == tabId }
        if (found != null) {
            _activeTabId.value = tabId
            updateTab(tabId) { it.copy(lastActiveAt = System.currentTimeMillis()) }
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val closingTab = currentTabs.find { it.id == tabId } ?: return

        if (!closingTab.isPrivate) {
            recentlyClosed.push(closingTab)
            if (recentlyClosed.size > 10) recentlyClosed.removeAt(0)
        }

        webViewManager.destroyTab(tabId)

        val updatedTabs = currentTabs.filter { it.id != tabId }
        if (updatedTabs.isEmpty()) {
            // Reopen a fresh tab
            _tabs.value = emptyList()
            createTab(url = "velocity:home", isPrivate = closingTab.isPrivate, select = true)
        } else {
            _tabs.value = updatedTabs
            if (_activeTabId.value == tabId) {
                // Select adjacent tab
                val oldIndex = currentTabs.indexOfFirst { it.id == tabId }
                val newIndex = (oldIndex - 1).coerceAtLeast(0).coerceAtMost(updatedTabs.size - 1)
                _activeTabId.value = updatedTabs[newIndex].id
            }
        }
    }

    fun closeOtherTabs(keepTabId: String) {
        val currentTabs = _tabs.value
        val toRemove = currentTabs.filter { it.id != keepTabId }
        for (tab in toRemove) {
            webViewManager.destroyTab(tab.id)
            if (!tab.isPrivate) recentlyClosed.push(tab)
        }
        _tabs.value = currentTabs.filter { it.id == keepTabId }
        _activeTabId.value = keepTabId
    }

    fun closeAllTabs(isPrivateOnly: Boolean = false) {
        val currentTabs = _tabs.value
        val toRemove = if (isPrivateOnly) currentTabs.filter { it.isPrivate } else currentTabs
        for (tab in toRemove) {
            webViewManager.destroyTab(tab.id)
            if (!tab.isPrivate) recentlyClosed.push(tab)
        }

        val remaining = if (isPrivateOnly) currentTabs.filter { !it.isPrivate } else emptyList()
        if (remaining.isEmpty()) {
            _tabs.value = emptyList()
            createTab(url = "velocity:home", isPrivate = false, select = true)
        } else {
            _tabs.value = remaining
            if (_activeTabId.value !in remaining.map { it.id }) {
                _activeTabId.value = remaining.last().id
            }
        }
    }

    fun reopenLastClosedTab(): Tab? {
        if (recentlyClosed.isEmpty()) return null
        val restored = recentlyClosed.pop()
        val newTab = restored.copy(
            id = java.util.UUID.randomUUID().toString(),
            savedState = null
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        return newTab
    }

    fun updateTab(tabId: String, transform: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) transform(tab) else tab
        }
    }
}
