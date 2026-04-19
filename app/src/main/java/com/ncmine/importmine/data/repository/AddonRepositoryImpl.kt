package com.ncmine.importmine.data.repository

import android.content.Context
import android.util.Log
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.model.PackStatus
import com.ncmine.importmine.domain.model.PackType
import com.ncmine.importmine.domain.repository.AddonRepository
import com.ncmine.importmine.util.BackupManager
import com.ncmine.importmine.util.FileScanner
import com.ncmine.importmine.util.PackConverter
import com.ncmine.importmine.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AddonRepositoryImpl"

/**
 * Implementação do repositório de Addons que interage com o sistema de arquivos.
 */
@Singleton
class AddonRepositoryImpl @Inject constructor(
    private val context: Context,
    private val fileScanner: FileScanner,
    private val packConverter: PackConverter,
    private val backupManager: BackupManager,
    private val preferenceManager: PreferenceManager
) : AddonRepository {

    private val _detectedAddons = MutableStateFlow<List<MinecraftPack>>(emptyList())
    private val _importHistory = MutableStateFlow<List<MinecraftPack>>(emptyList())

    // Diretório temporário para ícones extraídos
    private val iconCacheDir: File by lazy {
        File(context.cacheDir, "pack_icons").also { it.mkdirs() }
    }

    override fun scanForAddons(): Flow<List<MinecraftPack>> = flow {
        val scannedPacks = mutableListOf<MinecraftPack>()
        fileScanner.scanAllDirectoriesFlow(context).collect { file ->
            analyzeFile(file)?.let { pack ->
                scannedPacks.add(pack)
                emit(scannedPacks.toList())
            }
        }
        _detectedAddons.value = scannedPacks.toList()
    }.flowOn(Dispatchers.IO)

    override fun getDetectedAddons(): Flow<List<MinecraftPack>> = _detectedAddons

    override fun getImportHistory(): Flow<List<MinecraftPack>> = _importHistory

    override suspend fun importAddon(pack: MinecraftPack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val originalFile = pack.originalFile
            var fileToImport = originalFile

            // 1. Backup obrigatório antes de qualquer coisa
            Log.d(TAG, "Fazendo backup de: ${originalFile.name}")
            val backupFile = backupManager.backupFile(originalFile)
            if (backupFile == null) {
                Log.w(TAG, "Backup falhou para ${originalFile.name}, mas continuando mesmo assim...")
            }

            // 2. Se for ZIP, converte para MCPACK ou MCADDON
            if (originalFile.extension.lowercase() == "zip") {
                Log.d(TAG, "Convertendo ZIP -> Minecraft: ${originalFile.name}")
                val converted = packConverter.convertZipToMinecraft(context, originalFile, pack.manifestCount)
                if (converted != null) {
                    fileToImport = converted
                } else {
                    return@withContext Result.failure(Exception("Falha ao converter ZIP"))
                }
            }

            Log.d(TAG, "Arquivo pronto para importar: ${fileToImport.absolutePath}")

            val success = packConverter.importIntoMinecraft(context, fileToImport)

            if (success) {
                // Atualiza o histórico e o status do pack
                val updatedPack = pack.copy(
                    status = PackStatus.IMPORTED,
                    isImported = true,
                    processedFile = fileToImport,
                    backupPath = backupFile?.absolutePath
                )
                preferenceManager.addToHistory(updatedPack.id)
                _importHistory.update { currentList -> (currentList + updatedPack).distinctBy { it.id } }
                _detectedAddons.update { currentList ->
                    currentList.map { if (it.id == updatedPack.id) updatedPack else it }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Não foi possível abrir o arquivo. O Minecraft está instalado?"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar addon ${pack.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fixAddonStructure(pack: MinecraftPack): Result<MinecraftPack> = withContext(Dispatchers.IO) {
        try {
            if (pack.originalFile.extension.lowercase() != "zip") {
                return@withContext Result.failure(Exception("Apenas arquivos ZIP podem ser corrigidos/convertidos."))
            }

            val convertedFile = packConverter.convertZipToMinecraft(context, pack.originalFile, pack.manifestCount)
            if (convertedFile != null) {
                val updatedPack = pack.copy(
                    processedFile = convertedFile,
                    status = PackStatus.CONVERTED
                )
                _detectedAddons.update { currentList ->
                    currentList.map { if (it.id == updatedPack.id) updatedPack else it }
                }
                Result.success(updatedPack)
            } else {
                Result.failure(Exception("Não foi possível converter o arquivo ZIP para um formato Minecraft válido."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao corrigir estrutura do addon ${pack.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAddon(pack: MinecraftPack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (pack.originalFile.exists()) {
                pack.originalFile.delete()
            }
            pack.processedFile?.let { if (it.exists()) it.delete() }
            // Remover do cache de ícones também
            pack.iconFile?.let { if (it.exists()) it.delete() }

            _detectedAddons.update { currentList -> currentList.filter { it.id != pack.id } }
            preferenceManager.removeHistory(pack.id)
            preferenceManager.removeFavorite(pack.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar addon ${pack.name}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun isMinecraftInstalled(): Boolean {
        return packConverter.isMinecraftInstalled(context)
    }

    /**
     * Analisa um arquivo individual e cria o objeto MinecraftPack correspondente.
     */
    private fun analyzeFile(file: File): MinecraftPack? {
        return try {
            val ext = file.extension.lowercase()
            val persistentId = preferenceManager.getPersistentId(file.absolutePath)

            val manifest = fileScanner.extractManifestInfo(file)
            val iconFile = fileScanner.extractIcon(file, iconCacheDir)
            val (isValidZip, detectedType, manifestCount) = if (ext == "zip") {
                fileScanner.analyzeZipStructure(file)
            } else Triple(false, PackType.UNKNOWN, 0)

            // Se for um ZIP inválido, ignorar
            if (ext == "zip" && !isValidZip) {
                Log.d(TAG, "ZIP ignorado (sem estrutura Minecraft): ${file.name}")
                return null
            }

            MinecraftPack(
                id = persistentId,
                originalFile = file,
                name = manifest?.name ?: file.nameWithoutExtension,
                description = manifest?.description ?: "",
                version = manifest?.version ?: "1.0.0",
                author = manifest?.author ?: "",
                uuid = manifest?.uuid ?: "",
                minEngineVersion = manifest?.minEngineVersion ?: "",
                packType = manifest?.packType ?: detectedType,
                status = if (ext == "zip" && isValidZip) PackStatus.PENDING else PackStatus.VALID,
                iconFile = iconFile,
                fileSizeBytes = file.length(),
                lastModified = file.lastModified(),
                manifestCount = manifestCount,
                isFavorite = preferenceManager.isFavorite(persistentId),
                isImported = preferenceManager.isImported(persistentId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar ${file.name}: ${e.message}", e)
            null
        }
    }

    suspend fun clearIconCache() = withContext(Dispatchers.IO) {
        iconCacheDir.listFiles()?.forEach { it.delete() }
    }
}
