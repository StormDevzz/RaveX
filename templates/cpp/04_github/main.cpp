// ══════════════════════════════════════════════════════════════════════════════
//  04_github / main.cpp
//
//  RU: Пример C++ аддона, использующего ravex_github_tools для
//      автоматического обновления через GitHub Releases.
//
//      Демонстрирует:
//        1. ReleaseChecker — проверить, вышла ли новая версия
//        2. ReleaseManager — полный цикл: check -> download -> install -> rollback
//        3. Собственные HTTP-запросы к GitHub API через HttpClient
//        4. Парсинг JSON из ответа GitHub API
//
//      Библиотека ravex_github_tools не имеет внешних зависимостей:
//        - HTTP: WinHTTP (Windows) или POSIX sockets + OpenSSL (Linux)
//        - JSON: собственный парсер (без nlohmann/json, без RapidJSON)
//        - Semver: собственная реализация Version::compare()
//
//  EN: Example C++ addon using ravex_github_tools for
//      automatic updates via GitHub Releases.
//
//      Demonstrates:
//        1. ReleaseChecker — check if a new version is available
//        2. ReleaseManager — full cycle: check -> download -> install -> rollback
//        3. Custom HTTP requests to GitHub API via HttpClient
//        4. JSON parsing from GitHub API response
//
//      The ravex_github_tools library has no external dependencies:
//        - HTTP: WinHTTP (Windows) or POSIX sockets + OpenSSL (Linux)
//        - JSON: self-contained parser (no nlohmann/json, no RapidJSON)
//        - Semver: self-contained Version::compare() implementation
// ══════════════════════════════════════════════════════════════════════════════

#include "ravex/github/release_checker.h"
#include "ravex/github/release_manager.h"
#include "ravex/github/github_types.h"

#include <iostream>
#include <thread>
#include <chrono>

// RU: Платформенно-независимая задержка для колбэков прогресса.
// EN: Platform-independent delay for progress callbacks.
#ifdef _WIN32
    #include <windows.h>
    #define SLEEP_MS(ms) Sleep(ms)
#else
    #include <unistd.h>
    #define SLEEP_MS(ms) usleep((ms) * 1000)
#endif

// ─── Колбэки / Callbacks ─────────────────────────────────────────────────────
//
// RU: Эти функции передаются в ReleaseManager для отслеживания
//     прогресса загрузки, логирования и статуса.
//     Ты можешь заменить их на свои — например, показывать прогресс-бар
//     в GUI оверлея.
// EN: These functions are passed to ReleaseManager to track
//     download progress, logging and status.
//     You can replace them with your own — e.g. show a progress bar
//     in the GUI overlay.

// RU: Колбэк прогресса — вызывается во время загрузки файла.
//     downloaded — сколько байт уже скачано.
//     total — общий размер файла (0, если неизвестен).
// EN: Progress callback — called during file download.
//     downloaded — bytes downloaded so far.
//     total — total file size (0 if unknown).
static void onProgress(int64_t downloaded, int64_t total) {
    if (total > 0) {
        int pct = (int)(downloaded * 100 / total);
        std::cout << "\r  Progress: " << pct << "% (" << downloaded << "/" << total << " bytes)";
        std::cout.flush();
    } else {
        std::cout << "\r  Downloaded: " << downloaded << " bytes";
        std::cout.flush();
    }
}

// RU: Колбэк логирования — получает сообщения от библиотеки.
//     level — уровень логирования (DEBUG, INFO, WARN, ERROR).
//     msg — текст сообщения.
// EN: Logging callback — receives messages from the library.
//     level — log level (DEBUG, INFO, WARN, ERROR).
//     msg — message text.
static void onLog(ravex::github::LogLevel level, const std::string& msg) {
    static const char* labels[] = { "DEBUG", "INFO", "WARN", "ERROR" };
    std::cout << "[" << labels[(int)level] << "] " << msg << std::endl;
}

// RU: Колбэк статуса — вызывается при изменении состояния менеджера.
// EN: Status callback — called on manager state changes.
static void onStatus(const std::string& status) {
    std::cout << "[STATUS] " << status << std::endl;
}

// ─── Пример 1: ReleaseChecker ────────────────────────────────────────────────
//
// RU: Простейшая проверка: получить последний релиз с GitHub,
//     сравнить версии, посмотреть доступные ассеты для платформы.
//
//     ReleaseChecker не требует конфига — достаточно owner и repo.
//     Подходит, если тебе нужно только проверить наличие обновления.
//
// EN: Simplest check: fetch the latest release from GitHub,
//     compare versions, look at matching assets for your platform.
//
//     ReleaseChecker does not need a config — just owner and repo.
//     Good when you only need to check if an update exists.

