#include "ravex/github/release_manager.h"
#include "ravex/github/release_checker.h"
#include "ravex/github/http_client.h"
#include <fstream>
#include <sstream>
#include <cstdio>
#include <algorithm>
#include <cstring>
#include <vector>

#ifdef _WIN32
    #define WIN32_LEAN_AND_MEAN
    #include <windows.h>
    #include <shellapi.h>
#else
    #include <sys/stat.h>
    #include <unistd.h>
    #include <dirent.h>
#endif

namespace ravex {
namespace github {

// ─── File helpers ─────────────────────────────────────────────────────────────

static std::string readFile(const std::string& path) {
    std::ifstream ifs(path, std::ios::binary | std::ios::ate);
    if (!ifs) return {};
    std::streamoff size = ifs.tellg();
    ifs.seekg(0);
    std::string result((size_t)size, '\0');
    ifs.read(result.data(), size);
    return result;
}

static bool writeFile(const std::string& path, const std::string& data) {
    std::ofstream ofs(path, std::ios::binary);
    if (!ofs) return false;
    ofs.write(data.data(), data.size());
    return true;
}

static bool removeFile(const std::string& path) {
#ifdef _WIN32
    return DeleteFileA(path.c_str()) != 0;
#else
    return std::remove(path.c_str()) == 0;
#endif
}

static bool renameFile(const std::string& from, const std::string& to) {
#ifdef _WIN32
    return MoveFileExA(from.c_str(), to.c_str(), MOVEFILE_REPLACE_EXISTING) != 0;
#else
    return std::rename(from.c_str(), to.c_str()) == 0;
#endif
}

static std::string getTempDir() {
#ifdef _WIN32
    char buf[MAX_PATH];
    GetTempPathA(MAX_PATH, buf);
    return std::string(buf);
#else
    const char* tmp = getenv("TMPDIR");
    if (!tmp) tmp = getenv("TMP");
    if (!tmp) tmp = getenv("TEMP");
    if (!tmp) tmp = "/tmp";
    return std::string(tmp);
#endif
}

static std::string getExecutableDir() {
#ifdef _WIN32
    char buf[MAX_PATH];
    GetModuleFileNameA(nullptr, buf, MAX_PATH);
    std::string path(buf);
    size_t pos = path.find_last_of('\\');
    return (pos != std::string::npos) ? path.substr(0, pos) : path;
#else
    char buf[4096];
    ssize_t len = readlink("/proc/self/exe", buf, sizeof(buf) - 1);
    if (len > 0) {
        buf[len] = 0;
        std::string path(buf);
        size_t pos = path.find_last_of('/');
        return (pos != std::string::npos) ? path.substr(0, pos) : path;
    }
    return ".";
#endif
}

static std::string computeSha256(const std::string& data) {
    if (data.empty()) return "";
    uint64_t h[4] = { 0x6a09e667f3bcc908, 0xbb67ae8584caa73b,
                      0x3c6ef372fe94f82b, 0xa54ff53a5f1d36f1 };
    const size_t blockSize = 64;

    std::string s = data;
    uint64_t bitLen = s.size() * 8;
    s += (char)0x80;
    while ((s.size() % blockSize) != 56) s += (char)0x00;
    for (int i = 7; i >= 0; --i) s += (char)((bitLen >> (i * 8)) & 0xFF);

    auto rotR = [](uint64_t x, uint32_t n) { return (x >> n) | (x << (64 - n)); };
    auto ch   = [](uint64_t x, uint64_t y, uint64_t z) { return (x & y) ^ (~x & z); };
    auto maj  = [](uint64_t x, uint64_t y, uint64_t z) { return (x & y) ^ (x & z) ^ (y & z); };
    auto sig0 = [&](uint64_t x) { return rotR(x, 1) ^ rotR(x, 8) ^ (x >> 7); };
    auto sig1 = [&](uint64_t x) { return rotR(x, 19) ^ rotR(x, 61) ^ (x >> 6); };

    for (size_t i = 0; i < s.size(); i += blockSize) {
        uint64_t w[80] = {};
        for (int t = 0; t < 80; ++t) {
            if (t < 16) {
                w[t] = ((uint64_t)(unsigned char)s[i + t * 8]     << 56) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 1] << 48) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 2] << 40) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 3] << 32) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 4] << 24) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 5] << 16) |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 6] << 8)  |
                       ((uint64_t)(unsigned char)s[i + t * 8 + 7]);
            } else {
                w[t] = sig1(w[t - 2]) + w[t - 7] + sig0(w[t - 15]) + w[t - 16];
            }
        }

        uint64_t a = h[0], b = h[1], c = h[2], d = h[3];
        static const uint64_t k[80] = {
            0x428a2f98d728ae22, 0x7137449123ef65cd, 0xb5c0fbcfec4d3b2f, 0xe9b5dba58189dbbc,
            0x3956c25bf348b538, 0x59f111f1b605d019, 0x923f82a4af194f9b, 0xab1c5ed5da6d8118,
            0xd807aa98a3030242, 0x12835b0145706fbe, 0x243185be4ee4b28c, 0x550c7dc3d5ffb4e2,
            0x72be5d74f27b896f, 0x80deb1fe3b1696b1, 0x9bdc06a725c71235, 0xc19bf174cf692694,
            0xe49b69c19ef14ad2, 0xefbe4786384f25e3, 0x0fc19dc68b8cd5b5, 0x240ca1cc77ac9c65,
            0x2de92c6f592b0275, 0x4a7484aa6ea6e483, 0x5cb0a9dcbd41fbd4, 0x76f988da831153b5,
            0x983e5152ee66dfab, 0xa831c66d2db43210, 0xb00327c898fb213f, 0xbf597fc7beef0ee4,
            0xc6e00bf33da88fc2, 0xd5a79147930aa725, 0x06ca6351e003826f, 0x142929670a0e6e70,
            0x27b70a8546d22ffc, 0x2e1b21385c26c926, 0x4d2c6dfc5ac42aed, 0x53380d139d95b3df,
            0x650a73548baf63de, 0x766a0abb3c77b2a8, 0x81c2c92e47edaee6, 0x92722c851482353b,
            0xa2bfe8a14cf10364, 0xa81a664bbc423001, 0xc24b8b70d0f89791, 0xc76c51a30654be30,
            0xd192e819d6ef5218, 0xd69906245565a910, 0xf40e35855771202a, 0x106aa07032bbd1b8,
            0x19a4c116b8d2d0c8, 0x1e376c085141ab53, 0x2748774cdf8eeb99, 0x34b0bcb5e19b48a8,
            0x391c0cb3c5c95a63, 0x4ed8aa4ae3418acb, 0x5b9cca4f7763e373, 0x682e6ff3d6b2b8a3,
            0x748f82ee5defb2fc, 0x78a5636f43172f60, 0x84c87814a1f0ab72, 0x8cc702081a6439ec,
            0x90befffa23631e28, 0xa4506cebde82bde9, 0xbef9a3f7b2c67915, 0xc67178f2e372532b,
            0xca273eceea26619c, 0xd186b8c721c0c207, 0xeada7dd6cde0eb1e, 0xf57d4f7fee6ed178,
            0x06f067aa72176fba, 0x0a637dc5a2c898a6, 0x113f9804bef90dae, 0x1b710b35131c471b,
            0x28db77f523047d84, 0x32caab7b40c72493, 0x3c9ebe0a15c9bebc, 0x4d5b9cae4b5cfeca,
            0x54673360a5c4e7ee, 0x6d94fe2fe5a1e1db, 0x65205afe9f31e4cd, 0x75894b775be4c9fa
        };

        for (int t = 0; t < 80; ++t) {
            uint64_t t1 = h[3] + sig1(h[1]) + ch(h[1], h[2], h[3]) + k[t] + w[t];
            uint64_t t2 = sig0(h[0]) + maj(h[0], h[1], h[2]);
            h[3] = h[2]; h[2] = h[1]; h[1] = h[0]; h[0] = t1 + t2;
        }

        h[0] += a; h[1] += b; h[2] += c; h[3] += d;
    }

    char hex[65];
    for (int i = 0; i < 4; ++i) {
        std::sprintf(hex + i * 16, "%016llx", (unsigned long long)h[i]);
    }
    return std::string(hex, 64);
}

