#pragma once

#include "github_types.h"
#include <string>
#include <functional>
#include <memory>
#include <atomic>

namespace ravex {
namespace github {






class ReleaseManager {
public:
    explicit ReleaseManager(const GithubConfig& config);
    ~ReleaseManager();

    
    UpdateInfo      check();
    DownloadResult  download(const UpdateInfo& update);
    bool            install(const DownloadResult& download);
    UpdateInfo      checkAndUpdate();

    
    bool rollback();

    
    bool isDownloading() const;
    bool isInstalling() const;
    int  downloadProgress() const; 

    
    void onLog(LogCallback cb);
    void onProgress(ProgressCallback cb);
    void onStatus(std::function<void(const std::string&)> cb);

    
    void setConfig(const GithubConfig& cfg);
    const GithubConfig& config() const;

    
    std::string lastError() const;

private:
    std::unique_ptr<class ReleaseManagerImpl> m_impl;
};

} 
} 
