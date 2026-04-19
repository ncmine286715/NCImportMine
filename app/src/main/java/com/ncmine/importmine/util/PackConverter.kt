package com.ncmine.importmine.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PackConverter"
private const val OUTPUT_FOLDER = "NC Import Mine/Converted"

/**
 * Responsável por converter arquivos ZIP em MCPACK e iniciar a importação no Minecraft
 */
@Singleton
class PackConverter @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Retorna o diretório de saída seguro para compartilhamento
     */
    fun getOutputDirectory(context: Context): File {
        // Usar getExternalFilesDir garante que o FileProvider tenha acesso fácil e não precise de MANAGE_EXTERNAL_STORAGE para o Minecraft ler
        val outputDir = File(context.getExternalFilesDir(null), "Converted")
        if (!outputDir.exists()) outputDir.mkdirs()
        return outputDir
    }

    /**
     * Converte um arquivo .zip com estrutura válida de Minecraft para .mcpack ou .mcaddon
     *
     * @param context Contexto para localizar pasta segura
     * @param zipFile Arquivo ZIP original
     * @param manifestCount Quantidade de manifests encontrados (para decidir a extensão)
     * @return O arquivo convertido, ou null em caso de erro
     */
    fun convertZipToMinecraft(context: Context, zipFile: File, manifestCount: Int = 1): File? {
        if (!zipFile.exists()) {
            Log.e(TAG, "Arquivo ZIP não encontrado: ${zipFile.absolutePath}")
            return null
        }

        return try {
            val outputDir = getOutputDirectory(context)
            val extension = if (manifestCount > 1) "mcaddon" else "mcpack"
            val convertedFile = File(outputDir, "${zipFile.nameWithoutExtension}.$extension")

            // Copia o arquivo com a nova extensão
            zipFile.copyTo(convertedFile, overwrite = true)

            Log.d(TAG, "Convertido com sucesso ($extension): ${convertedFile.absolutePath}")
            convertedFile

        } catch (e: Exception) {
            Log.e(TAG, "Falha na conversão de ${zipFile.name}: ${e.message}")
            null
        }
    }

    /**
     * Abre o arquivo .mcpack, .mcworld ou .mcaddon no Minecraft (ou em qualquer app compatível)
     * Usa FileProvider para garantir segurança no compartilhamento
     *
     * @param context Contexto do Android
     * @param file Arquivo a importar
     * @return true se o Intent foi disparado com sucesso, false caso contrário
     */
    fun importIntoMinecraft(context: Context, file: File): Boolean {
        Log.i(TAG, "Tentando importar: ${file.absolutePath}")
        return try {
            // Garante que o arquivo existe e é legível
            if (!file.exists()) {
                Log.e(TAG, "Arquivo não existe para importar: ${file.absolutePath}")
                return false
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Minecraft responde bem ao MIME 'application/octet-stream' para suas extensões proprietárias
            // mas forçamos o mime-type correto para garantir que o Minecraft apareça no seletor
            val mimeType = when (file.extension.lowercase()) {
                "mcpack" -> "application/x-minecraft-pack"
                "mcworld" -> "application/x-minecraft-world"
                "mcaddon" -> "application/x-minecraft-addon"
                else -> "application/octet-stream"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Criamos o chooser imediatamente para garantir que o usuário veja as opções
            // e forçamos o Minecraft como sugestão se disponível
            val chooserTitle = "Importar ${file.name} para o Minecraft"
            val chooserIntent = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
            Log.d(TAG, "Sucesso: Seletor de importação disparado para ${file.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Erro fatal ao importar ${file.name}: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica se o Minecraft Bedrock está instalado no dispositivo
     */
    fun isMinecraftInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.mojang.minecraftpe", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
