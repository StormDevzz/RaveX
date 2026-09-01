#include "game/include/serverping.hpp"
#include "net/include/json.hpp"

#include <WinSock2.h>
#include <WS2tcpip.h>
#include <Windows.h>
#include <chrono>
#include <cstring>
#include <vector>

#pragma comment(lib, "ws2_32.lib")

namespace ravex::game {

static std::string encodeVarInt(int value) {
    std::string out;
    while (true) {
        unsigned char byte = value & 0x7F;
        value >>= 7;
        if (value != 0) byte |= 0x80;
        out.push_back(static_cast<char>(byte));
        if (value == 0) break;
    }
    return out;
}

static int readVarInt(SOCKET s) {
    int result = 0;
    int shift = 0;
    unsigned char buf;
    while (recv(s, reinterpret_cast<char*>(&buf), 1, 0) == 1) {
        result |= (buf & 0x7F) << shift;
        if ((buf & 0x80) == 0) break;
        shift += 7;
        if (shift > 35) break;
    }
    return result;
}

static bool sendPacket(SOCKET s, const std::string& data, std::string* error) {
    int sent = 0;
    int total = static_cast<int>(data.size());
    while (sent < total) {
        int n = send(s, data.data() + sent, total - sent, 0);
        if (n <= 0) {
            *error = "Failed to send packet";
            return false;
        }
        sent += n;
    }
    return true;
}

static bool recvAll(SOCKET s, char* buf, int len, std::string* error) {
    int received = 0;
    while (received < len) {
        int n = recv(s, buf + received, len - received, 0);
        if (n <= 0) {
            *error = "Failed to receive data";
            return false;
        }
        received += n;
    }
    return true;
}

static std::string extractText(const ravex::json::Value& desc) {
    if (desc.type() == ravex::json::Value::Type::String) return desc.asString();
    if (desc.type() == ravex::json::Value::Type::Object) {
        if (desc.has("text")) return desc.at("text").asString();
        if (desc.has("extra") && desc.at("extra").type() == ravex::json::Value::Type::Array) {
            std::string result;
            for (size_t i = 0; i < desc.at("extra").size(); ++i) {
                result += extractText(desc.at("extra").at(i));
            }
            return result;
        }
    }
    return "";
}

static bool parseAddress(const std::string& address, std::string& host, int& port, std::string* error) {
    size_t colonPos = address.find(':');
    if (colonPos != std::string::npos) {
        host = address.substr(0, colonPos);
        try {
            port = std::stoi(address.substr(colonPos + 1));
        } catch (...) {
            *error = "Invalid port number";
            return false;
        }
    } else {
        host = address;
        port = 25565;
    }
    if (host.empty()) {
        *error = "Empty host";
        return false;
    }
    return true;
}

bool pingServer(const std::string& address, ServerInfo* out, std::string* error) {
    error->clear();
    out->online = false;

    std::string host;
    int port = 25565;
    if (!parseAddress(address, host, port, error)) return false;

    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        *error = "WSAStartup failed";
        return false;
    }

