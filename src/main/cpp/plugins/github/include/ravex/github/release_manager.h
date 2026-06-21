#pragma once

#include "github_types.h"
#include <string>
#include <functional>
#include <memory>
#include <atomic>

namespace ravex {
namespace github {

// ─── Automatic release management ────────────────────────────────────────────
//
// Orchestrates: check for update → download → verify checksum → install
// Supports: progress callbacks, resume download, backup rollback

class ReleaseManager {
public:
    explicit ReleaseManager(const GithubConfig& config);
    ~ReleaseManager();

    // Main workflow
    UpdateInfo      check();
    DownloadResult  download(const UpdateInfo& update);
    bool            install(const DownloadResult& download);
    UpdateInfo      checkAndUpdate();

    // Rollback to previous version
    bool rollback();

    // Status
    bool isDownloading() const;
    bool isInstalling() const;
    int  downloadProgress() const; // 0-100

    // Callbacks
    void onLog(LogCallback cb);
    void onProgress(ProgressCallback cb);
    void onStatus(std::function<void(const std::string&)> cb);

    // Config
    void setConfig(const GithubConfig& cfg);
    const GithubConfig& config() const;

    // Error handling
    std::string lastError() const;

private:
    std::unique_ptr<class ReleaseManagerImpl> m_impl;
};

} // namespace github
} // namespace ravex
