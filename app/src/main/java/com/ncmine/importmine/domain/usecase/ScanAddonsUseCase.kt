package com.ncmine.importmine.domain.usecase

import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.repository.AddonRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para escanear addons no armazenamento
 */
class ScanAddonsUseCase(private val repository: AddonRepository) {
    operator fun invoke(): Flow<List<MinecraftPack>> {
        return repository.scanForAddons()
    }
}
