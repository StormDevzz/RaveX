#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// интерфейс обновления состояния прогресса загрузки файлов
struct DownloadProgressListener {
    virtual ~DownloadProgressListener() = default;
    virtual void on_progress_step(const std::string& status_text) = 0;
};

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex
