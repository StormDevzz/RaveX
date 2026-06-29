#pragma once

#include <cstdint>
#include <string>
#include <thread>
#include <atomic>

namespace proxy {

enum class Type {
    SOCKS5,
    SOCKS4,
    HTTP
};

struct Config {
    Type type = Type::SOCKS5;
    std::string proxyHost = "127.0.0.1";
    uint16_t proxyPort = 1080;
    bool useAuth = false;
    std::string username;
    std::string password;
    std::string targetHost;
    uint16_t targetPort = 25565;
};

class LocalProxy {
public:
    LocalProxy();
    ~LocalProxy();

    bool start(const Config& config, uint16_t listenPort = 0);
    void stop();
    uint16_t getPort() const { return port_; }
    bool isRunning() const { return running_.load(); }
    void setTarget(const std::string& host, uint16_t port);

private:
    Config config_;
    std::atomic<bool> running_{false};
    int serverFd_ = -1;
    uint16_t port_ = 0;
    std::thread acceptThread_;

    void acceptLoop();
    void relayThread(int clientFd, int remoteFd);

    
    bool connectToUpstream(int& remoteFd);
    bool socks5Handshake(int fd);
    bool socks4Handshake(int fd);
    bool httpConnectHandshake(int fd);
    bool socks5Auth(int fd);

    static bool sendAll(int fd, const uint8_t* data, int len);
    static bool recvAll(int fd, uint8_t* data, int len);
    static int recvSome(int fd, uint8_t* data, int maxLen);
    static bool sendString(int fd, const std::string& str);
    static std::string recvLine(int fd);
};

} 