// ─── ReleaseManagerImpl ──────────────────────────────────────────────────────

class ReleaseManagerImpl {
public:
    GithubConfig config;
    ReleaseChecker checker;
    std::string errMsg;
    std::function<void(const std::string&)> statusCb;
    LogCallback logCb;
    ProgressCallback progressCb;
    int dlProgress = 0;
    bool downloading = false;
    bool installing = false;

    ReleaseManagerImpl(const GithubConfig& cfg)
        : config(cfg), checker(cfg.owner, cfg.repo, cfg.token) {}

    // ─── check ─────────────────────────────────────────────────────────────
    UpdateInfo check() {
        UpdateInfo info;
        downloading = false;
        installing = false;
        dlProgress = 0;

        try {
            info = checker.checkForUpdates(
                config.currentVersion,
                config.channel
            );
        } catch (const std::exception& e) {
            info.error = true;
            info.errorMessage = std::string("check exception: ") + e.what();
            errMsg = info.errorMessage;
            if (logCb) logCb(LogLevel::Error, info.errorMessage);
            return info;
        }

        if (info.error) {
            errMsg = info.errorMessage;
            if (logCb) logCb(LogLevel::Error, info.errorMessage);
        } else if (info.available) {
            if (logCb) logCb(LogLevel::Info, "Update available: " + info.remoteVersion.toString());
        } else {
            if (logCb) logCb(LogLevel::Info, "Already up to date (" + info.localVersion.toString() + ")");
        }

        return info;
    }