    SOCKET sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sock == INVALID_SOCKET) {
        WSACleanup();
        *error = "Failed to create socket";
        return false;
    }

    struct addrinfo hints {};
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    struct addrinfo* result = nullptr;
    std::string portStr = std::to_string(port);
    if (getaddrinfo(host.c_str(), portStr.c_str(), &hints, &result) != 0 || !result) {
        closesocket(sock);
        WSACleanup();
        *error = "Failed to resolve host";
        return false;
    }

    u_long nonBlocking = 1;
    ioctlsocket(sock, FIONBIO, &nonBlocking);

    int connectResult = connect(sock, result->ai_addr, static_cast<int>(result->ai_addrlen));
    freeaddrinfo(result);

    if (connectResult == SOCKET_ERROR && WSAGetLastError() != WSAEWOULDBLOCK) {
        closesocket(sock);
        WSACleanup();
        *error = "Failed to connect";
        return false;
    }

    fd_set writeSet;
    FD_ZERO(&writeSet);
    FD_SET(sock, &writeSet);

    timeval timeout;
    timeout.tv_sec = 5;
    timeout.tv_usec = 0;

    connectResult = select(0, nullptr, &writeSet, nullptr, &timeout);
    if (connectResult <= 0) {
        closesocket(sock);
        WSACleanup();
        *error = "Connection timed out";
        return false;
    }

    int sockError = 0;
    int optLen = sizeof(sockError);
    getsockopt(sock, SOL_SOCKET, SO_ERROR, reinterpret_cast<char*>(&sockError), &optLen);
    if (sockError != 0) {
        closesocket(sock);
        WSACleanup();
        *error = "Connection failed";
        return false;
    }

    nonBlocking = 0;
    ioctlsocket(sock, FIONBIO, &nonBlocking);

    auto startTime = std::chrono::steady_clock::now();

    std::string serverAddrStr = host;
    std::string portVarInt = encodeVarInt(port);
    std::string nextState = encodeVarInt(1);

    std::string protocolVarInt = encodeVarInt(767);
    std::string addrLen = encodeVarInt(static_cast<int>(serverAddrStr.size()));

    std::string handshakePayload;
    handshakePayload += protocolVarInt;
    handshakePayload += addrLen;
    handshakePayload += serverAddrStr;
    handshakePayload += portVarInt;
    handshakePayload += nextState;

    std::string handshakePacket;
    handshakePacket += encodeVarInt(1 + static_cast<int>(handshakePayload.size()));
    handshakePacket += encodeVarInt(0);
    handshakePacket += handshakePayload;

    if (!sendPacket(sock, handshakePacket, error)) {
        closesocket(sock);
        WSACleanup();
        return false;
    }

    std::string statusPacket;
    statusPacket += encodeVarInt(1);
    statusPacket += encodeVarInt(0);

    if (!sendPacket(sock, statusPacket, error)) {
        closesocket(sock);
        WSACleanup();
        return false;
    }

    int responseLength = readVarInt(sock);
    if (responseLength <= 0 || responseLength > 65536) {
        closesocket(sock);
        WSACleanup();
        *error = "Invalid response length";
        return false;
    }

    std::vector<char> responseBuffer(responseLength);
    if (!recvAll(sock, responseBuffer.data(), responseLength, error)) {
        closesocket(sock);
        WSACleanup();
        return false;
    }

    auto endTime = std::chrono::steady_clock::now();
    out->latencyMs = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime).count();

    closesocket(sock);
    WSACleanup();

    int offset = 0;

    auto readVarIntFromBuffer = [&](int& pos) -> int {
        int result = 0;
        int shift = 0;
        while (pos < responseLength) {
            unsigned char byte = static_cast<unsigned char>(responseBuffer[pos]);
            pos++;
            result |= (byte & 0x7F) << shift;
            if ((byte & 0x80) == 0) break;
            shift += 7;
            if (shift > 35) break;
        }
        return result;
    };

    readVarIntFromBuffer(offset);

    int packetId = readVarIntFromBuffer(offset);
    if (packetId != 0) {
        *error = "Unexpected packet ID";
        return false;
    }

    int jsonLength = readVarIntFromBuffer(offset);
    if (jsonLength <= 0 || offset + jsonLength > responseLength) {
        *error = "Invalid JSON length";
        return false;
    }

    std::string jsonStr(responseBuffer.data() + offset, jsonLength);

    ravex::json::Value json = ravex::json::Value::parse(jsonStr);

    if (json.has("description")) {
        out->motd = extractText(json.at("description"));
    }

    if (json.has("players") && json.at("players").type() == ravex::json::Value::Type::Object) {
        if (json.at("players").has("online")) {
            out->onlinePlayers = static_cast<int>(json.at("players").at("online").asNumber());
        }
        if (json.at("players").has("max")) {
            out->maxPlayers = static_cast<int>(json.at("players").at("max").asNumber());
        }
    }

    if (json.has("version") && json.at("version").type() == ravex::json::Value::Type::Object) {
        if (json.at("version").has("name")) {
            out->version = json.at("version").at("name").asString();
        }
    }

    out->online = true;
    return true;
}

}
