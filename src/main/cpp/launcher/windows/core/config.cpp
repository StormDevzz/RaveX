#include "core/include/config.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <cstdio>
#include <utility>
#include <chrono>
#include <mutex>

namespace ravex {

namespace {

std::string jsonEscape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    out.push_back('"');
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", static_cast<unsigned int>(c));
                    out += buf;
                } else {
                    out.push_back(c);
                }
        }
    }
    out.push_back('"');
    return out;
}

std::wstring launcherConfigPath() {
    return joinPath(kickxDir(), L"launcher.json");
}

void deleteTree(const std::wstring& path) {
    WIN32_FIND_DATAW fd;
    std::wstring pattern = joinPath(path, L"*");
    HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring child = joinPath(path, name);
            if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                deleteTree(child);
            } else {
                SetFileAttributesW(child.c_str(), FILE_ATTRIBUTE_NORMAL);
                DeleteFileW(child.c_str());
            }
        } while (FindNextFileW(hFind, &fd));
        FindClose(hFind);
    }
    SetFileAttributesW(path.c_str(), FILE_ATTRIBUTE_NORMAL);
    RemoveDirectoryW(path.c_str());
}

using json::Value;

struct InstanceCache {
    std::vector<InstanceCfg> instances;
    std::chrono::steady_clock::time_point lastScan{};
    std::wstring lastDir;
    std::mutex mutex;
};

InstanceCache g_instanceCache;

void readBool(const Value& v, const char* key, bool& out) {
    if (v.has(key) && v.at(key).type() == Value::Type::Bool) out = v.at(key).asBool();
}

void readNum(const Value& v, const char* key, int& out) {
    if (v.has(key) && v.at(key).type() == Value::Type::Number) out = static_cast<int>(v.at(key).asNumber());
}

void readNumU(const Value& v, const char* key, uint32_t& out) {
    if (v.has(key) && v.at(key).type() == Value::Type::Number) out = static_cast<uint32_t>(v.at(key).asNumber());
}

void readStr(const Value& v, const char* key, std::string& out) {
    if (v.has(key) && v.at(key).type() == Value::Type::String) out = v.at(key).asString();
}

}

LauncherConfig loadLauncherConfig() {
    LauncherConfig cfg;
    std::string text;
    if (!readFile(launcherConfigPath(), text)) return cfg;
    Value root = Value::parse(text);
    if (root.isNull()) return cfg;
    readStr(root, "javaPath", cfg.javaPath);
    readBool(root, "checkUpdatesOnStart", cfg.checkUpdatesOnStart);
    readBool(root, "showSnapshots", cfg.showSnapshots);
    readBool(root, "showBeta", cfg.showBeta);
    readBool(root, "showAlpha", cfg.showAlpha);
    readNum(root, "activeAccount", cfg.activeAccount);
    readStr(root, "theme", cfg.theme);
    readStr(root, "language", cfg.language);
    readNumU(root, "customBg", cfg.customBg);
    readNumU(root, "customPanel", cfg.customPanel);
    readNumU(root, "customText", cfg.customText);
    readNumU(root, "customAccent", cfg.customAccent);
    readNumU(root, "customButton", cfg.customButton);
    readNum(root, "customAlpha", cfg.customAlpha);
    readNumU(root, "customGlow", cfg.customGlow);
    readBool(root, "glowEnabled", cfg.glowEnabled);
    readBool(root, "customDarkIcons", cfg.customDarkIcons);
    readBool(root, "autoStart", cfg.autoStart);
    readBool(root, "saveLogs", cfg.saveLogs);
    readBool(root, "telemetryEnabled", cfg.telemetryEnabled);
    readBool(root, "closeOnLaunch", cfg.closeOnLaunch);
    readNum(root, "downloadThreads", cfg.downloadThreads);
    if (cfg.downloadThreads < 1) cfg.downloadThreads = 1;
    if (cfg.downloadThreads > 12) cfg.downloadThreads = 12;
    if (root.has("installHistory") && root.at("installHistory").type() == Value::Type::Array) {
        const Value& arr = root.at("installHistory");
        for (std::size_t i = 0; i < arr.size(); ++i) {
            if (arr.at(i).type() == Value::Type::String) cfg.installHistory.push_back(arr.at(i).asString());
        }
    }
    if (root.has("accounts") && root.at("accounts").type() == Value::Type::Array) {
        const Value& arr = root.at("accounts");
        for (std::size_t i = 0; i < arr.size(); ++i) {
            const Value& item = arr.at(i);
            if (item.type() != Value::Type::Object) continue;
            Account account;
            readStr(item, "name", account.name);
            readStr(item, "uuid", account.uuid);
            readStr(item, "accessToken", account.accessToken);
            readStr(item, "type", account.type);
            cfg.accounts.push_back(std::move(account));
        }
    }
    return cfg;
}

