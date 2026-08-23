# 本机 AI/开发工具速查（Mekanism-Switch 项目视角 · MIKUFAN）

- 更新：2026-08-22（本次实测复核本机环境）
- 主机：Windows 10 专业版 10.0.19045 (22H2) · x64 · AMD Ryzen 7 5800H（8 核 16 线程）· 32 GB RAM；默认 Shell：`pwsh` 7.6.5（另有 powershell 5.1.19041 / cmd）
- 说明：本项目本地版环境速查（参考 `D:\Mikufan\Downloads\AI_TOOLS.md` 全局版重写）。与全局版唯一实测差异：**OS 实为 Windows 10 专业版**（全局版曾记 Enterprise LTSC 2024），**CPU 为 5800H（8 核/16 线程）**（全局版记 16 核）。
- 用法：工作前先跑「快速核对」；要精确路径/版本用 `Get-Command`；没列出的工具勿假设存在

## 快速核对

```powershell
Get-Command python,py,pip,node,npm,pnpm,java,javac,dotnet,git,tig,winget,choco,code | Select-Object Name,Source
```

## 语言与运行时（均在 PATH）

| 工具 | 版本 | 说明 |
|---|---|---|
| `python` / `py` / `pip` | 3.14.6 / pip 26.1.2 | `D:\Program Files\Python314`；勿用 `python3`（Store stub）；**无第三方库**，缺啥先 `pip install` |
| `node` / `npm` / `npx` / `pnpm` | v24.18.0 / 11.16.0 / 11.16.0 / 11.22.0 | `D:\Program Files\nodejs`；npm 全局：`@deepseek-ai/dsh`、`pnpm` |
| `java` / `javac` | OpenJDK 25.0.3 LTS（`JAVA_HOME`） | `D:\Program Files\Microsoft\jdk-25.0.3.9-hotspot`；**另有 jdk-21.0.12.8-hotspot（本项目构建用，见下）** |
| `dotnet` | 运行时 6.0.8 / 8.0.0 / 9.0.19 | **无 SDK**：不能 build/new，可运行已发布程序 |
| `git` / `git-lfs` / `tig` | 2.55.0.windows.3 / 3.7.1 / 2.6.1 | `D:\Program Files\Git\cmd` |
| `winget` / `choco` | 1.29.290 / 2.7.4 | 装软件 |

## 需绝对路径或先初始化

