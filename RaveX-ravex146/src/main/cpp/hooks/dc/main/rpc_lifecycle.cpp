#include "discord_rpc.hpp"
#include "connection.hpp"
#include "serialization.hpp"
#include "checks.hpp"
#include <mutex>
#include <memory>
#include <vector>
#include <cstdint>
#include <thread>
#include <atomic>
#include <chrono>
#include <cstdio>

#ifdef _WIN32
#include <windows.h>
static int getMyPid() {
    return static_cast<int>(GetCurrentProcessId());
}
#else
#include <unistd.h>
static int getMyPid() {
    return static_cast<int>(getpid());
}
#endif

namespace {
    std::mutex rpcMutex;
    std::string currentClientId;
    std::unique_ptr<DiscordConnection> activeConnection;
    std::atomic<bool> isInitialized(false);
    uint32_t currentNonce = 0;

    struct PacketHeader {
        uint32_t opcode;
        uint32_t length;
    };

    bool sendFrame(uint32_t opcode, const std::string& json) {
        if (!activeConnection || !activeConnection->isOpen()) return false;
        PacketHeader header = { opcode, static_cast<uint32_t>(json.length()) };
        
        if (!activeConnection->write(&header, sizeof(header))) return false;
        if (!activeConnection->write(json.c_str(), json.length())) return false;
        return true;
    }

    bool readFrame(std::string& outData, uint32_t& outOpcode) {
        if (!activeConnection || !activeConnection->isOpen()) return false;
        PacketHeader header = { 0, 0 };
        if (!activeConnection->read(&header, sizeof(header))) return false;
        outOpcode = header.opcode;
        if (header.length > 0) {
            std::vector<char> buffer(header.length + 1, 0);
            if (!activeConnection->read(buffer.data(), header.length)) return false;
            outData.assign(buffer.data(), header.length);
        }
        return true;
    }

    bool readFrame() {
        std::string tmpData;
        uint32_t tmpOpcode = 0;
        return readFrame(tmpData, tmpOpcode);
    }

    bool establishConnection() {
        if (!activeConnection) {
            activeConnection = DiscordConnection::create();
        }
        if (activeConnection->isOpen()) return true;

        std::printf("[RichPresence C++] Attempting connection...\n");
        if (activeConnection->connect()) {
            std::string handshake = DiscordSerialization::serializeHandshake(currentClientId);
            std::printf("[RichPresence C++] Sending handshake: %s\n", handshake.c_str());
            if (sendFrame(0, handshake)) {
                std::printf("[RichPresence C++] Handshake sent, waiting for response...\n");
                std::string responseData;
                uint32_t responseOpcode = 0;
                if (readFrame(responseData, responseOpcode)) {
                    if (ravex::checks::validateHandshakeResponse(responseData, responseOpcode)) {
                        std::printf("[RichPresence C++] Handshake validated. Connection established.\n");
                        return true;
                    } else {
                        std::printf("[RichPresence C++] Handshake validation failed.\n");
                    }
                } else {
                    std::printf("[RichPresence C++] Failed to read handshake response.\n");
                }
            } else {
                std::printf("[RichPresence C++] Failed to send handshake.\n");
            }
            activeConnection->disconnect();
        } else {
            std::printf("[RichPresence C++] Active connection connect() failed.\n");
        }
        return false;
    }
}

void DiscordRPC::initialize(const std::string& clientId) {
    std::lock_guard<std::mutex> lock(rpcMutex);
    currentClientId = clientId;
    isInitialized = true;
    currentNonce = 0;
    establishConnection();
}

void DiscordRPC::shutdown() {
    std::lock_guard<std::mutex> lock(rpcMutex);
    isInitialized = false;
    if (activeConnection) {
        if (activeConnection->isOpen()) {
            std::string nonce = std::to_string(currentNonce++);
            std::string clear = DiscordSerialization::serializeClearActivity(getMyPid(), nonce);
            sendFrame(1, clear);
            activeConnection->disconnect();
        }
        activeConnection.reset();
    }
}

void DiscordRPC::updatePresence(const DiscordRichPresence& presence) {
    std::lock_guard<std::mutex> lock(rpcMutex);
    if (!isInitialized) return;

    if (!establishConnection()) return;

    std::string nonce = std::to_string(currentNonce++);
    std::string activity = DiscordSerialization::serializeActivity(presence, getMyPid(), nonce);
    if (!sendFrame(1, activity)) {
        activeConnection->disconnect();
    }
}

bool DiscordRPC::isConnected() {
    std::lock_guard<std::mutex> lock(rpcMutex);
    return activeConnection && activeConnection->isOpen();
}
