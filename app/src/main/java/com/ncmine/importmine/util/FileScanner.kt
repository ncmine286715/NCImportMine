package com.ncmine.importmine.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ncmine.importmine.model.PackType
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val TAG = "FileScanner"

/**
 * Responsável por escanear diretórios e detectar arquivos Minecraft válidos
 */
object FileScanner {

    // Extensões que o app processa
    private val SUPPORTED_EXTENSIONS = setOf("mcpack", "mcworld", "mcaddon", "zip")

    // Estruturas válidas de pacotes Minecraft
    private val RESOURCE_PACK_FOLDERS = setOf("textures", "sounds", "models", "ui", "animations")
    private val BEHAVIOR_PACK_FOLDERS = setOf("entities", "blocks", "items", "scripts", "loot_tables")
    private val WORLD_TEMPLATE_FOLDERS = setOf("world_template", "db", "level.dat")

    /**
     * Escaneia diretórios e emite arquivos encontrados um por um via Flow
     */
    fun scanAllDirectoriesFlow(context: Context): Flow<File> = flow {
        // 1. Pasta Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists() && downloadsDir.canRead()) {
            scanDirectoryFlow(downloadsDir).collect { emit(it) }
        }

        // 2. Pasta Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists() && documentsDir.canRead()) {
            scanDirectoryFlow(documentsDir).collect { emit(it) }
        }

        // 3. Pasta do Minecraft
        val minecraftDir = File(Environment.getExternalStorageDirectory(), "games/com.mojang")
        if (minecraftDir.exists() && minecraftDir.canRead()) {
            scanDirectoryFlow(minecraftDir, depth = 2).collect { emit(it) }
        }

        // 4. Raiz (apenas arquivos diretos)
        val externalDir = Environment.getExternalStorageDirectory()
        if (externalDir.exists() && externalDir.canRead()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.isFile && isSupportedFile(file)) {
                    emit(file)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun scanDirectoryFlow(dir: File, depth: Int = 5, currentDepth: Int = 0): Flow<File> = flow {
        if (currentDepth > depth || !dir.exists() || !dir.canRead()) return@flow

        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && isSupportedFile(file) -> {
                        emit(file)
                    }
                    file.isDirectory && !isSystemDirectory(file) -> {
                        scanDirectoryFlow(file, depth, currentDepth + 1).collect { emit(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao escanear ${dir.absolutePath}: ${e.message}")
        }
    }

    /**
     * Escaneia todos os diretórios configurados e retorna a lista de pacotes encontrados (versão síncrona/legado)
     */
    fun scanAllDirectories(context: Context): List<File> {
        val foundFiles = mutableListOf<File>()

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists() && downloadsDir.canRead()) {
            foundFiles.addAll(scanDirectory(downloadsDir))
        }

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists() && documentsDir.canRead()) {
            foundFiles.addAll(scanDirectory(documentsDir))
        }

        val minecraftDir = File(Environment.getExternalStorageDirectory(), "games/com.mojang")
        if (minecraftDir.exists() && minecraftDir.canRead()) {
            foundFiles.addAll(scanDirectory(minecraftDir, depth = 2))
        }

        val externalDir = Environment.getExternalStorageDirectory()
        if (externalDir.exists() && externalDir.canRead()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.isFile && isSupportedFile(file)) {
                    if (!foundFiles.contains(file)) foundFiles.add(file)
                }
            }
        }

        return foundFiles.distinctBy { it.absolutePath }
    }

    private fun scanDirectory(dir: File, depth: Int = 5, currentDepth: Int = 0): List<File> {
        if (currentDepth > depth || !dir.exists() || !dir.canRead()) return emptyList()
        val files = mutableListOf<File>()
        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && isSupportedFile(file) -> files.add(file)
                    file.isDirectory && !isSystemDirectory(file) -> files.addAll(scanDirectory(file, depth, currentDepth + 1))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao escanear ${dir.absolutePath}: ${e.message}")
        }
        return files
    }

    /**
     * Verifica se um arquivo tem extensão suportada
     */
    fun isSupportedFile(file: File): Boolean {
        return file.extension.lowercase() in SUPPORTED_EXTENSIONS
    }

    /**
     * Evita escanear pastas do sistema que não terão arquivos Minecraft
     */
    private fun isSystemDirectory(dir: File): Boolean {
        val name = dir.name.lowercase()
        return name in setOf("android", ".thumbnails", ".trash", "cache", "obb", "data", "proc", "sys")
    }

    /**
     * Analisa um arquivo ZIP e verifica se tem estrutura válida de Minecraft
     * Retorna o tipo de pacote detectado e a contagem de manifests
     */
    fun analyzeZipStructure(file: File): Triple<Boolean, PackType, Int> {
        try {
            val foldersFound = mutableSetOf<String>()
            var manifestCount = 0
            var detectedType = PackType.UNKNOWN

            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    val topFolder = name.split("/").firstOrNull() ?: ""

                    if (name == "manifest.json" || name.endsWith("/manifest.json")) {
                        manifestCount++
                    }
                    foldersFound.add(topFolder)
                    entry = zis.nextEntry
                }
            }

            detectedType = when {
                manifestCount > 1 -> PackType.ADDON
                foldersFound.any { it in RESOURCE_PACK_FOLDERS } -> PackType.RESOURCE_PACK
                foldersFound.any { it in BEHAVIOR_PACK_FOLDERS } -> PackType.BEHAVIOR_PACK
                foldersFound.any { it in WORLD_TEMPLATE_FOLDERS } -> PackType.WORLD_TEMPLATE
                file.extension.lowercase() == "mcaddon" -> PackType.ADDON
                manifestCount == 1 -> PackType.UNKNOWN // Will be refined by manifest parsing
                else -> PackType.UNKNOWN
            }

            val isValid = manifestCount > 0 || foldersFound.any { it in RESOURCE_PACK_FOLDERS || it in BEHAVIOR_PACK_FOLDERS || it in WORLD_TEMPLATE_FOLDERS }
            return Triple(isValid, detectedType, manifestCount)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar ZIP ${file.name}: ${e.message}")
            return Triple(false, PackType.UNKNOWN, 0)
        }
    }

    /**
     * Extrai as informações do manifest.json de dentro de um ZIP/MCPACK
     */
    fun extractManifestInfo(file: File): ManifestInfo? {
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name.lowercase() == "manifest.json" ||
                        entry.name.lowercase().endsWith("/manifest.json")) {

                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        return parseManifest(content)
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair manifest de ${file.name}: ${e.message}")
        }
        return null
    }

    /**
     * Extrai o pack_icon.png de dentro do arquivo e salva temporariamente
     */
    fun extractIcon(file: File, outputDir: File): File? {
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name.lowercase().endsWith("pack_icon.png")) {
                        val iconFile = File(outputDir, "${file.nameWithoutExtension}_icon.png")
                        iconFile.outputStream().use { out ->
                            zis.copyTo(out)
                        }
                        return iconFile
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair ícone de ${file.name}: ${e.message}")
        }
        return null
    }

    /**
     * Faz o parse do manifest.json e retorna as informações estruturadas
     */
    private fun parseManifest(jsonContent: String): ManifestInfo? {
        return try {
            // Usa JsonReader com setLenient(true) para aceitar JSONs com comentários ou erros leves
            val reader = com.google.gson.stream.JsonReader(java.io.StringReader(jsonContent))
            reader.isLenient = true
            val json = JsonParser.parseReader(reader).asJsonObject

            val header = json.getAsJsonObject("header")
            val name = header?.get("name")?.asString ?: "Pacote Sem Nome"
            val description = header?.get("description")?.asString ?: ""
            val uuid = header?.get("uuid")?.asString ?: ""

            val version = try {
                val vArr = header?.getAsJsonArray("version")
                if (vArr != null) {
                    "${vArr[0].asInt}.${vArr[1].asInt}.${vArr[2].asInt}"
                } else {
                    header?.get("version")?.asString ?: "1.0.0"
                }
            } catch (e: Exception) { "1.0.0" }

            val minEngineVersion = try {
                val vArr = header?.getAsJsonArray("min_engine_version")
                if (vArr != null) {
                    "${vArr[0].asInt}.${vArr[1].asInt}.${vArr[2].asInt}"
                } else { "" }
            } catch (e: Exception) { "" }

            val modules = json.getAsJsonArray("modules")
            val packType = if (modules != null && modules.size() > 0) {
                val moduleType = modules[0].asJsonObject.get("type")?.asString?.lowercase() ?: ""
                when {
                    moduleType.contains("resources") -> PackType.RESOURCE_PACK
                    moduleType.contains("data") -> PackType.BEHAVIOR_PACK
                    moduleType.contains("world_template") -> PackType.WORLD_TEMPLATE
                    moduleType.contains("skin_pack") -> PackType.SKIN_PACK
                    else -> PackType.UNKNOWN
                }
            } else { PackType.UNKNOWN }

            val metadata = json.getAsJsonObject("metadata")
            val authors = metadata?.getAsJsonArray("authors")
            val author = if (authors != null && authors.size() > 0) {
                authors[0].asString
            } else { "" }

            ManifestInfo(
                name = name,
                description = description,
                version = version,
                uuid = uuid,
                minEngineVersion = minEngineVersion,
                packType = packType,
                author = author
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parsear manifest: ${e.message}")
            null
        }
    }
}

/**
 * Dados extraídos do manifest.json
 */
data class ManifestInfo(
    val name: String,
    val description: String,
    val version: String,
    val uuid: String,
    val minEngineVersion: String,
    val packType: PackType,
    val author: String
)
