#include "coordlogger.hpp"
#include <fstream>
#include <filesystem>
#include <cstring>

bool ensureDir(const char* path) {
    try {
        return std::filesystem::create_directories(path);
    } catch (...) {
        return false;
    }
}

bool writeLog(const char* filePath, const char* content) {
    try {
        std::ofstream out(filePath, std::ios::app);
        if (!out.is_open()) return false;
        out << content;
        return true;
    } catch (...) {
        return false;
    }
}