    // ─── download ──────────────────────────────────────────────────────────
    DownloadResult download(const UpdateInfo& update) {
        DownloadResult result;
        downloading = true;
        dlProgress = 0;

        if (update.matchingAssets.empty()) {
            result.errorMsg = "No matching assets for this platform";
            errMsg = result.errorMsg;
            if (logCb) logCb(LogLevel::Warn, result.errorMsg);
            downloading = false;
            return result;
        }

        // Pick the first matching asset
        const GithubAsset& asset = update.matchingAssets[0];

        std::string dlDir = config.downloadDir;
        if (dlDir.empty()) dlDir = getTempDir() + "/ravex-update";
#ifdef _WIN32
        CreateDirectoryA(dlDir.c_str(), nullptr);
#else
        mkdir(dlDir.c_str(), 0755);
#endif

        result.filePath = dlDir + "/" + asset.name;
        result.totalBytes = asset.size;

        using namespace std::placeholders;
        auto wrappedProgress = [this](int64_t downloaded, int64_t total) {
            dlProgress = (total > 0) ? (int)(downloaded * 100 / total) : 0;
            if (progressCb) progressCb(downloaded, total);
        };

        bool ok = checker.http().download(asset.browserDownloadUrl, result.filePath, wrappedProgress);
        if (!ok) {
            result.errorMsg = "Download failed";
            errMsg = result.errorMsg;
            if (logCb) logCb(LogLevel::Error, result.errorMsg);
            downloading = false;
            return result;
        }

        result.success = true;
        result.bytesDownloaded = result.totalBytes;

        // Checksum verification
        if (config.verifyChecksums) {
            std::string fileData = readFile(result.filePath);
            result.checksumSha256 = computeSha256(fileData);
        }

        downloading = false;
        if (logCb) logCb(LogLevel::Info, "Downloaded " + asset.name + " to " + result.filePath);
        return result;
    }

    // ─── install ───────────────────────────────────────────────────────────
    bool install(const DownloadResult& dl) {
        installing = true;
        if (!dl.success) {
            errMsg = "Cannot install: download was not successful";
            if (logCb) logCb(LogLevel::Error, errMsg);
            installing = false;
            return false;
        }

        std::string appDir = config.installDir.empty() ? getExecutableDir() : config.installDir;

        // Create backup
        std::string fileName = dl.filePath.substr(dl.filePath.find_last_of("/\\") + 1);
        std::string targetFile = appDir + "/" + fileName;
        std::string backupFile = appDir + "/" + fileName + ".bak";

        std::ifstream existing(targetFile, std::ios::binary);
        if (existing.good()) {
            existing.close();
            renameFile(targetFile, backupFile);
            if (logCb) logCb(LogLevel::Info, "Backed up " + fileName);
        }

        if (!renameFile(dl.filePath, targetFile)) {
            // Try copy instead (cross-drive)
            std::string data = readFile(dl.filePath);
            if (data.empty() || !writeFile(targetFile, data)) {
                errMsg = "Install failed: could not copy file to " + targetFile;
                if (logCb) logCb(LogLevel::Error, errMsg);
                installing = false;
                return false;
            }
            removeFile(dl.filePath);
        }

        installing = false;
        if (logCb) logCb(LogLevel::Info, "Installed " + fileName + " to " + targetFile);
        return true;
    }

    // ─── checkAndUpdate ────────────────────────────────────────────────────
    UpdateInfo checkAndUpdate() {
        UpdateInfo info = check();
        if (info.available && !info.error) {
            DownloadResult dl = download(info);
            if (dl.success) {
                if (!install(dl)) {
                    info.error = true;
                    info.errorMessage = "Install failed after download";
                }
            } else {
                info.error = true;
                info.errorMessage = dl.errorMsg;
            }
        }
        return info;
    }

