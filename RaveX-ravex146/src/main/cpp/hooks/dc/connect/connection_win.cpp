#ifdef _WIN32
#include "connection.hpp"
#include <windows.h>
#include <iostream>

class WinDiscordConnection : public DiscordConnection {
private:
    HANDLE pipeHandle = INVALID_HANDLE_VALUE;

public:
    ~WinDiscordConnection() override {
        disconnect();
    }

    bool connect() override {
        if (isOpen()) return true;

        for (int i = 0; i < 10; ++i) {
            std::string pipeName = "\\\\.\\pipe\\discord-ipc-" + std::to_string(i);
            pipeHandle = CreateFileA(
                pipeName.c_str(),
                GENERIC_READ | GENERIC_WRITE,
                0,
                nullptr,
                OPEN_EXISTING,
                0,
                nullptr
            );

            if (pipeHandle != INVALID_HANDLE_VALUE) {
                return true;
            }
        }
        return false;
    }

    void disconnect() override {
        if (pipeHandle != INVALID_HANDLE_VALUE) {
            CloseHandle(pipeHandle);
            pipeHandle = INVALID_HANDLE_VALUE;
        }
    }

    bool write(const void* data, size_t length) override {
        if (!isOpen()) return false;
        DWORD written = 0;
        BOOL ok = WriteFile(pipeHandle, data, static_cast<DWORD>(length), &written, nullptr);
        return ok && (written == length);
    }

    bool read(void* data, size_t length) override {
        if (!isOpen()) return false;
        DWORD bytesRead = 0;
        BOOL ok = ReadFile(pipeHandle, data, static_cast<DWORD>(length), &bytesRead, nullptr);
        return ok && (bytesRead == length);
    }

    bool isOpen() const override {
        return pipeHandle != INVALID_HANDLE_VALUE;
    }
};

std::unique_ptr<DiscordConnection> DiscordConnection::create() {
    return std::make_unique<WinDiscordConnection>();
}
#endif
