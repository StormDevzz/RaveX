#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// скачать текстовое содержимое url
std::string http_get(const std::string& url);

// скачать файл и сохранить на диск
bool http_download(const std::string& url, const std::string& dest_path);

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex
