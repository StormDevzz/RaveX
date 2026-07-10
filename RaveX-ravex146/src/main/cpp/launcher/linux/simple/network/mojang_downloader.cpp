#include "include/mojang_downloader.hpp"
#include "include/http_client.hpp"
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

bool download_client_jar(const std::string& url, const std::string& dest_path) {
    struct stat buffer;
    if (stat(dest_path.c_str(), &buffer) == 0) return true;
    return http_download(url, dest_path);
}

bool download_library_file(const std::string& url, const std::string& dest_path) {
    struct stat buffer;
    if (stat(dest_path.c_str(), &buffer) == 0) return true;
    return http_download(url, dest_path);
}

}
}
}
}
