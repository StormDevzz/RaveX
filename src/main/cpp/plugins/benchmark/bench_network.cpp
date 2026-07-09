#include "include/bench_network.hpp"

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <arpa/inet.h>
#endif

#include "include/bench_common.hpp"
#include <cstring>

namespace ravex {
namespace benchmark {

NetworkBench::NetworkBench() {}

double NetworkBench::measureTime(std::function<void()> fn) {
    Timer timer;
    fn();
    return timer.elapsedMs();
}

double NetworkBench::pingHost(const std::string& host, int count) {
#ifdef _WIN32
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif
    double totalMs = 0;
    for (int i = 0; i < count; i++) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) continue;

        struct hostent* server = gethostbyname(host.c_str());
        if (!server) {
#ifdef _WIN32
            closesocket(sock);
#else
            close(sock);
#endif
            continue;
        }

        struct sockaddr_in addr;
        std::memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        std::memcpy(&addr.sin_addr.s_addr, server->h_addr, server->h_length);
        addr.sin_port = htons(80);

        Timer timer;
        int result = connect(sock, (struct sockaddr*)&addr, sizeof(addr));
        if (result == 0) {
            totalMs += timer.elapsedMs();
        }

#ifdef _WIN32
        closesocket(sock);
#else
        close(sock);
#endif
    }
#ifdef _WIN32
    WSACleanup();
#endif
    return count > 0 ? totalMs / count : -1;
}

double NetworkBench::measureLatency(const std::string& host, int count) {
    return pingHost(host, count);
}

BenchResult NetworkBench::runAll(const std::string& host) {
    BenchResult r;
    r.type = BenchType::Network;
    r.name = "Network";
    r.timeMs = pingHost(host, 5);
    r.score = r.timeMs > 0 ? 1000.0 / (r.timeMs + 0.001) : 0;
    r.unit = "score";
    if (r.timeMs < 0) {
        r.score = 0;
        r.timeMs = 0;
    }
    return r;
}

}
}
