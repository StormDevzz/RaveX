#pragma once

#include "../../include/packet_types.hpp"
#include "../../include/varint.hpp"
#include <vector>
#include <string>

namespace packet {
namespace proto {

struct Handshake {
    int32_t protocolVersion = 767;
    std::string serverAddress;
    uint16_t port = 25565;
    int32_t nextState = 2;

    std::vector<uint8_t> encode();
    static Handshake decode(const uint8_t* data, size_t size);
};

struct StatusRequest {
    std::vector<uint8_t> encode();
};

struct StatusResponse {
    std::string json;
    static StatusResponse decode(const uint8_t* data, size_t size);
};

struct LoginStart {
    std::string username;
    std::vector<uint8_t> encode();
    static LoginStart decode(const uint8_t* data, size_t size);
};

struct LoginSuccess {
    std::string uuid;
    std::string username;
    static LoginSuccess decode(const uint8_t* data, size_t size);
};

struct CompressionRequest {
    int32_t threshold = 256;
    static CompressionRequest decode(const uint8_t* data, size_t size);
    std::vector<uint8_t> encode();
};

struct EncryptionRequest {
    std::string serverId;
    std::vector<uint8_t> publicKey;
    std::vector<uint8_t> verifyToken;
    static EncryptionRequest decode(const uint8_t* data, size_t size);
};

struct EncryptionResponse {
    std::vector<uint8_t> sharedSecret;
    std::vector<uint8_t> verifyToken;
    std::vector<uint8_t> encode();
    static EncryptionResponse decode(const uint8_t* data, size_t size);
};

}
}
