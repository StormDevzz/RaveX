#pragma once
#include <string>
#include <memory>

class DiscordConnection {
public:
    virtual ~DiscordConnection() = default;
    virtual bool connect() = 0;
    virtual void disconnect() = 0;
    virtual bool write(const void* data, size_t length) = 0;
    virtual bool read(void* data, size_t length) = 0;
    virtual bool isOpen() const = 0;

    static std::unique_ptr<DiscordConnection> create();
};
