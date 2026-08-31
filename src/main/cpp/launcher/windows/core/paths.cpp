#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include <windows.h>
#include <shlobj.h>

namespace ravex {

static std::wstring profileDir() {
    static std::wstring cached;
    if (!cached.empty()) return cached;
    PWSTR known = nullptr;
    if (SHGetKnownFolderPath(FOLDERID_Profile, KF_FLAG_DEFAULT, nullptr, &known) == S_OK) {
        cached = known;
        CoTaskMemFree(known);
        return cached;
    }
    if (known) CoTaskMemFree(known);
    DWORD size = GetEnvironmentVariableW(L"USERPROFILE", nullptr, 0);
    if (size > 0) {
        cached.resize(size - 1);
        GetEnvironmentVariableW(L"USERPROFILE", cached.data(), size);
    }
    return cached;
}

std::wstring kickxDir() {
    return joinPath(profileDir(), L".kickxxx");
}

std::wstring ravexDir() {
    return joinPath(profileDir(), L".ravex");
}

std::wstring minecraftDir() {
    return joinPath(profileDir(), L".minecraft");
}

std::wstring nativesDir() {
    return joinPath(ravexDir(), L"natives");
}

std::wstring modsDir() {
    return joinPath(minecraftDir(), L"mods");
}

std::wstring instancesDir() {
    return joinPath(kickxDir(), L"instances");
}

std::wstring instanceDir(const std::wstring& name) {
    return joinPath(instancesDir(), name);
}

std::wstring javaPath() {
    DWORD size = GetEnvironmentVariableW(L"JAVA_HOME", nullptr, 0);
    if (size > 0) {
        std::wstring home;
        home.resize(size - 1);
        GetEnvironmentVariableW(L"JAVA_HOME", home.data(), size);
        std::wstring candidate = joinPath(joinPath(home, L"bin"), L"java.exe");
        if (GetFileAttributesW(candidate.c_str()) != INVALID_FILE_ATTRIBUTES) return candidate;
    }
    return L"java.exe";
}

}