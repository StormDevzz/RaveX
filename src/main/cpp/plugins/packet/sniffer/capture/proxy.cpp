#include "proxy.hpp"
#include "../../include/logger.hpp"
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cstring>

namespace packet {

bool Proxy::start(const Address& target, uint16_t listenPort) {
    target_ = target;

    serverFd_ = socket(AF_INET, SOCK_STREAM, 0);
    if (serverFd_ < 0) {
        log::error("proxy: cannot create socket");
        return false;
    }

    int opt = 1;
    setsockopt(serverFd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(listenPort);

    if (bind(serverFd_, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        log::error("proxy: bind failed");
        close(serverFd_); serverFd_ = -1;
        return false;
    }

    if (listen(serverFd_, 1) < 0) {
        log::error("proxy: listen failed");
        close(serverFd_); serverFd_ = -1;
        return false;
    }

    log::info("proxy listening on 0.0.0.0:" + std::to_string(listenPort));
    running_ = true;
    return true;
}

bool Proxy::acceptClient() {
    struct sockaddr_in client;
    socklen_t len = sizeof(client);
    clientFd_ = accept(serverFd_, (struct sockaddr*)&client, &len);
    if (clientFd_ < 0) return false;
    log::info("proxy: client connected");

    
    struct sockaddr_in target;
    std::memset(&target, 0, sizeof(target));
    target.sin_family = AF_INET;
    target.sin_port = htons(target_.port);
    target.sin_addr.s_addr = htonl(0x7F000001); 

    int remoteFd = socket(AF_INET, SOCK_STREAM, 0);
    if (remoteFd < 0 || connect(remoteFd, (struct sockaddr*)&target, sizeof(target)) < 0) {
        log::error("proxy: cannot connect to target");
        close(clientFd_); clientFd_ = -1;
        return false;
    }

    remoteFd_ = remoteFd;
    return true;
}

void Proxy::stop() {
    running_ = false;
    if (clientFd_ >= 0) { close(clientFd_); clientFd_ = -1; }
    if (remoteFd_ >= 0) { close(remoteFd_); remoteFd_ = -1; }
    if (serverFd_ >= 0) { close(serverFd_); serverFd_ = -1; }
}

bool Proxy::relayPacket(int from, int to, bool outgoing) {
    uint8_t buf[65536];
    int n = static_cast<int>(read(from, buf, sizeof(buf)));
    if (n <= 0) return false;

    if (callback_) {
        Packet pkt;
        pkt.time = 0;
        pkt.outgoing = outgoing;
        pkt.data.assign(buf, buf + n);
        callback_(pkt);
    }

    if (write(to, buf, n) < 0) return false;
    return true;
}

} 
