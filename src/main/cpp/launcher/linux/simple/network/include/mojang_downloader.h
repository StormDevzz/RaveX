#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// скачать оригинальный client.jar
bool download_client_jar(const std::string& url, const std::string& dest_path);

// скачать файлы библиотек
bool download_library_file(const std::string& url, const std::string& dest_path);

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex
