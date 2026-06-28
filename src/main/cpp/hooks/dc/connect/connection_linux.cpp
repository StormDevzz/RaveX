#ifndef _WIN32
#include "connection.h"
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cstdlib>
#include <vector>
#include <cstring>
#include <cstdio>
#include <cerrno>

class LinuxDiscordConnection : public DiscordConnection {
private:
    int socketFd = -1;

    std::vector<std::string> getIpcPaths(int i) {
        std::vector<std::string> paths;
        const char* xdg = std::getenv("XDG_RUNTIME_DIR");
        if (xdg && std::strlen(xdg) > 0) {
            paths.push_back(std::string(xdg) + "/discord-ipc-" + std::to_string(i));
            paths.push_back(std::string(xdg) + "/app/com.discordapp.Discord/discord-ipc-" + std::to_string(i));
            paths.push_back(std::string(xdg) + "/.flatpak/com.discordapp.Discord/xdg-run/discord-ipc-" + std::to_string(i));
            paths.push_back(std::string(xdg) + "/snap.discord/discord-ipc-" + std::to_string(i));
        }
        const char* envVars[] = { "TMPDIR", "TMP", "TEMP" };
        for (const char* v : envVars) {
            const char* dir = std::getenv(v);
            if (dir && std::strlen(dir) > 0) {
                paths.push_back(std::string(dir) + "/discord-ipc-" + std::to_string(i));
            }
        }
        paths.push_back("/tmp/discord-ipc-" + std::to_string(i));
        return paths;
    }

public:
    ~LinuxDiscordConnection() override {
        disconnect();
    }

    bool connect() override {
        if (isOpen()) return true;

        for (int i = 0; i < 10; ++i) {
            auto paths = getIpcPaths(i);
            for (const auto& path : paths) {
                std::printf("[RichPresence C++] Trying to connect to %s...\n", path.c_str());
                socketFd = socket(AF_UNIX, SOCK_STREAM, 0);
                if (socketFd < 0) {
                    std::printf("[RichPresence C++] Failed to create socket: %d\n", errno);
                    continue;
                }

                struct sockaddr_un addr;
                std::memset(&addr, 0, sizeof(addr));
                addr.sun_family = AF_UNIX;
                std::strncpy(addr.sun_path, path.c_str(), sizeof(addr.sun_path) - 1);

                if (::connect(socketFd, (struct sockaddr*)&addr, sizeof(addr)) == 0) {
                    std::printf("[RichPresence C++] Connected successfully to %s!\n", path.c_str());
                    return true;
                } else {
                    std::printf("[RichPresence C++] Connection failed to %s: %d\n", path.c_str(), errno);
                }

                ::close(socketFd);
                socketFd = -1;
            }
        }
        return false;
    }

    void disconnect() override {
        if (socketFd >= 0) {
            ::close(socketFd);
            socketFd = -1;
        }
    }

    bool write(const void* data, size_t length) override {
        if (!isOpen()) return false;
        ssize_t sent = ::write(socketFd, data, length);
        return sent == static_cast<ssize_t>(length);
    }

    bool read(void* data, size_t length) override {
        if (!isOpen()) return false;
        size_t totalRead = 0;
        char* buf = static_cast<char*>(data);
        while (totalRead < length) {
            ssize_t readBytes = ::read(socketFd, buf + totalRead, length - totalRead);
            if (readBytes <= 0) return false;
            totalRead += readBytes;
        }
        return true;
    }

    bool isOpen() const override {
        return socketFd >= 0;
    }
};

std::unique_ptr<DiscordConnection> DiscordConnection::create() {
    return std::make_unique<LinuxDiscordConnection>();
}
#endif
