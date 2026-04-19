package com.ncmine.importmine.domain.model

import java.io.File

/**
 * Tipos de pacotes suportados pelo app
 */
enum class PackType {
    RESOURCE_PACK,     // Add-on de recursos (texturas, sons)
    BEHAVIOR_PACK,     // Add-on de comportamento (entidades, loot tables)
    WORLD_TEMPLATE,    // Template de mundo
    SKIN_PACK,         // Pacote de skins
    ADDON,             // Pacote .mcaddon (composto)
    UNKNOWN            // Estrutura não identificada
}

/**
 * Status do processo de análise/importação
 */
enum class PackStatus {
    PENDING,           // Aguardando análise
    ANALYZING,         // Em análise
    VALID,             // Arquivo válido e pronto para importar
    INVALID,           // Arquivo inválido ou corrompido
    CONVERTED,         // ZIP convertido para MCPACK com sucesso
    IMPORTING,         // Sendo importado no Minecraft
    IMPORTED,          // Importado com sucesso
    ERROR              // Erro durante o processo
}

/**
 * Modelo principal que representa um pacote Minecraft encontrado
 */
data class MinecraftPack(
    val id: String = java.util.UUID.randomUUID().toString(),

    // Arquivo original
    val originalFile: File,
    
    // Arquivo após conversão (pode ser o mesmo se já era .mcpack)
    var processedFile: File? = null,

    // Informações extraídas do manifest.json
    val name: String = "Pacote Desconhecido",
    val description: String = "",
    val version: String = "1.0.0",
    val author: String = "",
    val uuid: String = "",
    val minEngineVersion: String = "",

    // Tipo e status
    val packType: PackType = PackType.UNKNOWN,
    var status: PackStatus = PackStatus.PENDING,

    // Imagem do pacote (pack_icon.png)
    val iconFile: File? = null,

    // Metadados do arquivo
    val fileSizeBytes: Long = 0,
    val lastModified: Long = 0,

    // Caminho do backup
    var backupPath: String? = null,

    // Mensagem de erro, se houver
    var errorMessage: String? = null,

    // Histórico e Favoritos
    val isFavorite: Boolean = false,
    val isImported: Boolean = false,
    val isImporting: Boolean = false,

    // Metadados técnicos
    val manifestCount: Int = 0
) {
    /** Extensão do arquivo original */
    val originalExtension: String
        get() = originalFile.extension.lowercase()

    /** Se precisou de conversão ZIP → MCPACK/MCADDON */
    val wasConverted: Boolean
        get() = originalExtension == "zip" && (processedFile?.extension?.lowercase() == "mcpack" || processedFile?.extension?.lowercase() == "mcaddon")

    /** Tamanho formatado para exibição */
    val fileSizeFormatted: String
        get() = when {
            fileSizeBytes < 1024 -> "${fileSizeBytes} B"
            fileSizeBytes < 1024 * 1024 -> "${"%.1f".format(fileSizeBytes / 1024.0)} KB"
            fileSizeBytes < 1024 * 1024 * 1024 -> "${"%.1f".format(fileSizeBytes / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(fileSizeBytes / (1024.0 * 1024 * 1024))} GB"
        }

    /** Rótulo do tipo de pacote para UI */
    val packTypeLabel: String
        get() = when (packType) {
            PackType.RESOURCE_PACK -> "Resource Pack"
            PackType.BEHAVIOR_PACK -> "Behavior Pack"
            PackType.WORLD_TEMPLATE -> "World Template"
            PackType.SKIN_PACK -> "Skin Pack"
            PackType.ADDON -> "Minecraft Add-on"
            PackType.UNKNOWN -> "Add-on"
        }

    /** Cor do badge baseada no tipo de pacote (Long para Compose Color) */
    val packTypeBadgeColor: Long
        get() = when (packType) {
            PackType.RESOURCE_PACK -> 0xFF00E676 // NcGreenNeon
            PackType.BEHAVIOR_PACK -> 0xFF2979FF // Azul
            PackType.WORLD_TEMPLATE -> 0xFFFFC107 // Amber
            PackType.SKIN_PACK -> 0xFFE91E63 // Rosa
            PackType.ADDON -> 0xFF00E676 // NcGreenNeon
            PackType.UNKNOWN -> 0xFF9E9E9E // Cinza
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
