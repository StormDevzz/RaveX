#include "proxy_handler.h"
#include <logger.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <cstring>
#include <vector>
#include <poll.h>

using namespace packet;

namespace proxy {

LocalProxy::LocalProxy() = default;
LocalProxy::~LocalProxy() { stop(); }

bool LocalProxy::start(const Config& config, uint16_t listenPort) {
    config_ = config;

    serverFd_ = ::socket(AF_INET, SOCK_STREAM, 0);
    if (serverFd_ < 0) {
        log::error("[proxy] cannot create server socket");
        return false;
    }

    int opt = 1;
    setsockopt(serverFd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(listenPort);

    if (::bind(serverFd_, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        log::error("[proxy] bind failed");
        ::close(serverFd_);
        serverFd_ = -1;
        return false;
    }

    if (listenPort == 0) {
        struct sockaddr_in bound;
        socklen_t len = sizeof(bound);
        if (getsockname(serverFd_, (struct sockaddr*)&bound, &len) == 0) {
            port_ = ntohs(bound.sin_port);
        }
    } else {
        port_ = listenPort;
    }

    if (::listen(serverFd_, 5) < 0) {
        log::error("[proxy] listen failed");
        ::close(serverFd_);
        serverFd_ = -1;
        return false;
    }

    running_ = true;
    acceptThread_ = std::thread(&LocalProxy::acceptLoop, this);

    log::info("[proxy] local tunnel 127.0.0.1:" + std::to_string(port_)
              + " -> " + config_.proxyHost + ":" + std::to_string(config_.proxyPort)
              + " -> " + config_.targetHost + ":" + std::to_string(config_.targetPort));
    return true;
}

void LocalProxy::stop() {
    running_ = false;
    if (serverFd_ >= 0) {
        ::shutdown(serverFd_, SHUT_RDWR);
        ::close(serverFd_);
        serverFd_ = -1;
    }
    if (acceptThread_.joinable()) {
        acceptThread_.join();
    }
}

void LocalProxy::acceptLoop() {
    while (running_) {
        struct pollfd pfd;
        pfd.fd = serverFd_;
        pfd.events = POLLIN;
        pfd.revents = 0;

        int ret = poll(&pfd, 1, 500);
        if (ret < 0) break;
        if (ret == 0) continue;
        if (!(pfd.revents & POLLIN)) continue;

        struct sockaddr_in client;
        socklen_t len = sizeof(client);
        int clientFd = ::accept(serverFd_, (struct sockaddr*)&client, &len);
        if (clientFd < 0) continue;

        int remoteFd = -1;
        if (!connectToUpstream(remoteFd)) {
            ::close(clientFd);
            continue;
        }

        std::thread(&LocalProxy::relayThread, this, clientFd, remoteFd).detach();
    }
}

bool LocalProxy::connectToUpstream(int& remoteFd) {
    remoteFd = ::socket(AF_INET, SOCK_STREAM, 0);
    if (remoteFd < 0) {
        log::error("[proxy] cannot create upstream socket");
        return false;
    }

    struct hostent* server = gethostbyname(config_.proxyHost.c_str());
    if (!server) {
        log::error("[proxy] cannot resolve: " + config_.proxyHost);
        ::close(remoteFd);
        return false;
    }

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    std::memcpy(&addr.sin_addr.s_addr, server->h_addr, server->h_length);
    addr.sin_port = htons(config_.proxyPort);

    if (::connect(remoteFd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        log::error("[proxy] cannot connect to upstream proxy");
        ::close(remoteFd);
        return false;
    }

    switch (config_.type) {
        case Type::SOCKS5: return socks5Handshake(remoteFd);
        case Type::SOCKS4: return socks4Handshake(remoteFd);
        case Type::HTTP:   return httpConnectHandshake(remoteFd);
    }
    return false;
}

void LocalProxy::relayThread(int clientFd, int remoteFd) {
    log::info("[proxy] tunnel established, relaying data");
    uint8_t buf[65536];
    struct pollfd fds[2];
    fds[0].fd = clientFd;
    fds[1].fd = remoteFd;

    while (running_) {
        fds[0].events = POLLIN;
        fds[1].events = POLLIN;
        fds[0].revents = 0;
        fds[1].revents = 0;

        int ret = poll(fds, 2, 500);
        if (ret <= 0) continue;

        if ((fds[0].revents & (POLLERR | POLLHUP | POLLNVAL)) ||
            (fds[1].revents & (POLLERR | POLLHUP | POLLNVAL)))
            break;

        if (fds[0].revents & POLLIN) {
            int n = read(clientFd, buf, sizeof(buf));
            if (n <= 0) break;
            if (write(remoteFd, buf, n) < 0) break;
        }

        if (fds[1].revents & POLLIN) {
            int n = read(remoteFd, buf, sizeof(buf));
            if (n <= 0) break;
            if (write(clientFd, buf, n) < 0) break;
        }
    }

    ::close(clientFd);
    ::close(remoteFd);
    log::info("[proxy] tunnel closed");
}

bool LocalProxy::socks5Handshake(int fd) {
    if (!config_.useAuth) {
        uint8_t msg[3] = {0x05, 0x01, 0x00};
        if (!sendAll(fd, msg, 3)) return false;
    } else {
        uint8_t msg[4] = {0x05, 0x02, 0x00, 0x02};
        if (!sendAll(fd, msg, 4)) return false;
    }

    uint8_t resp[2];
    if (!recvAll(fd, resp, 2)) return false;
    if (resp[0] != 0x05) { log::error("[proxy] SOCKS5: bad version"); return false; }

    if (resp[1] == 0x02) {
        if (!socks5Auth(fd)) return false;
    } else if (resp[1] != 0x00) {
        log::error("[proxy] SOCKS5: auth rejected"); return false;
    }

    struct hostent* target = gethostbyname(config_.targetHost.c_str());
    if (!target) { log::error("[proxy] cannot resolve " + config_.targetHost); return false; }

    std::vector<uint8_t> conn;
    conn.push_back(0x05); conn.push_back(0x01); conn.push_back(0x00); conn.push_back(0x01);
    conn.insert(conn.end(), target->h_addr, target->h_addr + target->h_length);
    uint16_t p = htons(config_.targetPort);
    conn.push_back((p >> 8) & 0xFF);
    conn.push_back(p & 0xFF);

    if (!sendAll(fd, conn.data(), conn.size())) return false;

    uint8_t connResp[4];
    if (!recvAll(fd, connResp, 4)) return false;
    if (connResp[0] != 0x05 || connResp[1] != 0x00) {
        log::error("[proxy] SOCKS5: connect rejected, status=" + std::to_string(connResp[1]));
        return false;
    }

    uint8_t rest[256];
    int addrLen = (connResp[3] == 0x01) ? 6 : (connResp[3] == 0x04) ? 18 : 0;
    if (addrLen > 0) { if (!recvAll(fd, rest, addrLen)) return false; }
    else if (connResp[3] == 0x03) {
        if (!recvAll(fd, rest, 1)) return false;
        if (!recvAll(fd, rest + 1, rest[0] + 2)) return false;
    }

    return true;
}

bool LocalProxy::socks5Auth(int fd) {
    std::vector<uint8_t> auth;
    auth.push_back(0x01);
    auth.push_back(static_cast<uint8_t>(config_.username.size()));
    auth.insert(auth.end(), config_.username.begin(), config_.username.end());
    auth.push_back(static_cast<uint8_t>(config_.password.size()));
    auth.insert(auth.end(), config_.password.begin(), config_.password.end());

    if (!sendAll(fd, auth.data(), static_cast<int>(auth.size()))) return false;

    uint8_t resp[2];
    if (!recvAll(fd, resp, 2)) return false;
    if (resp[1] != 0x00) { log::error("[proxy] SOCKS5: auth failed"); return false; }
    return true;
}

bool LocalProxy::socks4Handshake(int fd) {
    struct hostent* target = gethostbyname(config_.targetHost.c_str());
    if (!target) { log::error("[proxy] cannot resolve " + config_.targetHost); return false; }

    std::vector<uint8_t> req;
    req.push_back(0x04); req.push_back(0x01);
    uint16_t p = htons(config_.targetPort);
    req.push_back((p >> 8) & 0xFF);
    req.push_back(p & 0xFF);
    req.insert(req.end(), target->h_addr, target->h_addr + target->h_length);
    req.push_back(0x00);

    if (!sendAll(fd, req.data(), static_cast<int>(req.size()))) return false;

    uint8_t resp[8];
    if (!recvAll(fd, resp, 8)) return false;
    if (resp[0] != 0x00 || resp[1] != 0x5A) {
        log::error("[proxy] SOCKS4: rejected");
        return false;
    }
    return true;
}

bool LocalProxy::httpConnectHandshake(int fd) {
    std::string req = "CONNECT " + config_.targetHost + ":" + std::to_string(config_.targetPort)
                      + " HTTP/1.1\r\nHost: " + config_.targetHost + ":" + std::to_string(config_.targetPort) + "\r\n";

    if (config_.useAuth) {
        std::string userPass = config_.username + ":" + config_.password;
        static const char b64[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        std::string encoded;
        size_t len = userPass.size();
        for (size_t i = 0; i < len; i += 3) {
            unsigned int c1 = (unsigned char)userPass[i];
            unsigned int c2 = (i + 1 < len) ? (unsigned char)userPass[i + 1] : 0;
            unsigned int c3 = (i + 2 < len) ? (unsigned char)userPass[i + 2] : 0;
            unsigned int triple = (c1 << 16) | (c2 << 8) | c3;
            encoded += b64[(triple >> 18) & 0x3F];
            encoded += b64[(triple >> 12) & 0x3F];
            encoded += (i + 1 < len) ? b64[(triple >> 6) & 0x3F] : '=';
            encoded += (i + 2 < len) ? b64[triple & 0x3F] : '=';
        }
        req += "Proxy-Authorization: Basic " + encoded + "\r\n";
    }
    req += "\r\n";

    if (!sendString(fd, req)) return false;

    std::string line;
    bool ok = false;
    while ((line = recvLine(fd)) != "") {
        if (!ok) {
            if (line.find("200") == std::string::npos) {
                log::error("[proxy] HTTP CONNECT failed: " + line);
                return false;
            }
            ok = true;
        }
    }
    return ok;
}

bool LocalProxy::sendAll(int fd, const uint8_t* data, int len) {
    int sent = 0;
    while (sent < len) {
        int n = write(fd, data + sent, len - sent);
        if (n <= 0) return false;
        sent += n;
    }
    return true;
}

bool LocalProxy::recvAll(int fd, uint8_t* data, int len) {
    int got = 0;
    while (got < len) {
        int n = read(fd, data + got, len - got);
        if (n <= 0) return false;
        got += n;
    }
    return true;
}

int LocalProxy::recvSome(int fd, uint8_t* data, int maxLen) {
    return read(fd, data, maxLen);
}

bool LocalProxy::sendString(int fd, const std::string& str) {
    return sendAll(fd, reinterpret_cast<const uint8_t*>(str.data()), static_cast<int>(str.size()));
}

std::string LocalProxy::recvLine(int fd) {
    std::string line;
    uint8_t ch;
    while (recvAll(fd, &ch, 1)) {
        if (ch == '\n') break;
        if (ch != '\r') line += static_cast<char>(ch);
    }
    return line;
}

} // namespace proxy
