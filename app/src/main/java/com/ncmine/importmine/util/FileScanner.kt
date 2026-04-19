package com.ncmine.importmine.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ncmine.importmine.domain.model.PackType
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import javax.inject.Inject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val TAG = "FileScanner"

/**
 * Responsável por escanear diretórios e detectar arquivos Minecraft válidos
 */
@Singleton
class FileScanner @Inject constructor(@ApplicationContext private val context: Context) {

    // Extensões que o app processa
    private val SUPPORTED_EXTENSIONS = setOf("mcpack", "mcworld", "mcaddon", "zip")

    // Estruturas válidas de pacotes Minecraft
    private val RESOURCE_PACK_FOLDERS = setOf("textures", "sounds", "models", "ui", "animations")
    private val BEHAVIOR_PACK_FOLDERS = setOf("entities", "blocks", "items", "scripts", "loot_tables")
    private val WORLD_TEMPLATE_FOLDERS = setOf("world_template", "db", "level.dat")

    /**
     * Escaneia diretórios e emite arquivos encontrados um por um via Flow.
     * Otimizado para evitar recursão excessiva e garantir emissão fluida.
     */
    fun scanAllDirectoriesFlow(context: Context): Flow<File> = flow {
        val roots = mutableListOf<File>()
        
        // 1. Pasta Downloads (Principal fonte de addons)
        roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        
        // 2. Pasta Documents
        roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
        
        // 3. Pasta do Minecraft (Legado)
        roots.add(File(Environment.getExternalStorageDirectory(), "games/com.mojang"))
        
        // 4. Raiz do armazenamento
        roots.add(Environment.getExternalStorageDirectory())

        roots.distinct().forEach { root ->
            if (root.exists() && root.canRead()) {
                scanDirectoryRecursive(root, if (root.name == "com.mojang") 2 else 5).collect { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun scanDirectoryRecursive(dir: File, maxDepth: Int, currentDepth: Int = 0): Flow<File> = flow {
        if (currentDepth > maxDepth || !dir.exists() || !dir.canRead()) return@flow

        val files = dir.listFiles() ?: return@flow
        for (file in files) {
            if (file.isFile) {
                if (isSupportedFile(file)) {
                    emit(file)
                }
            } else if (file.isDirectory && !isSystemDirectory(file)) {
                scanDirectoryRecursive(file, maxDepth, currentDepth + 1).collect { emit(it) }
            }
        }
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
