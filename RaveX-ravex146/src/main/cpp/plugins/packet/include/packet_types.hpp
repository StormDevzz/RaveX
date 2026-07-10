#pragma once

#include <cstdint>
#include <string>
#include <vector>
#include <array>
#include <functional>

namespace packet {

using PacketId = int32_t;
using Timestamp = uint64_t;

struct Address {
    std::string host;
    uint16_t port = 25565;
    std::string toString() const { return host + ":" + std::to_string(port); }
};

struct Packet {
    PacketId id = 0;
    std::vector<uint8_t> data;
    Timestamp time = 0;
    bool outgoing = false;
    int compression = -1;
};

enum class ProtocolState : uint8_t {
    Handshaking,
    Status,
    Login,
    Play,
};

struct Connection {
    Address addr;
    ProtocolState state = ProtocolState::Handshaking;
    int protocolVersion = 767;
    bool encrypted = false;
    bool compressed = false;
    int compressionThreshold = -1;
};

struct PacketStats {
    int64_t totalPackets = 0;
    int64_t totalBytes = 0;
    int64_t packetsPerSecond = 0;
    int64_t bytesPerSecond = 0;
    std::array<int64_t, 256> packetCounts = {};
};

struct SnifferConfig {
    std::string interface;
    std::string filterExp;
    Address target;
    bool captureOutgoing = true;
    bool captureIncoming = true;
    int snapLen = 65536;
    int timeout = 1000;
    bool promiscuous = true;
};

enum class LogLevel : uint8_t {
    Debug, Info, Warn, Error
};

}
