# ⛏️ NC Import Mine
### Importador de Add-ons Minecraft para Android
> Desenvolvido por **NCMINE**

---

## 🎯 O que é este app?

O **NC Import Mine** é um app Android que permite importar add-ons, worlds e resource packs do Minecraft com **um único clique**. Ele escaneia a pasta Downloads, converte ZIPs automaticamente, faz backup dos seus arquivos e os abre direto no Minecraft Bedrock Edition.

---

## ✅ Funcionalidades

| Função | Descrição |
|--------|-----------|
| 🔍 **Scanner automático** | Varre Downloads, Documents e a pasta do Minecraft |
| 📦 **Suporte total** | `.mcpack`, `.mcworld` e `.zip` com estrutura Minecraft |
| 💾 **Backup automático** | Cópia dos originais em `NC Import Mine/Backup/` |
| 🔄 **Conversão ZIP→MCPACK** | Converte automaticamente, sem alterar o conteúdo |
| 🖼️ **Info do add-on** | Extrai nome, descrição, versão, autor e ícone do `manifest.json` |
| 📲 **Importar com 1 clique** | Abre diretamente no Minecraft Bedrock |
| 💰 **AdMob** | Banner no rodapé + Interstitial após cada importação |

---

## 🚀 Como compilar e testar em 5 minutos

### Pré-requisitos

- [Android Studio Hedgehog ou mais recente](https://developer.android.com/studio) (2023.1.1+)
- JDK 17 (incluído no Android Studio)
- Android SDK 34 instalado
- Dispositivo Android 8.0+ ou emulador API 26+

---

### Passo a passo

#### 1️⃣ Abrir no Android Studio

```bash
# Clone ou extraia o projeto
# Abra o Android Studio → File → Open → selecione a pasta NCImportMine
```

Aguarde o Gradle sincronizar (primeira vez pode demorar ~2 minutos).

#### 2️⃣ Configurar o AdMob (opcional para teste)

Os IDs de teste do AdMob já estão configurados. Para usar seus próprios:

1. Acesse [admob.google.com](https://admob.google.com)
2. Crie um app e copie o **App ID**
3. Edite `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" />
```
4. Edite `AdMobManager.kt` e substitua os IDs de banner e interstitial.

#### 3️⃣ Gerar o APK de debug

**Pelo terminal (na raiz do projeto):**

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**Ou pelo Android Studio:**
- Menu `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

O APK ficará em:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 4️⃣ Instalar no dispositivo

```bash
# Via ADB (USB ou wireless)
adb install app/build/outputs/apk/debug/app-debug.apk

# Ou transfira o APK para o celular e instale manualmente
# (ative "Fontes desconhecidas" em Configurações → Segurança)
```

---

## 📱 Como usar o app

1. **Baixe** um arquivo `.mcpack`, `.mcworld` ou `.zip` Minecraft na pasta **Downloads**
2. Abra o NC Import Mine
3. Conceda a **permissão de armazenamento** (obrigatória)
4. Toque em **"🔍 Analisar Downloads Agora"**
5. O app exibe os pacotes encontrados com nome, ícone e descrição
6. Toque em **"IMPORTAR"** — o arquivo abre automaticamente no Minecraft!

---

## 🗂️ Estrutura do Projeto

```
NCImportMine/
├── app/src/main/
│   ├── java/com/ncmine/importmine/
│   │   ├── MainActivity.kt              # Entry point
│   │   ├── model/
│   │   │   └── MinecraftPack.kt        # Data classes
│   │   ├── viewmodel/
│   │   │   └── MainViewModel.kt        # MVVM ViewModel
│   │   ├── repository/
│   │   │   └── FileRepository.kt       # Operações de arquivo
│   │   ├── util/
│   │   │   ├── FileScanner.kt          # Scanner de diretórios
│   │   │   ├── BackupManager.kt        # Backup automático
│   │   │   ├── PackConverter.kt        # ZIP → MCPACK + importação
│   │   │   └── AdMobManager.kt         # Gerenciador de anúncios
│   │   └── ui/
│   │       ├── theme/                   # Cores, tipografia, tema
│   │       └── screens/
│   │           ├── HomeScreen.kt        # Tela principal
│   │           └── PermissionScreen.kt  # Tela de permissão
│   ├── res/
│   │   ├── values/   # strings, colors, themes
│   │   ├── xml/      # file_paths, backup_rules
│   │   └── drawable/ # ícones vetoriais
│   └── AndroidManifest.xml
├── app/build.gradle.kts                 # Dependências do módulo
├── build.gradle.kts                     # Build raiz
├── settings.gradle.kts
└── gradle.properties
```

---

## 🎨 Design

| Elemento | Valor |
|----------|-------|
| Fundo principal | `#0A0A0A` (preto profundo) |
| Cor de destaque | `#00FF9F` (verde neon) |
| Cards | `#111111` com borda neon |
| Tipografia | Roboto / Material3 |
| Animações | Compose Animation + pulsação |

---

## 🔐 Permissões explicadas

| Permissão | Por quê? |
|-----------|---------|
| `READ_EXTERNAL_STORAGE` | Ler arquivos na pasta Downloads (Android 8-12) |
| `WRITE_EXTERNAL_STORAGE` | Salvar backups (Android 8-9) |
| `MANAGE_EXTERNAL_STORAGE` | Acesso completo necessário para ler/gravar backups (Android 11+) |
| `INTERNET` | Carregar anúncios AdMob |

---

## ⚠️ Observações importantes

- **Minecraft Bedrock Edition** deve estar instalado para a importação funcionar
- No **Android 11+**, o usuário precisa autorizar manualmente "Permitir gerenciamento de todos os arquivos" nas configurações
- Os backups ficam em `/sdcard/NC Import Mine/Backup/`
- Arquivos convertidos ficam em `/sdcard/NC Import Mine/Converted/`
- Os IDs de AdMob padrão são os de **TESTE** do Google — substitua antes de publicar!

---

## 🛠️ Tecnologias utilizadas

- **Kotlin** — linguagem principal
- **Jetpack Compose + Material3** — UI moderna e declarativa
- **ViewModel + StateFlow** — arquitetura MVVM
- **Coroutines** — operações assíncronas sem travar a UI
- **Coil** — carregamento de imagens (ícones dos packs)
- **Accompanist Permissions** — gerenciamento de permissões
- **AdMob SDK** — monetização (banner + interstitial)
- **Gson** — parse do `manifest.json`
- **Core Splashscreen** — splash screen nativa API 31+
- **FileProvider** — compartilhamento seguro de arquivos

---

## 📦 Comandos úteis

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requer keystore configurado)
./gradlew assembleRelease

# Limpar build
./gradlew clean

# Instalar via ADB diretamente
./gradlew installDebug

# Ver todos os tasks disponíveis
./gradlew tasks
```

---

## 📄 Licença

Desenvolvido por **NCMINE**. Todos os direitos reservados.

---

*NC Import Mine — Feito com 💚 para a comunidade Minecraft*