void saveLauncherConfig(const LauncherConfig& cfg) {
    std::string out = "{\n";
    out += "  \"javaPath\": " + jsonEscape(cfg.javaPath) + ",\n";
    out += "  \"checkUpdatesOnStart\": ";
    out += cfg.checkUpdatesOnStart ? "true" : "false";
    out += ",\n  \"showSnapshots\": ";
    out += cfg.showSnapshots ? "true" : "false";
    out += ",\n  \"showBeta\": ";
    out += cfg.showBeta ? "true" : "false";
    out += ",\n  \"showAlpha\": ";
    out += cfg.showAlpha ? "true" : "false";
    out += ",\n  \"accounts\": [";
    for (std::size_t i = 0; i < cfg.accounts.size(); ++i) {
        if (i > 0) out += ",";
        const Account& account = cfg.accounts[i];
        out += "\n    {\"name\": " + jsonEscape(account.name) +
               ", \"uuid\": " + jsonEscape(account.uuid) +
               ", \"accessToken\": " + jsonEscape(account.accessToken) +
               ", \"type\": " + jsonEscape(account.type) + "}";
    }
    out += "],\n  \"activeAccount\": " + std::to_string(cfg.activeAccount) + ",\n";
    out += "  \"theme\": " + jsonEscape(cfg.theme) + ",\n";
    out += "  \"language\": " + jsonEscape(cfg.language) + ",\n";
    out += "  \"customBg\": " + std::to_string(cfg.customBg) + ",\n";
    out += "  \"customPanel\": " + std::to_string(cfg.customPanel) + ",\n";
    out += "  \"customText\": " + std::to_string(cfg.customText) + ",\n";
    out += "  \"customAccent\": " + std::to_string(cfg.customAccent) + ",\n";
    out += "  \"customButton\": " + std::to_string(cfg.customButton) + ",\n";
    out += "  \"customGlow\": " + std::to_string(cfg.customGlow) + ",\n";
    out += "  \"glowEnabled\": ";
    out += cfg.glowEnabled ? "true" : "false";
    out += ",\n  \"customDarkIcons\": ";
    out += cfg.customDarkIcons ? "true" : "false";
    out += ",\n  \"installHistory\": [";
    for (size_t i=0;i<cfg.installHistory.size();++i) { if(i) out+=","; out+= jsonEscape(cfg.installHistory[i]); }
    out += "],\n  \"autoStart\": ";
    out += cfg.autoStart ? "true" : "false";
    out += ",\n  \"customAlpha\": " + std::to_string(cfg.customAlpha) + ",\n";
    out += "  \"saveLogs\": ";
    out += cfg.saveLogs ? "true" : "false";
    out += ",\n  \"telemetryEnabled\": ";
    out += cfg.telemetryEnabled ? "true" : "false";
    out += ",\n  \"closeOnLaunch\": ";
    out += cfg.closeOnLaunch ? "true" : "false";
    out += ",\n  \"downloadThreads\": " + std::to_string(cfg.downloadThreads);
    out += "\n}\n";
    createDirs(kickxDir());
    writeFileAtomic(launcherConfigPath(), out);
}

std::vector<InstanceCfg> listInstances() {
    std::lock_guard<std::mutex> lk(g_instanceCache.mutex);
    std::wstring dir = instancesDir();
    bool needsRescan = (g_instanceCache.lastDir != dir);
    if (!needsRescan) {
        WIN32_FIND_DATAW fd;
        std::wstring pattern = joinPath(dir, L"*");
        HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
        if (hFind == INVALID_HANDLE_VALUE) {
            needsRescan = true;
        } else {
            FILETIME latestWrite = fd.ftLastWriteTime;
            do {
                if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                    if (std::wstring(fd.cFileName) != L"." && std::wstring(fd.cFileName) != L"..") {
                        std::wstring instDir = joinPath(dir, fd.cFileName);
                        WIN32_FIND_DATAW fd2;
                        HANDLE h2 = FindFirstFileW(joinPath(instDir, L"*").c_str(), &fd2);
                        if (h2 != INVALID_HANDLE_VALUE) {
                            do {
                                if (CompareFileTime(&fd2.ftLastWriteTime, &latestWrite) > 0)
                                    latestWrite = fd2.ftLastWriteTime;
                            } while (FindNextFileW(h2, &fd2));
                            FindClose(h2);
                        }
                    }
                }
            } while (FindNextFileW(hFind, &fd));
            FindClose(hFind);
            auto now = std::chrono::steady_clock::now();
            auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - g_instanceCache.lastScan).count();
            if (elapsed < 2000 && !needsRescan) {
                return g_instanceCache.instances;
            }
            FILETIME cacheFt;
            auto cacheTp = g_instanceCache.lastScan.time_since_epoch();
            auto cacheMs = std::chrono::duration_cast<std::chrono::milliseconds>(cacheTp).count();
            LARGE_INTEGER li; li.QuadPart = cacheMs * 10000LL + 116444736000000000LL;
            cacheFt.dwLowDateTime = li.LowPart;
            cacheFt.dwHighDateTime = li.HighPart;
            if (CompareFileTime(&latestWrite, &cacheFt) <= 0) {
                return g_instanceCache.instances;
            }
        }
    }
    std::vector<InstanceCfg> out;
    WIN32_FIND_DATAW fd;
    std::wstring pattern = joinPath(dir, L"*");
    HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) continue;
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring instDir = joinPath(dir, name);
            if (!fileExists(joinPath(instDir, L"instance.cfg"))) continue;
            InstanceCfg cfg = loadInstance(instDir);
            if (!cfg.name.empty()) out.push_back(std::move(cfg));
        } while (FindNextFileW(hFind, &fd));
        FindClose(hFind);
    }
    g_instanceCache.instances = out;
    g_instanceCache.lastScan = std::chrono::steady_clock::now();
    g_instanceCache.lastDir = dir;
    return g_instanceCache.instances;
}

