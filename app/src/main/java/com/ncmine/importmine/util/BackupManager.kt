package com.ncmine.importmine.util

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "BackupManager"
private const val BACKUP_FOLDER = "NC Import Mine/Backup"

/**
 * Gerencia o backup automático dos arquivos originais
 * Nunca deixa o usuário perder um arquivo!
 */
@Singleton
class BackupManager @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Retorna o diretório de backup (cria se não existir)
     */
    fun getBackupDirectory(): File {
        val baseDir = Environment.getExternalStorageDirectory()
        val backupDir = File(baseDir, BACKUP_FOLDER)

        if (!backupDir.exists()) {
            val created = backupDir.mkdirs()
            Log.d(TAG, "Diretório de backup criado: $created → ${backupDir.absolutePath}")
        }

        return backupDir
    }

    /**
     * Faz o backup de um arquivo original antes de qualquer processamento.
     * O backup inclui data e hora no nome para facilitar identificação.
     *
     * @param originalFile Arquivo original a fazer backup
     * @return O arquivo de backup criado, ou null em caso de erro
     */
    fun backupFile(originalFile: File): File? {
        if (!originalFile.exists()) {
            Log.w(TAG, "Arquivo original não existe: ${originalFile.absolutePath}")
            return null
        }

        return try {
            val backupDir = getBackupDirectory()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupName = "${originalFile.nameWithoutExtension}_backup_$timestamp.${originalFile.extension}"
            val backupFile = File(backupDir, backupName)

            originalFile.copyTo(backupFile, overwrite = false)

            Log.d(TAG, "Backup criado: ${backupFile.absolutePath}")
            backupFile

        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar backup de ${originalFile.name}: ${e.message}")
            null
        }
    }

    /**
     * Lista todos os backups salvos, do mais recente para o mais antigo
     */
    fun listBackups(): List<BackupEntry> {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                BackupEntry(
                    file = file,
                    originalName = extractOriginalName(file.name),
                    backupDate = Date(file.lastModified()),
                    sizeMb = file.length() / (1024.0 * 1024.0)
                )
            } ?: emptyList()
    }

    /**
     * Remove backups mais antigos que X dias (limpeza automática)
     */
    fun cleanOldBackups(keepDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        val backupDir = getBackupDirectory()

        backupDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                val deleted = file.delete()
                Log.d(TAG, "Backup antigo removido ($deleted): ${file.name}")
            }
        }
    }

    /**
     * Extrai o nome original do arquivo a partir do nome do backup
     */
    private fun extractOriginalName(backupName: String): String {
        return backupName.replace(Regex("_backup_\\d{8}_\\d{6}"), "")
    }

    /**
     * Calcula o tamanho total dos backups em MB
     */
    fun getTotalBackupSizeMb(): Double {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) return 0.0

        return backupDir.listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() }
            ?.div(1024.0 * 1024.0) ?: 0.0
    }
}

data class BackupEntry(
    val file: File,
    val originalName: String,
    val backupDate: Date,
    val sizeMb: Double
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(backupDate)

    val formattedSize: String
        get() = "${"%.1f".format(sizeMb)} MB"
}
