#include "game/include/modupdates.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <atomic>
#include <mutex>
#include <thread>

namespace ravex::game {

namespace {

std::string stripVersionFromFilename(const std::string& filename) {
    std::string name = filename;
    size_t lastDot = name.rfind('.');
    if (lastDot != std::string::npos) name = name.substr(0, lastDot);
    const std::vector<std::string> separators = {"-", "_", " "};
    for (const auto& sep : separators) {
        size_t pos = name.rfind(sep);
        while (pos != std::string::npos && pos > 0) {
            std::string candidate = name.substr(pos + sep.size());
            if (!candidate.empty() && (std::isdigit(candidate[0]) || candidate.rfind("v", 0) == 0)) {
                bool allDigitOrDot = true;
                for (char c : candidate) {
                    if (!std::isdigit(c) && c != '.' && c != '-' && c != '_') {
                        allDigitOrDot = false;
                        break;
                    }
                }
                if (allDigitOrDot && candidate.size() <= 20) {
                    name = name.substr(0, pos);
                    break;
                }
            }
            pos = name.rfind(sep, pos - 1);
        }
        if (pos != std::string::npos) break;
    }
    return name;
}

std::string extractVersionFromFilename(const std::string& filename) {
    std::string name = filename;
    size_t lastDot = name.rfind('.');
    if (lastDot != std::string::npos) name = name.substr(0, lastDot);
    const std::vector<std::string> separators = {"-", "_"};
    for (const auto& sep : separators) {
        size_t pos = name.rfind(sep);
        while (pos != std::string::npos && pos > 0) {
            std::string candidate = name.substr(pos + sep.size());
            if (!candidate.empty() && (std::isdigit(candidate[0]) || candidate.rfind("v", 0) == 0)) {
                bool allValid = true;
                for (char c : candidate) {
                    if (!std::isdigit(c) && c != '.' && c != '-' && c != '_') {
                        allValid = false;
                        break;
                    }
                }
                if (allValid && candidate.size() <= 20) return candidate;
            }
            pos = name.rfind(sep, pos - 1);
        }
    }
    return "";
}

std::string urlEncode(const std::string& value) {
    std::string result;
    result.reserve(value.size() * 3);
    for (unsigned char c : value) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~') {
            result += static_cast<char>(c);
        } else {
            char buf[4];
            snprintf(buf, sizeof(buf), "%%%02X", c);
            result += buf;
        }
    }
    return result;
}

bool searchModrinth(const std::string& query, std::string& slug, std::string& title, std::string* error) {
    std::string encoded = urlEncode(query);
    std::string url = "https://api.modrinth.com/v2/search?query=" + encoded + "&limit=1&facets=[[\"project_type:mod\"]]";
    std::string resp = net::httpGet(url, error);
    if (resp.empty()) return false;
    ravex::json::Value root = ravex::json::Value::parse(resp);
    if (root.isNull() || !root.has("hits") || root.size() == 0) return false;
    const ravex::json::Value& hits = root.at("hits");
    if (hits.size() == 0) return false;
    const ravex::json::Value& hit = hits.at(0);
    if (hit.has("slug")) slug = hit.at("slug").asString();
    if (hit.has("title")) title = hit.at("title").asString();
    return !slug.empty();
}

struct ModrinthVersionInfo {
    std::string versionNumber;
    std::string downloadUrl;
};

bool fetchModrinthLatestVersion(const std::string& projectId, const std::string& mcVersion, ModrinthVersionInfo& out, std::string* error) {
    std::string url = "https://api.modrinth.com/v2/project/" + projectId + "/version?game_versions=[\"" + mcVersion + "\"]";
    std::string resp = net::httpGet(url, error);
    if (resp.empty()) return false;
    ravex::json::Value arr = ravex::json::Value::parse(resp);
    if (arr.isNull() || arr.size() == 0) return false;
    const ravex::json::Value& ver = arr.at(0);
    if (ver.has("version_number")) out.versionNumber = ver.at("version_number").asString();
    if (ver.has("files") && ver.at("files").size() > 0) {
        const ravex::json::Value& file = ver.at("files").at(0);
        if (file.has("url")) out.downloadUrl = file.at("url").asString();
    }
    return !out.versionNumber.empty() && !out.downloadUrl.empty();
}

}