InstanceCfg loadInstance(const std::wstring& dir) {
    InstanceCfg cfg;
    std::wstring base = dir;
    while (!base.empty() && (base.back() == L'\\' || base.back() == L'/')) base.pop_back();
    std::size_t sep = base.find_last_of(L"\\/");
    if (sep != std::wstring::npos) cfg.name = toUtf8(base.substr(sep + 1));
    else cfg.name = toUtf8(base);
    std::string text;
    if (!readFile(joinPath(dir, L"instance.cfg"), text)) return cfg;
    std::size_t pos = 0;
    while (pos < text.size()) {
        std::size_t nl = text.find('\n', pos);
        std::string line;
        if (nl == std::string::npos) {
            line = text.substr(pos);
            pos = text.size();
        } else {
            line = text.substr(pos, nl - pos);
            pos = nl + 1;
        }
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty() || line[0] == '#') continue;
        std::size_t eq = line.find('=');
        if (eq == std::string::npos) continue;
        std::string key = line.substr(0, eq);
        std::string value = line.substr(eq + 1);
        if (key == "name") cfg.name = value;
        else if (key == "mc_version") cfg.mcVersion = value;
        else if (key == "loader") cfg.loader = value;
        else if (key == "jvm_args") cfg.jvmArgs = value;
        else if (key == "notes") {
            std::string v = value;
            std::string out;
            for (std::size_t i = 0; i < v.size(); ++i) {
                if (v[i] == '\\' && i + 1 < v.size() && v[i + 1] == 'n') { out += '\n'; ++i; }
                else out += v[i];
            }
            cfg.notes = out;
        }
        else if (key == "java_path") cfg.javaPath = value;
        else if (key == "loader_version") cfg.loaderVersion = value;
        else if (key == "use_bundled_java") cfg.useBundledJava = (value == "true" || value == "1");
        else if (key == "offline_mode") cfg.offlineMode = (value == "true" || value == "1");
        else if (key == "asset_index_id") cfg.assetIndexId = value;
        else if (key == "ram_mb") {
            try {
                cfg.ramMb = std::stoi(value);
            } catch (...) {
            }
        }
    }
    return cfg;
}

void saveInstance(const std::wstring& dir, const InstanceCfg& cfg) {
    createDirs(dir);
    std::string notes = cfg.notes;
    std::string notesEscaped;
    notesEscaped.reserve(notes.size());
    for (char c : notes) {
        if (c == '\n') notesEscaped += "\\n";
        else notesEscaped += c;
    }
    std::string text;
    text += "name=" + cfg.name + "\n";
    text += "mc_version=" + cfg.mcVersion + "\n";
    text += "loader=" + cfg.loader + "\n";
    text += "ram_mb=" + std::to_string(cfg.ramMb) + "\n";
    text += "jvm_args=" + cfg.jvmArgs + "\n";
    text += "notes=" + notesEscaped + "\n";
    text += "java_path=" + cfg.javaPath + "\n";
    text += "loader_version=" + cfg.loaderVersion + "\n";
    text += "use_bundled_java=" + std::string(cfg.useBundledJava ? "true" : "false") + "\n";
    text += "offline_mode=" + std::string(cfg.offlineMode ? "true" : "false") + "\n";
    text += "asset_index_id=" + cfg.assetIndexId + "\n";
    writeFileAtomic(joinPath(dir, L"instance.cfg"), text);
}

void deleteInstance(const std::wstring& dir) {
    deleteTree(dir);
}

}