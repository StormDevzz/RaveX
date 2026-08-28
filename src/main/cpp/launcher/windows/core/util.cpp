#include "core/include/util.hpp"
#include <windows.h>
#include <string>
#include <vector>

namespace ravex {

std::string toUtf8(const std::wstring& text) {
    if (text.empty()) return std::string();
    int size = WideCharToMultiByte(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), nullptr, 0, nullptr, nullptr);
    if (size <= 0) return std::string();
    std::string out(static_cast<size_t>(size), '\0');
    WideCharToMultiByte(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), out.data(), size, nullptr, nullptr);
    return out;
}

std::wstring fromUtf8(const std::string& text) {
    if (text.empty()) return std::wstring();
    int size = MultiByteToWideChar(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), nullptr, 0);
    if (size <= 0) return std::wstring();
    std::wstring out(static_cast<size_t>(size), L'\0');
    MultiByteToWideChar(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), out.data(), size);
    return out;
}

bool writeFileAtomic(const std::wstring& path, const std::string& data) {
    std::wstring tmp = path + L".tmp";
    HANDLE hFile = CreateFileW(tmp.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hFile == INVALID_HANDLE_VALUE) return false;
    DWORD written = 0;
    bool ok = data.empty() || (WriteFile(hFile, data.data(), static_cast<DWORD>(data.size()), &written, nullptr) && written == data.size());
    CloseHandle(hFile);
    if (!ok) {
        DeleteFileW(tmp.c_str());
        return false;
    }
    if (!MoveFileExW(tmp.c_str(), path.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
        DeleteFileW(tmp.c_str());
        return false;
    }
    return true;
}

bool readFile(const std::wstring& path, std::string& out) {
    HANDLE hFile = CreateFileW(path.c_str(), GENERIC_READ, FILE_SHARE_READ, nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hFile == INVALID_HANDLE_VALUE) return false;
    LARGE_INTEGER size;
    if (!GetFileSizeEx(hFile, &size)) {
        CloseHandle(hFile);
        return false;
    }
    out.clear();
    if (size.QuadPart > 0) {
        if (size.QuadPart > 64 * 1024 * 1024) {
            CloseHandle(hFile);
            return false;
        }
        out.resize(static_cast<size_t>(size.QuadPart));
        DWORD read = 0;
        if (!ReadFile(hFile, out.data(), static_cast<DWORD>(size.QuadPart), &read, nullptr) || read != size.QuadPart) {
            CloseHandle(hFile);
            out.clear();
            return false;
        }
    }
    CloseHandle(hFile);
    return true;
}

bool fileExists(const std::wstring& path) {
    return GetFileAttributesW(path.c_str()) != INVALID_FILE_ATTRIBUTES;
}

bool createDirs(const std::wstring& path) {
    if (path.empty()) return false;
    std::wstring norm = path;
    for (wchar_t& c : norm) {
        if (c == L'/') c = L'\\';
    }
    while (norm.size() > 1 && norm.back() == L'\\') norm.pop_back();
    if (norm.size() >= 2 && norm[1] == L':' && norm.size() == 2) return true;
    size_t start = 0;
    if (norm.size() >= 2 && norm[1] == L':') start = 3;
    else if (norm.size() >= 2 && norm[0] == L'\\' && norm[1] == L'\\') {
        start = 2;
        size_t second = norm.find(L'\\', 2);
        if (second != std::wstring::npos) start = second + 1;
    }
    size_t i = start;
    while (i <= norm.size()) {
        if (i == norm.size() || norm[i] == L'\\') {
            if (i > start) {
                std::wstring part = norm.substr(0, i);
                if (!CreateDirectoryW(part.c_str(), nullptr)) {
                    if (GetLastError() != ERROR_ALREADY_EXISTS) return false;
                }
            }
            if (i == norm.size()) break;
        }
        ++i;
    }
    return true;
}

std::wstring joinPath(const std::wstring& a, const std::wstring& b) {
    if (a.empty()) return b;
    if (b.empty()) return a;
    wchar_t last = a.back();
    if (last == L'\\' || last == L'/') return a + b;
    return a + L"\\" + b;
}

}