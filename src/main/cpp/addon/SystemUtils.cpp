#include "include/SystemUtils.hpp"
#include <fstream>
#include <algorithm>

#ifdef _WIN32
#include <windows.h>
#else
#include <dirent.h>
#endif

namespace ravex {
namespace addon {

std::vector<std::string> SystemUtils::listFiles(const std::string& dir, const std::string& extension) {
    std::vector<std::string> results;
#ifdef _WIN32
    std::string pattern = dir + "\\*" + extension;
    WIN32_FIND_DATAA ffd;
    HANDLE hFind = FindFirstFileA(pattern.c_str(), &ffd);
    if (hFind != INVALID_HANDLE_VALUE) {
        do {
            if (!(ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
                results.push_back(dir + "\\" + ffd.cFileName);
            }
        } while (FindNextFileA(hFind, &ffd) != 0);
        FindClose(hFind);
    }
#else
    DIR* d = opendir(dir.c_str());
    if (d) {
        struct dirent* entry;
        while ((entry = readdir(d))) {
            std::string name(entry->d_name);
            if (name.size() >= extension.size() &&
                name.compare(name.size() - extension.size(), extension.size(), extension) == 0) {
                results.push_back(dir + "/" + name);
            }
        }
        closedir(d);
    }
#endif
    return results;
}

bool SystemUtils::fileExists(const std::string& path) {
#ifdef _WIN32
    DWORD attrs = GetFileAttributesA(path.c_str());
    return attrs != INVALID_FILE_ATTRIBUTES && !(attrs & FILE_ATTRIBUTE_DIRECTORY);
#else
    std::ifstream f(path.c_str());
    return f.good();
#endif
}

}
}
