package com.ncmine.importmine.domain.usecase

import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.repository.AddonRepository
import javax.inject.Inject

/**
 * Caso de uso para importar um addon para o Minecraft
 */
class ImportAddonUseCase @Inject constructor(private val repository: AddonRepository) {
    suspend operator fun invoke(pack: MinecraftPack): Result<Unit> {
        return repository.importAddon(pack)
    }
}
