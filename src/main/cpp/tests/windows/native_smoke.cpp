#include <windows.h>

#include <filesystem>
#include <iostream>
#include <string>
#include <vector>

namespace {

std::wstring win32ErrorMessage(DWORD error) {
    wchar_t* buffer = nullptr;
    const DWORD length = FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER |
        FORMAT_MESSAGE_FROM_SYSTEM |
        FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr,
        error,
        0,
        reinterpret_cast<LPWSTR>(&buffer),
        0,
        nullptr
    );

    std::wstring result = length != 0
        ? std::wstring(buffer, length)
        : L"unknown Win32 error";

    if (buffer != nullptr) {
        LocalFree(buffer);
    }

    while (!result.empty() &&
           (result.back() == L'\r' || result.back() == L'\n')) {
        result.pop_back();
    }

    return result;
}

int fail(const std::wstring& message) {
    std::wcerr << L"[FAIL] " << message << L'\n';
    return 1;
}

}

int wmain(int argc, wchar_t** argv) {
    if (argc != 2) {
        return fail(L"Usage: ravex_native_smoke.exe <native-directory>");
    }

    const std::filesystem::path nativeDirectory(argv[1]);

    if (!std::filesystem::is_directory(nativeDirectory)) {
        return fail(L"Native directory does not exist: " +
                    nativeDirectory.wstring());
    }

    std::vector<std::filesystem::path> libraries;

    for (const auto& entry :
         std::filesystem::directory_iterator(nativeDirectory)) {
        if (entry.is_regular_file() &&
            entry.path().extension() == L".dll") {
            libraries.push_back(entry.path());
        }
    }

    if (libraries.empty()) {
        return fail(L"No DLL files found in: " +
                    nativeDirectory.wstring());
    }

    SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);

    int failures = 0;

    for (const auto& library : libraries) {
        const HMODULE handle = LoadLibraryExW(
            library.c_str(),
            nullptr,
            LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR |
            LOAD_LIBRARY_SEARCH_DEFAULT_DIRS
        );

        if (handle == nullptr) {
            ++failures;
            std::wcerr << L"[FAIL] " << library.filename().wstring()
                       << L": " << win32ErrorMessage(GetLastError())
                       << L'\n';
            continue;
        }

        std::wcout << L"[ OK ] " << library.filename().wstring() << L'\n';
        FreeLibrary(handle);
    }

    std::wcout << L"Checked DLLs: " << libraries.size() << L'\n';
    std::wcout << L"Failures: " << failures << L'\n';

    return failures == 0 ? 0 : 1;
}
