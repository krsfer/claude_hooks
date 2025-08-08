package com.claudehooks.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudehooks.dashboard.domain.model.*
import com.claudehooks.dashboard.domain.repository.HookRepository
import com.claudehooks.dashboard.domain.usecase.ExportHooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isConnectedToRedis: Boolean = false,
    val connectionError: String? = null,
    val hooks: List<HookData> = emptyList(),
    val filteredHooks: List<HookData> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
    val stats: DashboardStats = DashboardStats.empty(),
    val filters: FilterCriteria = FilterCriteria(),
    val selectedSession: String? = null,
    val searchQuery: String = "",
    val selectedHook: HookData? = null,
    val availableToolNames: List<String> = emptyList(),
    val availableSessionIds: List<String> = emptyList(),
    val exportInProgress: Boolean = false,
    val exportError: String? = null,
    val showFilters: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: HookRepository,
    private val exportHooksUseCase: ExportHooksUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        connectToRedis()
        loadInitialData()
        subscribeToRealtimeHooks()
    }
    
    private fun connectToRedis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, connectionError = null) }
            
            try {
                val connected = repository.connectToRedis()
                _uiState.update { 
                    it.copy(
                        isConnectedToRedis = connected,
                        connectionError = if (!connected) "Failed to connect to Redis" else null,
                        isLoading = false
                    )
                }
                
                if (connected) {
                    Timber.d("Successfully connected to Redis")
                } else {
                    Timber.w("Failed to connect to Redis")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to Redis")
                _uiState.update { 
                    it.copy(
                        isConnectedToRedis = false,
                        connectionError = e.message ?: "Unknown connection error",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private fun subscribeToRealtimeHooks() {
        viewModelScope.launch {
            repository.subscribeToRealtimeHooks()
                .catch { e ->
                    Timber.e(e, "Error in real-time hooks subscription")
                    _uiState.update { 
                        it.copy(connectionError = "Real-time subscription failed: ${e.message}")
                    }
                }
                .collect { hookData ->
                    Timber.d("Received real-time hook: ${hookData.hook_type} - ${hookData.id}")
                    loadHooksData()
                    loadStats()
                }
        }
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            loadHooksData()
            loadSessions()
            loadStats()
            loadAvailableFilters()
        }
    }
    
    private fun loadHooksData() {
        viewModelScope.launch {
            try {
                val hooks = repository.getHooks(limit = 1000) // Load recent hooks
                _uiState.update { 
                    it.copy(hooks = hooks)
                }
                applyFilters()
            } catch (e: Exception) {
                Timber.e(e, "Error loading hooks data")
            }
        }
    }
    
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val sessions = repository.getSessionSummaries()
                _uiState.update { 
                    it.copy(sessions = sessions)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading sessions")
            }
        }
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getDashboardStats()
                _uiState.update { 
                    it.copy(stats = stats)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading stats")
            }
        }
    }
    
    private fun loadAvailableFilters() {
        viewModelScope.launch {
            try {
                val toolNames = repository.getAllToolNames()
                val sessionIds = repository.getAllSessionIds()
                _uiState.update { 
                    it.copy(
                        availableToolNames = toolNames,
                        availableSessionIds = sessionIds
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading available filters")
            }
        }
    }
    
    fun updateFilters(newFilters: FilterCriteria) {
        _uiState.update { 
            it.copy(filters = newFilters)
        }
        applyFilters()
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filters = it.filters.copy(searchQuery = query.takeIf { it.isNotBlank() })
            )
        }
        applyFilters()
    }
    
    private fun applyFilters() {
        viewModelScope.launch {
            try {
                val filteredHooks = if (_uiState.value.filters.isEmpty()) {
                    _uiState.value.hooks
                } else {
                    repository.getFilteredHooks(_uiState.value.filters)
                }
                
                _uiState.update { 
                    it.copy(filteredHooks = filteredHooks)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error applying filters")
            }
        }
    }
    
    fun selectSession(sessionId: String?) {
        _uiState.update { 
            it.copy(
                selectedSession = sessionId,
                filters = if (sessionId != null) {
                    it.filters.copy(sessionId = sessionId)
                } else {
                    it.filters.copy(sessionId = null)
                }
            )
        }
        applyFilters()
    }
    
    fun selectHook(hook: HookData?) {
        _uiState.update { 
            it.copy(selectedHook = hook)
        }
    }
    
    fun toggleFilters() {
        _uiState.update { 
            it.copy(showFilters = !it.showFilters)
        }
    }
    
    fun clearFilters() {
        _uiState.update { 
            it.copy(
                filters = FilterCriteria(),
                searchQuery = "",
                selectedSession = null
            )
        }
        applyFilters()
    }
    
    fun refreshData() {
        loadInitialData()
    }
    
    fun retryConnection() {
        connectToRedis()
    }
    
    fun exportHooks(
        format: ExportHooksUseCase.ExportFormat,
        fileName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(exportInProgress = true, exportError = null)
            }
            
            try {
                // This would need context - typically called from UI with context
                // For now, we'll just indicate export is in progress
                _uiState.update { 
                    it.copy(
                        exportInProgress = false,
                        exportError = "Export functionality requires UI context"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _uiState.update { 
                    it.copy(
                        exportInProgress = false,
                        exportError = e.message ?: "Export failed"
                    )
                }
            }
        }
    }
    
    fun clearExportError() {
        _uiState.update { 
            it.copy(exportError = null)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnectFromRedis()
        }
    }
}