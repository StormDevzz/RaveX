#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {


struct DownloadProgressListener {
    virtual ~DownloadProgressListener() = default;
    virtual void on_progress_step(const std::string& status_text) = 0;
};

} 
} 
} 
} 
