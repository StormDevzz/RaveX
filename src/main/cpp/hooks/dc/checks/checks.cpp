#include "checks.hpp"
#include <cstdio>
#include <cstring>

#ifdef _WIN32
#include <windows.h>
#include <sstream>

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

        if (opcode != 2) {
            std::printf("[Checks] Handshake validation failed: expected opcode 2, got %u\n", opcode);
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
        if (opcode != 2) {
            std::printf("[Checks] Handshake validation failed: expected opcode 2, got %u\n", opcode);
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
