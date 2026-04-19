package com.ncmine.importmine.domain.repository

import com.ncmine.importmine.domain.model.MinecraftPack
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface que define as operações de dados para Addons
 */
interface AddonRepository {
    
    /**
     * Escaneia o armazenamento em busca de novos addons
     */
    fun scanForAddons(): Flow<List<MinecraftPack>>
    
    /**
     * Obtém a lista de addons detectados
     */
    fun getDetectedAddons(): Flow<List<MinecraftPack>>
    
    /**
     * Obtém o histórico de addons importados
     */
    fun getImportHistory(): Flow<List<MinecraftPack>>
    
    /**
     * Importa um addon para o Minecraft
     */
    suspend fun importAddon(pack: MinecraftPack): Result<Unit>
    
    /**
     * Corrige a estrutura de um addon (ex: ZIP para MCPACK)
     */
    suspend fun fixAddonStructure(pack: MinecraftPack): Result<MinecraftPack>
    
    /**
     * Remove um addon da lista de detectados
     */
    suspend fun deleteAddon(pack: MinecraftPack): Result<Unit>

    /**
     * Verifica se o Minecraft está instalado no dispositivo
     */
    fun isMinecraftInstalled(): Boolean
}
