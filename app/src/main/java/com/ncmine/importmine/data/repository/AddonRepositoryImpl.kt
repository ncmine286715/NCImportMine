package com.ncmine.importmine.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ncmine.importmine.domain.model.MinecraftPack
import com.ncmine.importmine.domain.model.PackStatus
import com.ncmine.importmine.domain.model.PackType
import com.ncmine.importmine.domain.repository.AddonRepository
import com.ncmine.importmine.util.FileScanner
import com.ncmine.importmine.util.PackConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementação avançada do repositório de Addons com suporte a conversão e scanner inteligente
 */
class AddonRepositoryImpl(
    private val context: Context
) : AddonRepository {

    private val _detectedAddons = MutableStateFlow<List<MinecraftPack>>(emptyList())
    private val _importHistory = MutableStateFlow<List<MinecraftPack>>(emptyList())

    override fun scanForAddons(): Flow<List<MinecraftPack>> = flow {
        val files = FileScanner.scanAllDirectories(context)
        val packs = files.map { file ->
            val manifest = FileScanner.extractManifestInfo(file)
            val zipAnalysis = if (file.extension.lowercase() == "zip") {
                FileScanner.analyzeZipStructure(file)
            } else null

            MinecraftPack(
                originalFile = file,
                name = manifest?.name ?: file.nameWithoutExtension,
                description = manifest?.description ?: "",
                version = manifest?.version ?: "1.0.0",
                author = manifest?.author ?: "",
                packType = manifest?.packType ?: zipAnalysis?.second ?: PackType.UNKNOWN,
                status = if (zipAnalysis?.first == true) PackStatus.PENDING else PackStatus.VALID,
                manifestCount = zipAnalysis?.third ?: (if (manifest != null) 1 else 0)
            )
        }
        _detectedAddons.value = packs
        emit(packs)
    }.flowOn(Dispatchers.IO)

    override fun getDetectedAddons(): Flow<List<MinecraftPack>> = _detectedAddons

    override fun getImportHistory(): Flow<List<MinecraftPack>> = _importHistory

    override suspend fun importAddon(pack: MinecraftPack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var fileToImport = pack.processedFile ?: pack.originalFile
            
            // Se for ZIP, tenta converter antes de importar
            if (fileToImport.extension.lowercase() == "zip") {
                val converted = PackConverter.convertZipToMinecraft(context, fileToImport, pack.manifestCount)
                if (converted != null) {
                    fileToImport = converted
                }
            }

            val success = PackConverter.importIntoMinecraft(context, fileToImport)
            
            if (success) {
                val updatedHistory = _importHistory.value.toMutableList()
                updatedHistory.add(0, pack.copy(status = PackStatus.IMPORTED, processedFile = fileToImport))
                _importHistory.value = updatedHistory
                Result.success(Unit)
            } else {
                Result.failure(Exception("Falha ao abrir o Minecraft"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fixAddonStructure(pack: MinecraftPack): Result<MinecraftPack> = withContext(Dispatchers.IO) {
        try {
            val convertedFile = PackConverter.convertZipToMinecraft(context, pack.originalFile, pack.manifestCount)
            if (convertedFile != null) {
                val updatedPack = pack.copy(
                    processedFile = convertedFile,
                    status = PackStatus.CONVERTED
                )
                Result.success(updatedPack)
            } else {
                Result.failure(Exception("Não foi possível converter o arquivo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAddon(pack: MinecraftPack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (pack.originalFile.exists()) pack.originalFile.delete()
            pack.processedFile?.let { if (it.exists()) it.delete() }
            
            val updatedList = _detectedAddons.value.filter { it.id != pack.id }
            _detectedAddons.value = updatedList
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