void exampleReleaseChecker() {
    std::cout << "\n=== ReleaseChecker Example ===" << std::endl;

    // RU: Создаём чекер для репозитория StormDevzz/RaveX.
    //     Замени owner/repo на свои.
    // EN: Create a checker for the StormDevzz/RaveX repository.
    //     Replace owner/repo with your own.
    ravex::github::ReleaseChecker checker("StormDevzz", "RaveX");

    checker.setLogCallback(onLog);

    // RU: Проверяем обновления относительно текущей версии "1.4.1".
    //     info содержит результат: есть ли обновление, какая версия,
    //     какие ассеты доступны.
    // EN: Check for updates relative to current version "1.4.1".
    //     info contains the result: whether an update is available,
    //     what version, what assets are available.
    auto info = checker.checkForUpdates("1.4.1");

    if (info.error) {
        std::cerr << "  Error: " << info.errorMessage << std::endl;
        return;
    }

    std::cout << "  Local version:  " << info.localVersion.toString() << std::endl;
    std::cout << "  Remote version: " << info.remoteVersion.toString() << std::endl;
    std::cout << "  Update available: " << (info.available ? "YES" : "NO") << std::endl;

    if (!info.release.tagName.empty()) {
        std::cout << "  Release name: " << info.release.name << std::endl;
        std::cout << "  Release URL:  " << info.release.htmlUrl << std::endl;
    }

    // RU: Ассеты, подходящие под платформу (Windows — .dll/.exe/.zip,
    //     Linux — .so/.tar.gz/AppImage и т.д.).
    // EN: Assets matching the platform (Windows — .dll/.exe/.zip,
    //     Linux — .so/.tar.gz/AppImage, etc.).
    if (!info.matchingAssets.empty()) {
        std::cout << "  Matching assets for this platform:" << std::endl;
        for (const auto& asset : info.matchingAssets) {
            std::cout << "    - " << asset.name << " (" << asset.size << " bytes)" << std::endl;
        }
    }
}

// ─── Пример 2: ReleaseManager (полный цикл) ──────────────────────────────────
//
// RU: Полный менеджер обновлений. Делает всё:
//     1. config -> GithubConfig (owner, repo, currentVersion, channel...)
//     2. check() -> проверить, есть ли обновление
//     3. download() -> скачать ассет с прогресс-колбэком
//     4. install() -> установить (с бекапом текущей версии)
//     5. rollback() -> откатить, если что-то пошло не так
//
//     install() создаёт .bak копии перед заменой файлов.
//     rollback() восстанавливает их при необходимости.
//
// EN: Full update manager. Does everything:
//     1. config -> GithubConfig (owner, repo, currentVersion, channel...)
//     2. check() -> check if update is available
//     3. download() -> download asset with progress callback
//     4. install() -> install (with backup of current version)
//     5. rollback() -> roll back if something goes wrong
//
//     install() creates .bak copies before replacing files.
//     rollback() restores them if needed.

void exampleReleaseManager() {
    std::cout << "\n=== ReleaseManager Example ===" << std::endl;

    // RU: Конфигурация — заполни своими данными.
    //     currentVersion — текущая версия аддона (для сравнения).
    //     channel — Stable (только стабильные релизы) или Preview (включая pre-release).
    //     verifyChecksums — проверять SHA-256 после загрузки.
    // EN: Configuration — fill with your own data.
    //     currentVersion — current addon version (for comparison).
    //     channel — Stable (only stable releases) or Preview (including pre-release).
    //     verifyChecksums — verify SHA-256 after download.
    ravex::github::GithubConfig cfg;
    cfg.owner          = "StormDevzz";
    cfg.repo           = "RaveX";
    cfg.currentVersion = "1.4.1";
    cfg.channel        = ravex::github::ReleaseChannel::Stable;
    cfg.verifyChecksums = true;

    ravex::github::ReleaseManager manager(cfg);

    manager.onLog(onLog);
    manager.onProgress(onProgress);
    manager.onStatus(onStatus);

    // 1. Проверка / Check
    std::cout << "  Checking for updates..." << std::endl;
    auto info = manager.check();

    if (info.error) {
        std::cerr << "  Check failed: " << info.errorMessage << std::endl;
        return;
    }

    if (!info.available) {
        std::cout << "  Already up to date." << std::endl;
        return;
    }

    std::cout << "  Update found: " << info.remoteVersion.toString() << std::endl;

    // 2. Загрузка / Download
    std::cout << "  Downloading..." << std::endl;
    auto dl = manager.download(info);

    if (!dl.success) {
        std::cerr << "  Download failed: " << dl.errorMsg << std::endl;
        return;
    }

    std::cout << "\n  Downloaded to: " << dl.filePath << std::endl;
    if (!dl.checksumSha256.empty()) {
        std::cout << "  SHA-256: " << dl.checksumSha256 << std::endl;
    }

    // 3. Установка / Install
    std::cout << "  Installing..." << std::endl;
    if (manager.install(dl)) {
        std::cout << "  Install succeeded!" << std::endl;
    } else {
        std::cerr << "  Install failed: " << manager.lastError() << std::endl;
        return;
    }

    // 4. Откат / Rollback
    std::cout << "  Rolling back..." << std::endl;
    if (manager.rollback()) {
        std::cout << "  Rollback succeeded." << std::endl;
    } else {
        std::cout << "  No backups to restore." << std::endl;
    }
}

