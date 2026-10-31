#include "core/include/util.hpp"
#include <windows.h>
#include <shldisp.h>
#include <shlobj.h>
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

namespace {

struct DirStats {
    unsigned long long files = 0;
    unsigned long long bytes = 0;
};

void collectDirStats(const std::wstring& dir, DirStats& stats) {
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW((dir + L"\\*").c_str(), &fd);
    if (h == INVALID_HANDLE_VALUE) return;
    do {
        if (wcscmp(fd.cFileName, L".") == 0 || wcscmp(fd.cFileName, L"..") == 0) continue;
        std::wstring child = joinPath(dir, fd.cFileName);
        if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            collectDirStats(child, stats);
        } else {
            ++stats.files;
            stats.bytes += (static_cast<unsigned long long>(fd.nFileSizeHigh) << 32) | fd.nFileSizeLow;
        }
    } while (FindNextFileW(h, &fd));
    FindClose(h);
}

void pumpMessages() {
    MSG msg;
    while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
}

bool extractZipWithTar(const std::wstring& zipPath, const std::wstring& destDir, std::string* error) {
    std::wstring cmd = L"tar -xf \"" + zipPath + L"\" -C \"" + destDir + L"\"";
    STARTUPINFOW si{};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    PROCESS_INFORMATION pi{};
    std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end());
    cmdBuf.push_back(L'\0');
    if (!CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, FALSE,
                        CREATE_NO_WINDOW, nullptr, nullptr, &si, &pi)) {
        return false;   // tar.exe is not present, caller falls back to the shell
    }
    DWORD wait = WaitForSingleObject(pi.hProcess, 300000);
    DWORD exitCode = 1;
    if (wait == WAIT_OBJECT_0) GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    if (wait != WAIT_OBJECT_0) {
        if (error) *error = "tar extraction timed out";
        return false;
    }
    if (exitCode != 0) {
        if (error) *error = "tar extraction failed with code " + std::to_string(exitCode);
        return false;
    }
    return true;
}

// Windows 7 has no tar.exe. The shell ZIP folder exists on every supported
// version and CopyHere extracts into a normal directory. CopyHere is
// asynchronous, so wait until the destination stops growing.
bool extractZipWithShell(const std::wstring& zipPath, const std::wstring& destDir, std::string* error) {
    HRESULT hrInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    bool comReady = SUCCEEDED(hrInit);
    if (!comReady && hrInit != RPC_E_CHANGED_MODE) {
        if (error) *error = "COM init failed";
        return false;
    }
    bool ok = false;
    IShellDispatch* shell = nullptr;
    Folder* zipFolder = nullptr;
    Folder* destFolder = nullptr;
    FolderItems* items = nullptr;
    VARIANT vZip, vDest, vItems, vOptions;
    VariantInit(&vZip); VariantInit(&vDest); VariantInit(&vItems); VariantInit(&vOptions);
    do {
        if (FAILED(CoCreateInstance(CLSID_Shell, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&shell)))) break;
        vZip.vt = VT_BSTR;
        vZip.bstrVal = SysAllocString(zipPath.c_str());
        if (!vZip.bstrVal || FAILED(shell->NameSpace(vZip, &zipFolder))) break;
        vDest.vt = VT_BSTR;
        vDest.bstrVal = SysAllocString(destDir.c_str());
        if (!vDest.bstrVal || FAILED(shell->NameSpace(vDest, &destFolder))) break;
        if (FAILED(zipFolder->Items(&items))) break;
        vItems.vt = VT_DISPATCH;
        vItems.pdispVal = items;
        vOptions.vt = VT_I4;
        vOptions.lVal = 4 | 16 | 512 | 1024;   // silent, no prompts, no error UI
        if (FAILED(destFolder->CopyHere(vItems, vOptions))) break;
        ok = true;
    } while (false);

    if (ok) {
        const ULONGLONG start = GetTickCount64();
        DirStats prev;
        int stable = 0;
        bool sawFiles = false;
        while (true) {
            pumpMessages();
            DirStats now;
            collectDirStats(destDir, now);
            if (now.files > 0) sawFiles = true;
            if (sawFiles && now.files == prev.files && now.bytes == prev.bytes) {
                if (++stable >= 15) break;   // 3 seconds without changes
            } else {
                stable = 0;
            }
            prev = now;
            ULONGLONG elapsed = GetTickCount64() - start;
            if (elapsed > 300000) {
                ok = false;
                if (error) *error = "ZIP extraction timed out";
                break;
            }
            if (!sawFiles && elapsed > 120000) {
                ok = false;
                if (error) *error = "ZIP extraction produced no files";
                break;
            }
            Sleep(200);
        }
    }

    if (destFolder) destFolder->Release();
    if (zipFolder) zipFolder->Release();
    if (shell) shell->Release();
    VariantClear(&vZip); VariantClear(&vDest); VariantClear(&vItems); VariantClear(&vOptions);
    if (comReady) CoUninitialize();
    return ok;
}

} // namespace

bool extractZip(const std::wstring& zipPath, const std::wstring& destDir, std::string* error) {
    createDirs(destDir);
    std::string tarError;
    if (extractZipWithTar(zipPath, destDir, &tarError)) return true;
    // Partial output from a failed tar run is harmless: the shell overwrites
    // existing files and creates the missing ones.
    return extractZipWithShell(zipPath, destDir, error);
}

std::wstring windowsVersionName() {
    OSVERSIONINFOEXW os{};
    os.dwOSVersionInfoSize = sizeof(os);
    using RtlGetVersionFn = LONG(WINAPI*)(OSVERSIONINFOEXW*);
    if (HMODULE ntdll = GetModuleHandleW(L"ntdll.dll")) {
        auto fn = reinterpret_cast<RtlGetVersionFn>(GetProcAddress(ntdll, "RtlGetVersion"));
        if (fn) fn(&os);
    }
    if (os.dwMajorVersion == 6) {
        if (os.dwMinorVersion == 0) return L"Windows Vista";
        if (os.dwMinorVersion == 1) return L"Windows 7";
        if (os.dwMinorVersion == 2) return L"Windows 8";
        if (os.dwMinorVersion == 3) return L"Windows 8.1";
    }
    if (os.dwMajorVersion == 10) {
        return os.dwBuildNumber >= 22000 ? L"Windows 11" : L"Windows 10";
    }
    if (os.dwMajorVersion == 0) return L"Windows";
    return L"Windows " + std::to_wstring(os.dwMajorVersion) + L"." + std::to_wstring(os.dwMinorVersion);
}

bool isLegacyWindows() {
    OSVERSIONINFOEXW os{};
    os.dwOSVersionInfoSize = sizeof(os);
    using RtlGetVersionFn = LONG(WINAPI*)(OSVERSIONINFOEXW*);
    if (HMODULE ntdll = GetModuleHandleW(L"ntdll.dll")) {
        auto fn = reinterpret_cast<RtlGetVersionFn>(GetProcAddress(ntdll, "RtlGetVersion"));
        if (fn) fn(&os);
    }
    // Windows 7, 8 and 8.1 report major version 6. Adoptium no longer
    // supports them, but Liberica still ships a Java 21 runtime for them.
    return os.dwMajorVersion == 6 && os.dwMinorVersion >= 1;
}

}