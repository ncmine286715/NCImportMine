package com.ncmine.importmine.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.usecase.ImportAddonUseCase
import com.ncmine.importmine.domain.usecase.ScanAddonsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado da UI para a tela principal
 */
data class HomeUiState(
    val packs: List<MinecraftPack> = emptyList(),
    val history: List<MinecraftPack> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null,
    val showImportSuccess: Boolean = false
)

/**
 * ViewModel principal para gerenciar o estado da aplicação
 */
class MainViewModel(
    private val scanAddonsUseCase: ScanAddonsUseCase,
    private val importAddonUseCase: ImportAddonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        scanAddons()
    }

    fun scanAddons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            scanAddonsUseCase()
                .catch { e -> _uiState.update { it.copy(isScanning = false, error = e.message) } }
                .collect { packs ->
                    _uiState.update { it.copy(isScanning = false, packs = packs) }
                }
        }
    }

    fun importPack(pack: MinecraftPack) {
        viewModelScope.launch {
            val result = importAddonUseCase(pack)
            if (result.isSuccess) {
                _uiState.update { it.copy(showImportSuccess = true) }
            } else {
                _uiState.update { it.copy(error = "Erro ao importar: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun dismissImportSuccess() {
        _uiState.update { it.copy(showImportSuccess = false) }
    }
}