// ─── Пример 3: Пользовательский HTTP / Custom HTTP ───────────────────────────
//
// RU: Показывает, как использовать HttpClient библиотеки напрямую
//     для выполнения произвольных запросов к GitHub API (или любому
//     другому HTTP-серверу). Ответ парсится через JsonValue::parse.
//
// EN: Shows how to use the library's HttpClient directly
//     for arbitrary requests to the GitHub API (or any other
//     HTTP server). The response is parsed via JsonValue::parse.

void exampleCustomHttp() {
    std::cout << "\n=== Custom HTTP Example ===" << std::endl;

    // RU: Получаем ссылку на HttpClient из ReleaseChecker.
    //     Можно также создать HttpClient напрямую.
    // EN: Get a reference to HttpClient from ReleaseChecker.
    //     You can also create an HttpClient directly.
    ravex::github::ReleaseChecker checker("StormDevzz", "RaveX");
    auto& http = checker.http();

    // RU: GET-запрос к GitHub API для получения информации о репозитории.
    // EN: GET request to GitHub API for repository info.
    auto resp = http.get("https://api.github.com/repos/StormDevzz/RaveX");
    if (resp.error) {
        std::cerr << "  HTTP error: " << resp.errorMsg << std::endl;
        return;
    }

    // RU: Парсим JSON-ответ. JsonValue::parse — самописный парсер,
    //     без внешних библиотек. Поддерживает объекты, массивы,
    //     строки, числа, bool, null.
    // EN: Parse the JSON response. JsonValue::parse is self-written,
    //     no external libraries. Supports objects, arrays, strings,
    //     numbers, bool, null.
    auto json = ravex::github::JsonValue::parse(resp.body);
    if (json.isObject()) {
        std::cout << "  Repo:     " << json["full_name"].asString() << std::endl;
        std::cout << "  Stars:    " << json["stargazers_count"].asInt() << std::endl;
        std::cout << "  Language: " << json["language"].asString() << std::endl;
        std::cout << "  Forks:    " << json["forks_count"].asInt() << std::endl;
        if (json.has("license") && !json["license"].isNull())
            std::cout << "  License:  " << json["license"]["name"].asString() << std::endl;
    }
}

// ─── Main ────────────────────────────────────────────────────────────────────
//
// RU: Точка входа для тестирования без загрузки в RaveX.
//     Когда RAVEX_ADDON_MODE не определён, работает как отдельная
//     консольная программа для отладки GitHub-функций.
//
//     Для использования как аддон RaveX, определи RAVEX_ADDON_MODE
//     и добавь extern "C" точки входа (createAddon / destroyAddon).
//
// EN: Entry point for testing without loading into RaveX.
//     When RAVEX_ADDON_MODE is not defined, runs as a standalone
//     console program for debugging GitHub functions.
//
//     To use as a RaveX addon, define RAVEX_ADDON_MODE and add
//     extern "C" entry points (createAddon / destroyAddon).

#ifndef RAVEX_ADDON_MODE
int main() {
    std::cout << "RaveX GitHub Addon Example" << std::endl;
    std::cout << "==========================" << std::endl;

    exampleReleaseChecker();
    exampleCustomHttp();

    // RU: ReleaseManager требует реальный GitHub релиз.
    //     Раскомментируй для теста с репозиторием, у которого есть релизы.
    // EN: ReleaseManager requires a real GitHub release.
    //     Uncomment to test with a repository that has releases.
    // exampleReleaseManager();

    std::cout << "\nDone." << std::endl;
    return 0;
}
#endif
