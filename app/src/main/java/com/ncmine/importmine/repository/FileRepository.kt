package com.ncmine.importmine.repository

import android.content.Context
import android.util.Log
import com.ncmine.importmine.model.MinecraftPack
import com.ncmine.importmine.model.PackStatus
import com.ncmine.importmine.model.PackType
import com.ncmine.importmine.model.ScanResult
import com.ncmine.importmine.util.BackupManager
import com.ncmine.importmine.util.FileScanner
import com.ncmine.importmine.util.PackConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FileRepository"

/**
 * Repository que orquestra todas as operações de arquivo.
 * Segue o padrão MVVM: ViewModel → Repository → Data Sources (Utils)
 */
class FileRepository(private val context: Context) {

    private val prefs = com.ncmine.importmine.util.PreferenceManager(context)

    // Diretório temporário para ícones extraídos
    private val iconCacheDir: File by lazy {
        File(context.cacheDir, "pack_icons").also { it.mkdirs() }
    }

    /**
     * Realiza a varredura normal (apenas .mcpack, .mcworld e .mcaddon)
     * Emite os pacotes um a por um para a UI atualizar em tempo real
     */
    fun fastScan(): Flow<MinecraftPack> = flow {
        val rawFiles = FileScanner.scanAllDirectories(context)
        val filteredFiles = rawFiles.filter { it.extension.lowercase() in setOf("mcpack", "mcworld", "mcaddon") }
        
        filteredFiles.forEach { file ->
            analyzeFile(file)?.let { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Realiza a Ultra Varredura (inclui .zip e análise profunda)
     * Também emite um por um
     */
    fun ultraScan(): Flow<MinecraftPack> = flow {
        val rawFiles = FileScanner.scanAllDirectories(context)
        
        rawFiles.forEach { file ->
            analyzeFile(file)?.let { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Analisa um arquivo individual e cria o objeto MinecraftPack correspondente
     */
    fun analyzeFile(file: File): MinecraftPack? {
        return try {
            val ext = file.extension.lowercase()
            val persistentId = prefs.getPersistentId(file.absolutePath)

            when (ext) {
                "mcpack", "mcworld", "mcaddon" -> {
                    // Extrai informações do manifest diretamente
                    val manifest = FileScanner.extractManifestInfo(file)
                    val iconFile = FileScanner.extractIcon(file, iconCacheDir)

                    MinecraftPack(
                        id = persistentId,
                        originalFile = file,
                        name = manifest?.name ?: file.nameWithoutExtension,
                        description = manifest?.description ?: "",
                        version = manifest?.version ?: "1.0.0",
                        author = manifest?.author ?: "",
                        uuid = manifest?.uuid ?: "",
                        minEngineVersion = manifest?.minEngineVersion ?: "",
                        packType = manifest?.packType ?: when(ext) {
                            "mcworld" -> PackType.WORLD_TEMPLATE
                            "mcaddon" -> PackType.ADDON
                            else -> PackType.UNKNOWN
                        },
                        status = PackStatus.VALID,
                        iconFile = iconFile,
                        fileSizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        isFavorite = prefs.isFavorite(persistentId),
                        isImported = prefs.isImported(persistentId)
                    )
                }

                "zip" -> {
                    // Verifica se o ZIP tem estrutura de Minecraft
                    val (isValid, detectedType, manifestCount) = FileScanner.analyzeZipStructure(file)

                    if (!isValid) {
                        Log.d(TAG, "ZIP ignorado (sem estrutura Minecraft): ${file.name}")
                        return null
                    }

                    val manifest = FileScanner.extractManifestInfo(file)
                    val iconFile = FileScanner.extractIcon(file, iconCacheDir)

                    MinecraftPack(
                        id = persistentId,
                        originalFile = file,
                        name = manifest?.name ?: file.nameWithoutExtension,
                        description = manifest?.description ?: "Arquivo ZIP com estrutura Minecraft",
                        version = manifest?.version ?: "1.0.0",
                        author = manifest?.author ?: "",
                        uuid = manifest?.uuid ?: "",
                        minEngineVersion = manifest?.minEngineVersion ?: "",
                        packType = manifest?.packType ?: detectedType,
                        status = PackStatus.VALID,
                        iconFile = iconFile,
                        fileSizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        manifestCount = manifestCount,
                        isFavorite = prefs.isFavorite(persistentId),
                        isImported = prefs.isImported(persistentId)
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar ${file.name}: ${e.message}")
            null
        }
    }

    /**
     * Processa um pacote completo: backup + conversão (se ZIP) + retorna arquivo pronto
     */
    suspend fun processPackForImport(pack: MinecraftPack): ProcessResult = withContext(Dispatchers.IO) {
        try {
            val originalFile = pack.originalFile

            // 1. Backup obrigatório antes de qualquer coisa
            Log.d(TAG, "Fazendo backup de: ${originalFile.name}")
            val backupFile = BackupManager.backupFile(originalFile)
            if (backupFile == null) {
                Log.w(TAG, "Backup falhou, mas continuando mesmo assim...")
            }

            // 2. Se for ZIP, converte para MCPACK ou MCADDON
            val fileToImport = if (originalFile.extension.lowercase() == "zip") {
                Log.d(TAG, "Convertendo ZIP → Minecraft: ${originalFile.name}")
                PackConverter.convertZipToMinecraft(context, originalFile, pack.manifestCount)
                    ?: return@withContext ProcessResult.Error("Falha ao converter ZIP")
            } else {
                // Já é .mcpack ou .mcworld, usa direto
                originalFile
            }

            Log.d(TAG, "Arquivo pronto para importar: ${fileToImport.absolutePath}")

            ProcessResult.Success(
                fileToImport = fileToImport,
                backupPath = backupFile?.absolutePath,
                wasConverted = originalFile.extension.lowercase() == "zip"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar pacote ${pack.name}: ${e.message}")
            ProcessResult.Error("Erro ao processar: ${e.message}")
        }
    }

    /**
     * Dispara a importação no Minecraft
     */
    fun importIntoMinecraft(file: File): Boolean {
        return PackConverter.importIntoMinecraft(context, file)
    }

    /**
     * Verifica se o Minecraft está instalado
     */
    fun isMinecraftInstalled(): Boolean {
        return PackConverter.isMinecraftInstalled(context)
    }

    /**
     * Limpa o cache de ícones (economia de espaço)
     */
    suspend fun clearIconCache() = withContext(Dispatchers.IO) {
        iconCacheDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * Resultado do processamento de um pacote
 */
sealed class ProcessResult {
    data class Success(
        val fileToImport: File,
        val backupPath: String?,
        val wasConverted: Boolean
    ) : ProcessResult()

    data class Error(val message: String) : ProcessResult()
}