- **JDK 21**（Minecraft 1.21.1 / NeoForge 要求，也是本仓库 toolchain 版本）：`D:\Program Files\Microsoft\jdk-21.0.12.8-hotspot`。注意 `JAVA_HOME` 指向 JDK 25；Gradle 会通过 toolchain 自动选用 21（找不到时由 foojay-resolver 联网下载）。
- **Firefox 154.0**：`D:\Program Files\Mozilla Firefox\firefox.exe`（不在 PATH）
- **Git Bash**：`D:\Program Files\Git\bin\bash.exe -lc "<cmd>"`
- **WSL**：`wsl.exe` 存在但无发行版（需先 `wsl --install`）
- **MSVC**（Visual Studio Professional 2026 18.x）：`D:\Program Files\Microsoft Visual Studio\18\Professional\`；先初始化：`cmd /c "call "...\Common7\Tools\VsDevCmd.bat" && <cmd>"`；cl / MSBuild / CMake / Ninja 随 VS 提供（不在 PATH）
- **Qt 6.8.3 MinGW + 工具链**：`D:\Mikufan\Soft\Qt\6.8.3\mingw_64\bin` 与 `D:\Mikufan\Soft\Qt\Tools\mingw1310_64\bin`、`...\Tools\CMake_64\`、`...\Tools\Ninja\`、`...\Tools\QtCreator\`（均不在 PATH，用绝对路径）

## 系统与硬件

- GPU：RTX 3060 Laptop，驱动 610.88（`nvidia-smi` 可用）
- `curl`(system32)、OpenSSH(`ssh`/`scp`/`sftp`)、`tar`、`robocopy`、`icacls`、`certutil` 等系统工具在 PATH
- IDE：VS Code 1.134.0（`D:\Program Files\Microsoft VS Code\bin`）—— IntelliJ IDEA 已卸载
- 无 `HTTP_PROXY`/`HTTPS_PROXY`（需代理自行设置）

## 已移除 / 未安装（勿再假设可用）

- `rg`（随 Codex 桌面应用一起移除；全文搜索用 `Select-String`/`git grep`）；poppler、clinfo、Pillow 等 codex-runtimes 产物
- IntelliJ IDEA 2026.1、Codex 桌面应用与 `~\.codex`、Codex++、CCSwitch、VS 2026 Build Tools
- ffmpeg / ImageMagick(`magick`) / LibreOffice；Go / Rust / PHP / Ruby / Perl / Deno / Bun；Docker / K8s / Helm；WSL 发行版；.NET SDK；sqlite3 / psql / mysql / redis-cli；aws / az / gcloud；Playwright / Selenium
- 详细清单见 `D:\Mikufan\Downloads\AI_TOOLS.md`（全局版）

## 本项目（Mekanism-Switch）构建速查

| 项 | 值 |
|---|---|
| 构建 | `.\gradlew.bat build`（产物 `build/libs/meks-0.2.2.jar`，约 314 KB） |
| 运行客户端 / 服务端 | `.\gradlew.bat runClient` / `runServer` |
| 数据生成 / GameTest | `.\gradlew.bat runData` / `runGameTestServer`（本项目未使用 datagen，资源均在 `src/main/resources`） |
| JDK | 21（Minecraft 1.21.1 要求；本机已装 jdk-21.0.12.8） |
| Gradle | wrapper 9.2.1（`gradle/wrapper/gradle-wrapper.properties`） |
| 依赖仓库 | `modmaven.dev`（Mekanism `1.21.1-10.7.19.85`，runtime 走 `localRuntime`，开发运行自动带上，不需手放 mods） |
| 备注 | 项目根 `.gradle\9.2.1` 有先前构建的缓存（疑似 IDE 曾把 Gradle 用户缓存指向项目）；`%USERPROFILE%\.gradle` 已有 wrapper 发行版缓存 + 代理 gradle.properties（见下方「代理环境」） |
| CI | `.github/workflows/build.yml`：ubuntu + JDK 21 (temurin) + `./gradlew build` |

### 测试部署（本地实测）

- 构建产物（`build/libs/meks-0.2.2.jar`）拷到测试档的 **mods 子目录**（不是 versions 目录本身）：

```powershell
.\gradlew.bat build
Copy-Item .\build\libs\meks-0.3.0.jar 'D:\Mikufan\Soft\Minecraft\.minecraft\versions\Mekanism-Switch\mods\' -Force
```

- 测试档：`D:\Mikufan\Soft\Minecraft\.minecraft\versions\Mekanism-Switch\`（版本名 = Mekanism-Switch，含 mods 子目录）。拷完后启动器切到该版本即可进游戏测试；旧 jar 会被 `-Force` 覆盖。

### 代理环境（2026-08-22 实测）

- 本机**无系统级/环境变量代理**；代理是 **sing-box**（进程名 `sing-box`）的本地 HTTP 代理：**`127.0.0.1:616`**。命令行走代理用 `curl.exe -x http://127.0.0.1:616 <url>`。
- Gradle 走代理：已写**用户级** `%USERPROFILE%\.gradle\gradle.properties`（只影响本机，不进仓库）：

```
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=616
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=616
systemProp.http.nonProxyHosts=localhost|127.0.0.1|127.*|[::1]
```

（daemon 下载依赖时生效；**关代理后删掉该文件**，否则新下载会走死代理。）
- wrapper 发行版 `gradle-9.2.1-bin.zip`（135.5MB）已用 curl 经代理下到 `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.2.1-bin\2t0n5ozlw9xmuyvbp7dnzaxug\`，构建不再需要联网下载发行版。
- ⚠️ 坑：**不要用 `GRADLE_OPTS`/`-Dhttp.nonProxyHosts=...` 传值**——值里的 `|` 会被 `gradlew.bat`（cmd）当管道拆断，报 `'127.0.0.1' is not recognized ...`。代理配置一律走 gradle.properties 的 `systemProp.*`。

> 项目结构、核心系统与代码审查见同目录 `PROJECT_OVERVIEW.md`。