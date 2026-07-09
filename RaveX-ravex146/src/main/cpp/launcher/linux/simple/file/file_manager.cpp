#include "include/file_manager.hpp"
#include <sys/stat.h>
#include <cstdlib>
#include <fstream>
#include <unistd.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace file {

void ensure_directory(const std::string& path) {
    std::string cmd = "mkdir -p \"" + path + "\"";
    system(cmd.c_str());
}

bool file_exists(const std::string& path) {
    struct stat buffer;
    return (stat(path.c_str(), &buffer) == 0);
}

bool create_directory(const std::string& path) {
    if (file_exists(path)) return true;
    std::string cmd = "mkdir -p \"" + path + "\"";
    return (system(cmd.c_str()) == 0);
}

bool write_file(const std::string& path, const std::string& content) {
    std::ofstream file(path);
    if (!file.is_open()) return false;
    file << content;
    file.close();
    return true;
}

std::string read_file(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) return "";
    std::string content((std::istreambuf_iterator<char>(file)),
                         std::istreambuf_iterator<char>());
    file.close();
    return content;
}

bool copy_file(const std::string& src, const std::string& dst) {
    std::string cmd = "cp \"" + src + "\" \"" + dst + "\"";
    return (system(cmd.c_str()) == 0);
}

bool remove_file(const std::string& path) {
    return (unlink(path.c_str()) == 0);
}

} 
} 
} 
} 
