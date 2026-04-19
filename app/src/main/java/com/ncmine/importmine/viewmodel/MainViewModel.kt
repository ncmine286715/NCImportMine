package com.ncmine.importmine.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.model.PackStatus
import com.ncmine.importmine.repository.FileRepository
import com.ncmine.importmine.repository.ProcessResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MainViewModel"

/**
 * Ordem de ordenação dos pacotes
 */
enum class SortOrder {
    A_Z,
    NEWEST,
    SIZE
}

/**
 * Estado da UI principal
 */
data class MainUiState(
    val isScanning: Boolean = false,
    val packs: List<MinecraftPack> = emptyList(),
    val filteredPacks: List<MinecraftPack> = emptyList(),
    val favoritePacks: List<MinecraftPack> = emptyList(),
    val importedPacks: List<MinecraftPack> = emptyList(),
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val selectedPack: MinecraftPack? = null,
    val importingPackId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showPermissionDialog: Boolean = false,
    val showImportSuccess: Boolean = false,
    val scanCompleted: Boolean = false,
    val isMinecraftInstalled: Boolean = false,
    val lastImportedPack: MinecraftPack? = null
)

/**
 * ViewModel que gerencia o estado da UI e coordena as operações
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application.applicationContext)
    private val prefs = com.ncmine.importmine.util.PreferenceManager(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkMinecraftInstallation()
    }

    private fun loadHistoryAndFavorites() {
        val favorites = prefs.getFavorites()
        val history = prefs.getHistory()
        
        _uiState.update { state ->
            val updatedPacks = state.packs.map { pack ->
                pack.copy(
                    isFavorite = favorites.contains(pack.id),
                    isImported = history.contains(pack.id)
                )
            }
            applyFilters(state.copy(
                packs = updatedPacks,
                favoritePacks = updatedPacks.filter { it.isFavorite },
                importedPacks = updatedPacks.filter { it.isImported }
            ))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { applyFilters(it.copy(searchQuery = query)) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { applyFilters(it.copy(sortOrder = order)) }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.update { applyFilters(it.copy(selectedTab = tab)) }
    }

    private fun applyFilters(state: MainUiState): MainUiState {
        val baseList = when (state.selectedTab) {
            1 -> state.packs.filter { it.isFavorite }
            2 -> state.packs.filter { it.isImported }
            else -> state.packs
        }

        var filtered = baseList

        // Pesquisa
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.author.contains(state.searchQuery, ignoreCase = true) ||
                it.packTypeLabel.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Ordenação
        filtered = when (state.sortOrder) {
            SortOrder.A_Z -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.NEWEST -> filtered.sortedByDescending { it.lastModified }
            SortOrder.SIZE -> filtered.sortedByDescending { it.fileSizeBytes }
        }

        return state.copy(
            filteredPacks = filtered,
            favoritePacks = state.packs.filter { it.isFavorite },
            importedPacks = state.packs.filter { it.isImported }
        )
    }

    fun toggleFavorite(pack: MinecraftPack) {
        prefs.toggleFavorite(pack.id)
        loadHistoryAndFavorites()
    }

    private fun checkMinecraftInstallation() {
        val installed = repository.isMinecraftInstalled()
        _uiState.update { it.copy(isMinecraftInstalled = installed) }
    }

    /**
     * Inicia a varredura rápida (apenas .mcpack, .mcworld)
     * Os arquivos aparecem na hora que são detectados
     */
    fun startFastScan() {
        viewModelScope.launch {
            repository.fastScan()
                .onStart {
                    _uiState.update { 
                        it.copy(
                            isScanning = true, 
                            errorMessage = null, 
                            successMessage = null,
                            scanCompleted = false,
                            packs = emptyList() 
                        ) 
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(isScanning = false, errorMessage = e.message) }
                }
                .collect { newPack ->
                    _uiState.update { state ->
                        if (state.packs.none { it.originalFile.absolutePath == newPack.originalFile.absolutePath }) {
                            val favorites = prefs.getFavorites()
                            val history = prefs.getHistory()
                            val initializedPack = newPack.copy(
                                isFavorite = favorites.contains(newPack.id),
                                isImported = history.contains(newPack.id)
                            )
                            val updatedPacks = state.packs + initializedPack
                            applyFilters(state.copy(
                                packs = updatedPacks,
                                favoritePacks = if (initializedPack.isFavorite) state.favoritePacks + initializedPack else state.favoritePacks,
                                importedPacks = if (initializedPack.isImported) state.importedPacks + initializedPack else state.importedPacks
                            ))
                        } else state
                    }
                }
            _uiState.update { it.copy(isScanning = false, scanCompleted = true) }
        }
    }

    /**
     * Atalho para iniciar uma varredura rápida
     */
    fun refreshPacks() {
        startFastScan()
    }

    /**
     * Inicia a Ultra Varredura (inclui ZIPs e análise de manifest/ícones)
     * Atualiza a lista conforme processa cada arquivo
     */
    fun startUltraScan() {
        viewModelScope.launch {
            repository.ultraScan()
                .onStart {
                    _uiState.update { it.copy(isScanning = true, errorMessage = null) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isScanning = false, errorMessage = e.message) }
                }
                .collect { newPack ->
                    _uiState.update { state ->
                        val existingIndex = state.packs.indexOfFirst { 
                            it.originalFile.absolutePath == newPack.originalFile.absolutePath 
                        }
                        
                        val updatedPacks = if (existingIndex >= 0) {
                            val list = state.packs.toMutableList()
                            list[existingIndex] = newPack
                            list
                        } else {
                            state.packs + newPack
                        }
                        applyFilters(state.copy(packs = updatedPacks))
                    }
                }
            _uiState.update { it.copy(isScanning = false, scanCompleted = true) }
        }
    }

    /**
     * Importa um pacote no Minecraft
     */
    fun importPack(pack: MinecraftPack) {
        viewModelScope.launch {
            // Atualiza o estado para "Importando"
            _uiState.update { state ->
                val updatedPacks = state.packs.map { 
                    if (it.id == pack.id) it.copy(isImporting = true, status = PackStatus.IMPORTING) else it 
                }
                state.copy(
                    packs = updatedPacks,
                    importingPackId = pack.id,
                    errorMessage = null
                )
            }

            delay(500)

            when (val result = repository.processPackForImport(pack)) {
                is ProcessResult.Success -> {
                    val opened = repository.importIntoMinecraft(result.fileToImport)
                    if (opened) {
                        prefs.addToHistory(pack.id)
                        _uiState.update { state ->
                            val updatedPacks = state.packs.map { 
                                if (it.id == pack.id) it.copy(isImported = true, isImporting = false, status = PackStatus.IMPORTED) else it 
                            }
                            applyFilters(state.copy(
                                packs = updatedPacks,
                                importedPacks = updatedPacks.filter { it.isImported },
                                favoritePacks = updatedPacks.filter { it.isFavorite },
                                importingPackId = null,
                                showImportSuccess = true,
                                lastImportedPack = pack,
                                successMessage = buildSuccessMessage(pack, result)
                            ))
                        }
                    } else {
                        _uiState.update { state ->
                            val updatedPacks = state.packs.map { 
                                if (it.id == pack.id) it.copy(isImporting = false, status = PackStatus.ERROR) else it 
                            }
                            state.copy(
                                packs = updatedPacks,
                                importingPackId = null,
                                errorMessage = "Não foi possível abrir o arquivo. O Minecraft está instalado?"
                            )
                        }
                    }
                }
                is ProcessResult.Error -> {
                    _uiState.update { state ->
                        val updatedPacks = state.packs.map { 
                            if (it.id == pack.id) it.copy(isImporting = false, status = PackStatus.ERROR) else it 
                        }
                        state.copy(
                            packs = updatedPacks,
                            importingPackId = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun selectPack(pack: MinecraftPack?) {
        _uiState.update { it.copy(selectedPack = pack) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissImportSuccess() {
        _uiState.update { it.copy(showImportSuccess = false, lastImportedPack = null) }
    }

    fun showPermissionDialog(show: Boolean) {
        _uiState.update { it.copy(showPermissionDialog = show) }
    }

    fun resetScan() {
        _uiState.update {
            applyFilters(it.copy(
                packs = emptyList(),
                favoritePacks = emptyList(),
                importedPacks = emptyList(),
                scanCompleted = false,
                selectedPack = null,
                errorMessage = null,
                successMessage = null
            ))
        }
    }

    fun clearHistory() {
        prefs.clearHistory()
        loadHistoryAndFavorites()
    }

    fun clearFavorites() {
        prefs.clearFavorites()
        loadHistoryAndFavorites()
    }

    private fun updatePackStatus(packId: String, status: PackStatus) {
        _uiState.update { state ->
            state.copy(
                packs = state.packs.map { pack ->
                    if (pack.id == packId) pack.copy(status = status) else pack
                }
            )
        }
    }

    private fun buildSuccessMessage(pack: MinecraftPack, result: ProcessResult.Success): String {
        return buildString {
            append("✅ ${pack.name} enviado para o Minecraft!")
            if (result.wasConverted) {
                val type = if (pack.manifestCount > 1) "MCADDON" else "MCPACK"
                append("\n🔄 ZIP convertido para $type")
            }
            if (result.backupPath != null) append("\n💾 Backup salvo com sucesso")
        }
    }
}