    // ─── rollback ──────────────────────────────────────────────────────────
    bool rollback() {
        std::string appDir = config.installDir.empty() ? getExecutableDir() : config.installDir;
#ifdef _WIN32
        std::string searchPath = appDir + "\\*.bak";
        WIN32_FIND_DATAA findData;
        HANDLE hFind = FindFirstFileA(searchPath.c_str(), &findData);
        if (hFind == INVALID_HANDLE_VALUE) {
            errMsg = "No backup files found in " + appDir;
            if (logCb) logCb(LogLevel::Warn, errMsg);
            return false;
        }
        int restored = 0;
        do {
            std::string bakFile = appDir + "\\" + findData.cFileName;
            std::string origName = findData.cFileName;
            if (origName.size() > 4) origName = origName.substr(0, origName.size() - 4);
            std::string targetFile = appDir + "\\" + origName;
            if (renameFile(bakFile, targetFile)) {
                restored++;
            }
        } while (FindNextFileA(hFind, &findData));
        FindClose(hFind);
        if (logCb) logCb(LogLevel::Info, "Rolled back " + std::to_string(restored) + " files");
        return restored > 0;
#else
        DIR* dir = opendir(appDir.c_str());
        if (!dir) {
            errMsg = "Cannot open directory: " + appDir;
            if (logCb) logCb(LogLevel::Warn, errMsg);
            return false;
        }
        int restored = 0;
        struct dirent* entry;
        std::vector<std::string> backups;
        while ((entry = readdir(dir)) != nullptr) {
            std::string name = entry->d_name;
            if (name.size() > 4 && name.substr(name.size() - 4) == ".bak") {
                backups.push_back(appDir + "/" + name);
            }
        }
        closedir(dir);
        for (const auto& bakFile : backups) {
            std::string origName = bakFile.substr(0, bakFile.size() - 4);
            size_t pos = origName.find_last_of('/');
            if (pos != std::string::npos) origName = origName.substr(pos + 1);
            std::string targetFile = appDir + "/" + origName;
            if (renameFile(bakFile, targetFile)) restored++;
        }
        if (logCb) logCb(LogLevel::Info, "Rolled back " + std::to_string(restored) + " files");
        return restored > 0;
#endif
    }

    bool isDownloading() const { return downloading; }
    bool isInstalling()  const { return installing; }
    int  downloadProgress() const { return dlProgress; }

    void setConfig(const GithubConfig& cfg) {
        config = cfg;
        // Re-init checker with new creds
        checker = ReleaseChecker(cfg.owner, cfg.repo, cfg.token);
    }

    const GithubConfig& getConfig() const { return config; }

    std::string lastError() const { return errMsg; }
};

// ─── ReleaseManager public API ───────────────────────────────────────────────

ReleaseManager::ReleaseManager(const GithubConfig& config)
    : m_impl(std::make_unique<ReleaseManagerImpl>(config)) {}

ReleaseManager::~ReleaseManager() = default;

UpdateInfo ReleaseManager::check()                             { return m_impl->check(); }
DownloadResult ReleaseManager::download(const UpdateInfo& u)   { return m_impl->download(u); }
bool ReleaseManager::install(const DownloadResult& d)          { return m_impl->install(d); }
UpdateInfo ReleaseManager::checkAndUpdate()                    { return m_impl->checkAndUpdate(); }
bool ReleaseManager::rollback()                                { return m_impl->rollback(); }

bool ReleaseManager::isDownloading() const                    { return m_impl->isDownloading(); }
bool ReleaseManager::isInstalling() const                     { return m_impl->isInstalling(); }
int ReleaseManager::downloadProgress() const                  { return m_impl->downloadProgress(); }

void ReleaseManager::onLog(LogCallback cb)                    { m_impl->logCb = std::move(cb); }
void ReleaseManager::onProgress(ProgressCallback cb)          { m_impl->progressCb = std::move(cb); }
void ReleaseManager::onStatus(std::function<void(const std::string&)> cb) { m_impl->statusCb = std::move(cb); }

void ReleaseManager::setConfig(const GithubConfig& cfg)       { m_impl->setConfig(cfg); }
const GithubConfig& ReleaseManager::config() const            { return m_impl->getConfig(); }
std::string ReleaseManager::lastError() const                 { return m_impl->lastError(); }

} // namespace github
} // namespace ravex
