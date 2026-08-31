#include "core/include/config.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include <windows.h>
#include <cstdio>
#include <utility>

namespace ravex {

namespace {

struct Json {
    enum class Kind { Null, Bool, Number, String, Array, Object };
    Kind kind = Kind::Null;
    bool boolValue = false;
    double numValue = 0.0;
    std::string strValue;
    std::vector<Json> items;
    std::vector<std::pair<std::string, Json>> members;

    const Json* find(const std::string& key) const {
        if (kind != Kind::Object) return nullptr;
        for (const auto& kv : members) {
            if (kv.first == key) return &kv.second;
        }
        return nullptr;
    }
};

class JsonParser {
public:
    explicit JsonParser(const std::string& text) : text_(text) {}

    bool parse(Json& out) {
        skipWs();
        if (!parseValue(out)) return false;
        skipWs();
        return pos_ == text_.size();
    }

private:
    const std::string& text_;
    std::size_t pos_ = 0;

    void skipWs() {
        while (pos_ < text_.size()) {
            char c = text_[pos_];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
            ++pos_;
        }
    }

    bool parseValue(Json& out) {
        skipWs();
        if (pos_ >= text_.size()) return false;
        char c = text_[pos_];
        if (c == '{') return parseObject(out);
        if (c == '[') return parseArray(out);
        if (c == '"') {
            std::string s;
            if (!parseString(s)) return false;
            out.kind = Json::Kind::String;
            out.strValue = std::move(s);
            return true;
        }
        if (c == 't') {
            if (text_.compare(pos_, 4, "true") != 0) return false;
            pos_ += 4;
            out.kind = Json::Kind::Bool;
            out.boolValue = true;
            return true;
        }
        if (c == 'f') {
            if (text_.compare(pos_, 5, "false") != 0) return false;
            pos_ += 5;
            out.kind = Json::Kind::Bool;
            out.boolValue = false;
            return true;
        }
        if (c == 'n') {
            if (text_.compare(pos_, 4, "null") != 0) return false;
            pos_ += 4;
            out.kind = Json::Kind::Null;
            return true;
        }
        if (c == '-' || (c >= '0' && c <= '9')) return parseNumber(out);
        return false;
    }

    bool parseString(std::string& out) {
        if (text_[pos_] != '"') return false;
        ++pos_;
        out.clear();
        while (pos_ < text_.size()) {
            char c = text_[pos_];
            if (c == '"') {
                ++pos_;
                return true;
            }
            if (c == '\\') {
                ++pos_;
                if (pos_ >= text_.size()) return false;
                char e = text_[pos_];
                switch (e) {
                    case '"': out.push_back('"'); break;
                    case '\\': out.push_back('\\'); break;
                    case '/': out.push_back('/'); break;
                    case 'b': out.push_back('\b'); break;
                    case 'f': out.push_back('\f'); break;
                    case 'n': out.push_back('\n'); break;
                    case 'r': out.push_back('\r'); break;
                    case 't': out.push_back('\t'); break;
                    case 'u': {
                        if (pos_ + 4 >= text_.size()) return false;
                        unsigned int cp = 0;
                        for (int i = 1; i <= 4; ++i) {
                            char h = text_[pos_ + static_cast<std::size_t>(i)];
                            cp <<= 4;
                            if (h >= '0' && h <= '9') cp |= static_cast<unsigned int>(h - '0');
                            else if (h >= 'a' && h <= 'f') cp |= static_cast<unsigned int>(h - 'a' + 10);
                            else if (h >= 'A' && h <= 'F') cp |= static_cast<unsigned int>(h - 'A' + 10);
                            else return false;
                        }
                        pos_ += 4;
                        if (cp < 0x80) out.push_back(static_cast<char>(cp));
                        else if (cp < 0x800) {
                            out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        } else {
                            out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
                            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        }
                        break;
                    }
                    default:
                        return false;
                }
                ++pos_;
            } else {
                out.push_back(c);
                ++pos_;
            }
        }
        return false;
    }

    bool parseNumber(Json& out) {
        std::size_t start = pos_;
        if (pos_ < text_.size() && text_[pos_] == '-') ++pos_;
        while (pos_ < text_.size() && text_[pos_] >= '0' && text_[pos_] <= '9') ++pos_;
        if (pos_ < text_.size() && text_[pos_] == '.') {
            ++pos_;
            while (pos_ < text_.size() && text_[pos_] >= '0' && text_[pos_] <= '9') ++pos_;
        }
        if (pos_ < text_.size() && (text_[pos_] == 'e' || text_[pos_] == 'E')) {
            ++pos_;
            if (pos_ < text_.size() && (text_[pos_] == '+' || text_[pos_] == '-')) ++pos_;
            while (pos_ < text_.size() && text_[pos_] >= '0' && text_[pos_] <= '9') ++pos_;
        }
        if (pos_ == start) return false;
        out.kind = Json::Kind::Number;
        out.numValue = std::stod(text_.substr(start, pos_ - start));
        return true;
    }

    bool parseObject(Json& out) {
        ++pos_;
        out.kind = Json::Kind::Object;
        skipWs();
        if (pos_ < text_.size() && text_[pos_] == '}') {
            ++pos_;
            return true;
        }
        while (true) {
            skipWs();
            std::string key;
            if (!parseString(key)) return false;
            skipWs();
            if (pos_ >= text_.size() || text_[pos_] != ':') return false;
            ++pos_;
            Json value;
            if (!parseValue(value)) return false;
            out.members.emplace_back(std::move(key), std::move(value));
            skipWs();
            if (pos_ >= text_.size()) return false;
            if (text_[pos_] == ',') {
                ++pos_;
                continue;
            }
            if (text_[pos_] == '}') {
                ++pos_;
                return true;
            }
            return false;
        }
    }

