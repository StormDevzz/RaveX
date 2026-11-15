#include "checks.hpp"
#include <cstdio>
#include <cstring>

#ifdef _WIN32
#include <windows.h>
#include <sstream>
#include <tlhelp32.h>

typedef LONG(WINAPI* fnRtlGetVersion)(PRTL_OSVERSIONINFOW);

namespace ravex {
namespace checks {
    std::string getOSInfo() {
        RTL_OSVERSIONINFOW osvi = { 0 };
        osvi.dwOSVersionInfoSize = sizeof(osvi);
        HMODULE hNtdll = GetModuleHandleA("ntdll.dll");
        if (hNtdll) {
            auto pRtlGetVersion = (fnRtlGetVersion)GetProcAddress(hNtdll, "RtlGetVersion");
            if (pRtlGetVersion) {
                pRtlGetVersion(&osvi);
                std::ostringstream ss;
                ss << "Windows (kernel " << osvi.dwMajorVersion << "." << osvi.dwMinorVersion << "." << osvi.dwBuildNumber << ")";
                return ss.str();
            }
        }
        return "Windows";
    }

    bool validateHandshakeResponse(const std::string& responseJson, uint32_t opcode) {

        if (opcode != 1) {
            std::printf("[Checks] Handshake validation failed: expected opcode 1, got %u\n", opcode);
            return false;
        }
        if (responseJson.find("\"cmd\":\"DISPATCH\"") == std::string::npos) {
            std::printf("[Checks] Handshake validation failed: missing cmd DISPATCH\n");
            return false;
        }
        if (responseJson.find("\"evt\":\"READY\"") == std::string::npos) {
            std::printf("[Checks] Handshake validation failed: missing evt READY\n");
            return false;
        }
        std::printf("[Checks] Handshake response validated successfully.\n");
        return true;
    }

    bool isDiscordRunning() {
#ifdef _WIN32
        HANDLE hSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
        if (hSnap == INVALID_HANDLE_VALUE) return false;
        PROCESSENTRY32W pe;
        pe.dwSize = sizeof(pe);
        bool found = false;
        if (Process32FirstW(hSnap, &pe)) {
            do {
                const wchar_t* name = pe.szExeFile;
                if (_wcsicmp(name, L"Discord.exe") == 0 || _wcsicmp(name, L"DiscordCanary.exe") == 0 ||
                    _wcsicmp(name, L"DiscordPTB.exe") == 0 || _wcsicmp(name, L"DiscordDevelopment.exe") == 0 ||
                    _wcsicmp(name, L"Vesktop.exe") == 0 || _wcsicmp(name, L"ArmCord.exe") == 0) {
                    found = true;
                    break;
                }
            } while (Process32NextW(hSnap, &pe));
        }
        CloseHandle(hSnap);
        return found;
#else
        FILE* fp = popen("pgrep -x Discord 2>/dev/null || pgrep -x discord 2>/dev/null || pgrep -x vesktop 2>/dev/null || pgrep -x armcord 2>/dev/null", "r");
        if (!fp) return false;
        char buf[128];
        bool found = fgets(buf, sizeof(buf), fp) != nullptr;
        pclose(fp);
        return found;
#endif
    }
}
}
#else
#include <sys/utsname.h>

namespace ravex {
namespace checks {
    std::string getOSInfo() {
        struct utsname buf;
        if (uname(&buf) == 0) {
            return std::string(buf.sysname) + " (kernel " + std::string(buf.release) + ")";
        }
        return "Linux";
    }

    bool validateHandshakeResponse(const std::string& responseJson, uint32_t opcode) {
        if (opcode != 1) {
            std::printf("[Checks] Handshake validation failed: expected opcode 1, got %u\n", opcode);
            return false;
        }
        if (responseJson.find("\"cmd\":\"DISPATCH\"") == std::string::npos) {
            std::printf("[Checks] Handshake validation failed: missing cmd DISPATCH\n");
            return false;
        }
        if (responseJson.find("\"evt\":\"READY\"") == std::string::npos) {
            std::printf("[Checks] Handshake validation failed: missing evt READY\n");
            return false;
        }
        std::printf("[Checks] Handshake response validated successfully.\n");
        return true;
    }
}
}
#endif