bool checkModUpdates(const std::wstring& modsDir, const std::string& mcVersion, std::vector<ModUpdateInfo>* out, std::string* error) {
    if (!fileExists(modsDir)) {
        if (error) *error = "Mods directory does not exist";
        return false;
    }
    std::vector<std::wstring> jarFiles;
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW(joinPath(modsDir, L"*.jar").c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        do {
            if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
                jarFiles.push_back(fd.cFileName);
            }
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
    if (jarFiles.empty()) return true;

    struct Job {
        std::wstring filename;
        ModUpdateInfo* result;
    };
    std::vector<Job> jobs;
    std::vector<ModUpdateInfo> results(jarFiles.size());
    for (size_t i = 0; i < jarFiles.size(); ++i) {
        results[i].filename = toUtf8(jarFiles[i]);
        jobs.push_back({jarFiles[i], &results[i]});
    }

    std::atomic<size_t> idx{0};
    std::atomic<bool> failed{false};
    std::mutex mtx;
    std::string firstErr;
    const size_t conc = 6;
    std::vector<std::thread> ths;
    for (size_t t = 0; t < conc; ++t) {
        ths.emplace_back([&]() {
            while (true) {
                size_t i = idx.fetch_add(1);
                if (i >= jobs.size()) break;
                if (failed.load()) break;
                const auto& job = jobs[i];
                std::string baseName = toUtf8(job.filename);
                std::string searchQuery = stripVersionFromFilename(baseName);
                if (searchQuery.empty()) continue;
                std::string slug, title;
                std::string err;
                if (!searchModrinth(searchQuery, slug, title, &err)) continue;
                job.result->modSlug = slug;
                job.result->modTitle = title;
            }
        });
    }
    for (auto& th : ths) th.join();

    std::vector<size_t> matched;
    for (size_t i = 0; i < results.size(); ++i) {
        if (!results[i].modSlug.empty()) matched.push_back(i);
    }
    if (matched.empty()) return true;

    std::atomic<size_t> idx2{0};
    std::atomic<bool> failed2{false};
    std::string firstErr2;
    std::vector<std::thread> ths2;
    for (size_t t = 0; t < conc; ++t) {
        ths2.emplace_back([&]() {
            while (true) {
                size_t mi = idx2.fetch_add(1);
                if (mi >= matched.size()) break;
                if (failed2.load()) break;
                size_t i = matched[mi];
                std::string err;
                ModrinthVersionInfo verInfo;
                if (!fetchModrinthLatestVersion(results[i].modSlug, mcVersion, verInfo, &err)) continue;
                std::string currentVer = extractVersionFromFilename(results[i].filename);
                if (currentVer == verInfo.versionNumber) continue;
                results[i].currentVersion = currentVer;
                results[i].latestVersion = verInfo.versionNumber;
                results[i].downloadUrl = verInfo.downloadUrl;
                std::lock_guard<std::mutex> lk(mtx);
                out->push_back(results[i]);
            }
        });
    }
    for (auto& th : ths2) th.join();
    if (failed) { if (error) *error = firstErr.empty() ? "Update check failed" : firstErr; return false; }
    return true;
}

bool downloadModUpdate(const ModUpdateInfo& info, const std::wstring& modsDir, std::string* error) {
    std::wstring destFile = joinPath(modsDir, fromUtf8(info.filename));
    std::wstring tempFile = destFile + L".update";
    if (!net::downloadFile(info.downloadUrl, tempFile, nullptr, nullptr, error)) {
        DeleteFileW(tempFile.c_str());
        return false;
    }
    if (fileExists(destFile)) {
        std::wstring backupFile = destFile + L".bak";
        MoveFileExW(destFile.c_str(), backupFile.c_str(), MOVEFILE_REPLACE_EXISTING);
    }
    if (!MoveFileExW(tempFile.c_str(), destFile.c_str(), MOVEFILE_REPLACE_EXISTING)) {
        if (error) *error = "Failed to move updated file into place";
        return false;
    }
    std::wstring backupFile = destFile + L".bak";
    DeleteFileW(backupFile.c_str());
    return true;
}

}