    bool parseArray(Json& out) {
        ++pos_;
        out.kind = Json::Kind::Array;
        skipWs();
        if (pos_ < text_.size() && text_[pos_] == ']') {
            ++pos_;
            return true;
        }
        while (true) {
            Json value;
            if (!parseValue(value)) return false;
            out.items.push_back(std::move(value));
            skipWs();
            if (pos_ >= text_.size()) return false;
            if (text_[pos_] == ',') {
                ++pos_;
                continue;
            }
            if (text_[pos_] == ']') {
                ++pos_;
                return true;
            }
            return false;
        }
    }
};

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

}

LauncherConfig loadLauncherConfig() {
    LauncherConfig cfg;
    std::string text;
    if (!readFile(launcherConfigPath(), text)) return cfg;
    Json root;
    if (!JsonParser(text).parse(root)) return cfg;
    if (const Json* j = root.find("javaPath")) {
        if (const std::string* v = j->kind == Json::Kind::String ? &j->strValue : nullptr) cfg.javaPath = *v;
    }
    if (const Json* j = root.find("checkUpdatesOnStart")) {
        if (j->kind == Json::Kind::Bool) cfg.checkUpdatesOnStart = j->boolValue;
    }
    if (const Json* j = root.find("showSnapshots")) {
        if (j->kind == Json::Kind::Bool) cfg.showSnapshots = j->boolValue;
    }
    if (const Json* j = root.find("showBeta")) {
        if (j->kind == Json::Kind::Bool) cfg.showBeta = j->boolValue;
    }
    if (const Json* j = root.find("showAlpha")) {
        if (j->kind == Json::Kind::Bool) cfg.showAlpha = j->boolValue;
    }
    if (const Json* j = root.find("activeAccount")) {
        if (j->kind == Json::Kind::Number) cfg.activeAccount = static_cast<int>(j->numValue);
    }
    if (const Json* j = root.find("theme")) {
        if (j->kind == Json::Kind::String) cfg.theme = j->strValue;
    }
    if (const Json* j = root.find("language")) {
        if (j->kind == Json::Kind::String) cfg.language = j->strValue;
    }
    if (const Json* j = root.find("customBg")) {
        if (j->kind == Json::Kind::Number) cfg.customBg = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("customPanel")) {
        if (j->kind == Json::Kind::Number) cfg.customPanel = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("customText")) {
        if (j->kind == Json::Kind::Number) cfg.customText = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("customAccent")) {
        if (j->kind == Json::Kind::Number) cfg.customAccent = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("customButton")) {
        if (j->kind == Json::Kind::Number) cfg.customButton = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("customAlpha")) {
        if (j->kind == Json::Kind::Number) cfg.customAlpha = static_cast<int>(j->numValue);
    }
    if (const Json* j = root.find("customGlow")) {
        if (j->kind == Json::Kind::Number) cfg.customGlow = static_cast<uint32_t>(j->numValue);
    }
    if (const Json* j = root.find("glowEnabled")) {
        if (j->kind == Json::Kind::Bool) cfg.glowEnabled = j->boolValue;
    }
    if (const Json* j = root.find("customDarkIcons")) {
        if (j->kind == Json::Kind::Bool) cfg.customDarkIcons = j->boolValue;
    }
    if (const Json* j = root.find("saveLogs")) {
        if (j->kind == Json::Kind::Bool) cfg.saveLogs = j->boolValue;
    }
    if (const Json* j = root.find("telemetryEnabled")) {
        if (j->kind == Json::Kind::Bool) cfg.telemetryEnabled = j->boolValue;
    }
    if (const Json* j = root.find("accounts")) {
        if (j->kind == Json::Kind::Array) {
            for (const Json& item : j->items) {
                if (item.kind != Json::Kind::Object) continue;
                Account account;
                if (const Json* f = item.find("name")) {
                    if (f->kind == Json::Kind::String) account.name = f->strValue;
                }
                if (const Json* f = item.find("uuid")) {
                    if (f->kind == Json::Kind::String) account.uuid = f->strValue;
                }
                if (const Json* f = item.find("accessToken")) {
                    if (f->kind == Json::Kind::String) account.accessToken = f->strValue;
                }
                if (const Json* f = item.find("type")) {
                    if (f->kind == Json::Kind::String) account.type = f->strValue;
                }
                cfg.accounts.push_back(std::move(account));
            }
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
    out += ",\n  \"customAlpha\": " + std::to_string(cfg.customAlpha) + ",\n";
    out += "  \"saveLogs\": ";
    out += cfg.saveLogs ? "true" : "false";
    out += ",\n  \"telemetryEnabled\": ";
    out += cfg.telemetryEnabled ? "true" : "false";
    out += "\n}\n";
    createDirs(kickxDir());
    writeFileAtomic(launcherConfigPath(), out);
}

std::vector<InstanceCfg> listInstances() {
    std::vector<InstanceCfg> out;
    std::wstring dir = instancesDir();
    std::wstring pattern = joinPath(dir, L"*");
    WIN32_FIND_DATAW fd;
    HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
    if (hFind == INVALID_HANDLE_VALUE) return out;
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
    return out;
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