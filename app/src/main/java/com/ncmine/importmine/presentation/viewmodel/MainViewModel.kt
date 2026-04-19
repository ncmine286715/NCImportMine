package com.ncmine.importmine.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.model.PackStatus
import com.ncmine.importmine.domain.repository.AddonRepository
import com.ncmine.importmine.domain.usecase.ImportAddonUseCase
import com.ncmine.importmine.util.AdMobManager
import android.app.Activity
import com.ncmine.importmine.domain.usecase.ScanAddonsUseCase
import com.ncmine.importmine.util.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val lastImportedPack: MinecraftPack? = null,
    val isPremium: Boolean = false
)

/**
 * ViewModel que gerencia o estado da UI e coordena as operações
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    @ApplicationContext private val context: Context,
    private val scanAddonsUseCase: ScanAddonsUseCase,
    private val importAddonUseCase: ImportAddonUseCase,
    private val addonRepository: AddonRepository,
    private val preferenceManager: PreferenceManager,
    private val adMobManager: AdMobManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkMinecraftInstallation()
        loadHistoryAndFavorites()

        _uiState.update { it.copy(isPremium = adMobManager.isPremium) }
        if (!adMobManager.isPremium) {
            adMobManager.loadRewardedAd(context)
            adMobManager.loadInterstitialAd(context)
        }
        scanAddons()
    }

    private fun loadHistoryAndFavorites() {
        viewModelScope.launch {
            addonRepository.getImportHistory().collect { history ->
                _uiState.update { state ->
                    val updatedPacks = state.packs.map { pack ->
                        pack.copy(
                            isFavorite = preferenceManager.isFavorite(pack.id),
                            isImported = history.any { it.id == pack.id }
                        )
                    }
                    applyFilters(state.copy(
                        packs = updatedPacks,
                        favoritePacks = updatedPacks.filter { it.isFavorite },
                        importedPacks = updatedPacks.filter { it.isImported }
                    ))
                }
            }
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

    private fun performImport(pack: MinecraftPack) {
        viewModelScope.launch {
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

            delay(500) // Simula um pequeno atraso para feedback visual

            val result = importAddonUseCase(pack)
            if (result.isSuccess) {
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
                        successMessage = buildSuccessMessage(pack)
                    ))
                }
                adMobManager.loadInterstitialAd(context)
            } else {
                _uiState.update { state ->
                    val updatedPacks = state.packs.map { 
                        if (it.id == pack.id) it.copy(isImporting = false, status = PackStatus.ERROR) else it 
                    }
                    state.copy(
                        packs = updatedPacks,
                        importingPackId = null,
                        errorMessage = result.exceptionOrNull()?.message ?: "Erro desconhecido ao importar."
                    )
                }
                adMobManager.loadInterstitialAd(context)
            }
        }
    }

    fun toggleFavorite(pack: MinecraftPack) {
        preferenceManager.toggleFavorite(pack.id)
        loadHistoryAndFavorites()
    }

    private fun checkMinecraftInstallation() {
        val installed = addonRepository.isMinecraftInstalled()
        _uiState.update { it.copy(isMinecraftInstalled = installed) }
    }

    fun scanAddons() {
        viewModelScope.launch {
            scanAddonsUseCase()
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
                .collect { packs ->
                    _uiState.update { state ->
                        val updatedPacks = packs.map { pack ->
                            pack.copy(
                                isFavorite = preferenceManager.isFavorite(pack.id),
                                isImported = preferenceManager.isImported(pack.id)
                            )
                        }
                        applyFilters(state.copy(
                            isScanning = false,
                            scanCompleted = true,
                            packs = updatedPacks
                        ))
                    }
                }
        }
    }

    fun startFastScan() {
        scanAddons()
    }

    fun importPack(activity: Activity, pack: MinecraftPack) {
        if (!adMobManager.isPremium) {
            adMobManager.showInterstitial(activity) {
                performImport(pack)
            }
        } else {
            performImport(pack)
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
        preferenceManager.clearHistory()
        loadHistoryAndFavorites()
    }

    fun clearFavorites() {
        preferenceManager.clearFavorites()
        loadHistoryAndFavorites()
    }

    fun togglePremium() {
        adMobManager.isPremium = !adMobManager.isPremium
        _uiState.update { it.copy(isPremium = adMobManager.isPremium) }
    }

    private fun buildSuccessMessage(pack: MinecraftPack): String {
        return buildString {
            append("✅ ${pack.name} enviado para o Minecraft!")
            // A informação de conversão e backup agora está encapsulada no repositório
            // e não é diretamente exposta no resultado do use case.
            // Poderíamos adicionar um campo no MinecraftPack para isso se necessário.
        }
    }
}
