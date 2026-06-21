#include "ravex/github/http_client.h"
#include <cstring>
#include <sstream>
#include <fstream>
#include <iomanip>
#include <cctype>
#include <thread>
#include <chrono>

#ifdef _WIN32
    #include <windows.h>
    #include <winhttp.h>
    #pragma comment(lib, "winhttp.lib")
#else
    #include <sys/socket.h>
    #include <netinet/in.h>
    #include <netdb.h>
    #include <arpa/inet.h>
    #include <unistd.h>
    #include <fcntl.h>
    #include <openssl/ssl.h>
    #include <openssl/err.h>
#endif

namespace ravex {
namespace github {

// ─── Internal helpers ────────────────────────────────────────────────────────

static std::string urlEncode(const std::string& s) {
    std::ostringstream oss;
    oss << std::hex;
    for (unsigned char c : s) {
        if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~')
            oss << c;
        else
            oss << '%' << std::setw(2) << std::uppercase << (unsigned int)c;
    }
    return oss.str();
}

static void parseUrl(const std::string& url, std::string& host, std::string& path,
                     int& port, bool& isHttps) {
    isHttps = url.compare(0, 8, "https://") == 0;
    size_t start = isHttps ? 8 : 7;
    size_t slash = url.find('/', start);
    std::string hostPart = (slash != std::string::npos) ? url.substr(start, slash - start) : url.substr(start);
    path = (slash != std::string::npos) ? url.substr(slash) : "/";

    size_t colon = hostPart.find(':');
    if (colon != std::string::npos) {
        host = hostPart.substr(0, colon);
        port = std::stoi(hostPart.substr(colon + 1));
    } else {
        host = hostPart;
        port = isHttps ? 443 : 80;
    }
}

// ─── Platform-specific implementation ────────────────────────────────────────

struct HttpClient::Impl {
    std::string token;
    std::string userAgent = "RaveX-GitHub/1.0";
    int timeoutSec = 30;
    LogCallback logCb;

    void log(LogLevel lvl, const std::string& msg) {
        if (logCb) logCb(lvl, msg);
    }

    HttpResponse exec(const HttpRequest& req) {
#ifdef _WIN32
        return execWinHttp(req);
#else
        return execSocket(req);
#endif
    }

    bool downloadFile(const std::string& url, const std::string& outputPath,
                      ProgressCallback progress) {
#ifdef _WIN32
        return downloadWinHttp(url, outputPath, progress);
#else
        return downloadSocket(url, outputPath, progress);
#endif
    }

    // ─── Windows: WinHTTP ───────────────────────────────────────────────
#ifdef _WIN32
    HttpResponse execWinHttp(const HttpRequest& req) {
        HttpResponse resp;
        std::string host, path; int port; bool https;
        parseUrl(req.url, host, path, port, https);

        HINTERNET hSession = WinHttpOpen(
            std::wstring(userAgent.begin(), userAgent.end()).c_str(),
            WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, nullptr, nullptr, 0);
        if (!hSession) { resp.error = true; resp.errorMsg = "WinHttpOpen failed"; return resp; }

        HINTERNET hConnect = WinHttpConnect(hSession,
            std::wstring(host.begin(), host.end()).c_str(), port, 0);
        if (!hConnect) { resp.error = true; resp.errorMsg = "WinHttpConnect failed"; WinHttpCloseHandle(hSession); return resp; }

        const wchar_t* methods[] = { L"GET", L"POST", L"PUT", L"DELETE" };
        HINTERNET hRequest = WinHttpOpenRequest(hConnect, methods[(int)req.method],
            std::wstring(path.begin(), path.end()).c_str(), nullptr,
            WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES,
            https ? WINHTTP_FLAG_SECURE : 0);
        if (!hRequest) { resp.error = true; resp.errorMsg = "WinHttpOpenRequest failed"; WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession); return resp; }

        // Timeout
        int timeout = req.timeoutSec * 1000;
        WinHttpSetOption(hRequest, WINHTTP_OPTION_CONNECT_TIMEOUT, &timeout, sizeof(timeout));
        WinHttpSetOption(hRequest, WINHTTP_OPTION_SEND_TIMEOUT, &timeout, sizeof(timeout));
        WinHttpSetOption(hRequest, WINHTTP_OPTION_RECEIVE_TIMEOUT, &timeout, sizeof(timeout));

        // Headers
        std::string headers;
        for (auto& [k, v] : req.headers) headers += k + ": " + v + "\r\n";
        if (!token.empty()) headers += "Authorization: Bearer " + token + "\r\n";

        std::wstring hdrW(headers.begin(), headers.end());
        if (!WinHttpSendRequest(hRequest, hdrW.c_str(), hdrW.length(),
                                req.method == HttpMethod::POST || req.method == HttpMethod::PUT
                                    ? (LPVOID)req.body.data() : nullptr,
                                req.method == HttpMethod::POST || req.method == HttpMethod::PUT
                                    ? (DWORD)req.body.size() : 0,
                                req.method == HttpMethod::POST || req.method == HttpMethod::PUT
                                    ? (DWORD)req.body.size() : 0,
                                0)) {
            resp.error = true; resp.errorMsg = "WinHttpSendRequest failed";
            WinHttpCloseHandle(hRequest); WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession);
            return resp;
        }

        if (!WinHttpReceiveResponse(hRequest, nullptr)) {
            resp.error = true; resp.errorMsg = "WinHttpReceiveResponse failed";
            WinHttpCloseHandle(hRequest); WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession);
            return resp;
        }

        // Status code
        DWORD status = 0, statusSize = sizeof(status);
        WinHttpQueryHeaders(hRequest, WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                            nullptr, &status, &statusSize, nullptr);
        resp.statusCode = status;

        // Body
        DWORD bytesAvail = 0;
        while (WinHttpQueryDataAvailable(hRequest, &bytesAvail) && bytesAvail > 0) {
            std::vector<char> buf(bytesAvail);
            DWORD bytesRead = 0;
            if (WinHttpReadData(hRequest, buf.data(), bytesAvail, &bytesRead)) {
                resp.body.append(buf.data(), bytesRead);
            }
        }

        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return resp;
    }

    bool downloadWinHttp(const std::string& url, const std::string& outputPath,
                         ProgressCallback progress) {
        // Similar to execWinHttp but writes to file with progress
        std::string host, path; int port; bool https;
        parseUrl(url, host, path, port, https);

        HINTERNET hSession = WinHttpOpen(
            std::wstring(userAgent.begin(), userAgent.end()).c_str(),
            WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, nullptr, nullptr, 0);
        if (!hSession) return false;

        HINTERNET hConnect = WinHttpConnect(hSession,
            std::wstring(host.begin(), host.end()).c_str(), port, 0);
        if (!hConnect) { WinHttpCloseHandle(hSession); return false; }

        HINTERNET hRequest = WinHttpOpenRequest(hConnect, L"GET",
            std::wstring(path.begin(), path.end()).c_str(), nullptr,
            WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES,
            https ? WINHTTP_FLAG_SECURE : 0);
        if (!hRequest) { WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession); return false; }

        int timeout = timeoutSec * 1000;
        WinHttpSetOption(hRequest, WINHTTP_OPTION_CONNECT_TIMEOUT, &timeout, sizeof(timeout));
        WinHttpSetOption(hRequest, WINHTTP_OPTION_SEND_TIMEOUT, &timeout, sizeof(timeout));
        WinHttpSetOption(hRequest, WINHTTP_OPTION_RECEIVE_TIMEOUT, &timeout, sizeof(timeout));

        if (!WinHttpSendRequest(hRequest, WINHTTP_NO_ADDITIONAL_HEADERS, 0,
                                nullptr, 0, 0, 0)) {
            WinHttpCloseHandle(hRequest); WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession);
            return false;
        }
        if (!WinHttpReceiveResponse(hRequest, nullptr)) {
            WinHttpCloseHandle(hRequest); WinHttpCloseHandle(hConnect); WinHttpCloseHandle(hSession);
            return false;
        }

        std::ofstream ofs(outputPath, std::ios::binary);
        if (!ofs) return false;

        DWORD total = 0, bytesAvail = 0;
        while (WinHttpQueryDataAvailable(hRequest, &bytesAvail) && bytesAvail > 0) {
            std::vector<char> buf(bytesAvail);
            DWORD bytesRead = 0;
            if (WinHttpReadData(hRequest, buf.data(), bytesAvail, &bytesRead)) {
                ofs.write(buf.data(), bytesRead);
                total += bytesRead;
                if (progress) progress(total, 0); // total unknown with chunked
            }
        }

        ofs.close();
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return true;
    }

    // ─── Linux: POSIX sockets + OpenSSL ─────────────────────────────────
#else
    static int sockConnect(const std::string& host, int port, int timeoutSec) {
        struct hostent* he = gethostbyname(host.c_str());
        if (!he) return -1;

        int fd = socket(AF_INET, SOCK_STREAM, 0);
        if (fd < 0) return -1;

        struct sockaddr_in addr = {};
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        memcpy(&addr.sin_addr, he->h_addr_list[0], he->h_length);

        // Non-blocking connect with timeout
        fcntl(fd, F_SETFL, O_NONBLOCK);
        int res = connect(fd, (struct sockaddr*)&addr, sizeof(addr));
        if (res < 0 && errno != EINPROGRESS) { close(fd); return -1; }

        fd_set fdSet;
        FD_ZERO(&fdSet);
        FD_SET(fd, &fdSet);
        struct timeval tv = { timeoutSec, 0 };
        res = select(fd + 1, nullptr, &fdSet, nullptr, &tv);
        if (res <= 0) { close(fd); return -1; }

        fcntl(fd, F_SETFL, 0);
        return fd;
    }

    static bool sockSend(int fd, const std::string& data) {
        const char* ptr = data.data();
        size_t remaining = data.size();
        while (remaining > 0) {
            ssize_t sent = send(fd, ptr, remaining, 0);
            if (sent <= 0) return false;
            ptr += sent;
            remaining -= sent;
        }
        return true;
    }

    static std::string sockRecv(int fd) {
        std::string result;
        char buf[65536];
        fd_set fdSet;
        struct timeval tv = { 5, 0 }; // 5s timeout per read
        while (true) {
            FD_ZERO(&fdSet);
            FD_SET(fd, &fdSet);
            int res = select(fd + 1, &fdSet, nullptr, nullptr, &tv);
            if (res <= 0) break;
            ssize_t n = recv(fd, buf, sizeof(buf), 0);
            if (n <= 0) break;
            result.append(buf, n);
        }
        return result;
    }

    HttpResponse execSocket(const HttpRequest& req) {
        HttpResponse resp;
        std::string host, path; int port; bool https;
        parseUrl(req.url, host, path, port, https);

        SSL_CTX* sslCtx = nullptr;
        SSL*     ssl    = nullptr;

        int fd = sockConnect(host, port, timeoutSec);
        if (fd < 0) { resp.error = true; resp.errorMsg = "connect failed"; return resp; }

        if (https) {
            SSL_load_error_strings();
            OpenSSL_add_all_algorithms();
            sslCtx = SSL_CTX_new(TLS_client_method());
            if (sslCtx) {
                ssl = SSL_new(sslCtx);
                SSL_set_fd(ssl, fd);
                if (SSL_connect(ssl) <= 0) {
                    resp.error = true; resp.errorMsg = "SSL handshake failed";
                    SSL_free(ssl); ssl = nullptr;
                    SSL_CTX_free(sslCtx); sslCtx = nullptr;
                    close(fd);
                    return resp;
                }
            }
        }

        // Build HTTP request
        std::ostringstream reqStream;
        static const char* methodStrs[] = { "GET", "POST", "PUT", "DELETE" };
        reqStream << methodStrs[(int)req.method] << " " << path << " HTTP/1.1\r\n";
        reqStream << "Host: " << host << "\r\n";
        reqStream << "User-Agent: " << userAgent << "\r\n";
        reqStream << "Accept: application/vnd.github.v3+json\r\n";
        if (!token.empty()) reqStream << "Authorization: Bearer " << token << "\r\n";
        for (auto& [k, v] : req.headers) reqStream << k << ": " << v << "\r\n";
        if (!req.body.empty()) {
            reqStream << "Content-Length: " << req.body.size() << "\r\n";
        }
        reqStream << "Connection: close\r\n\r\n";
        if (!req.body.empty()) reqStream << req.body;

        std::string reqStr = reqStream.str();

        bool sent = false;
        if (ssl) {
            sent = SSL_write(ssl, reqStr.data(), (int)reqStr.size()) > 0;
        } else {
            sent = sockSend(fd, reqStr);
        }
        if (!sent) { resp.error = true; resp.errorMsg = "send failed"; close(fd); return resp; }

        std::string rawResp;
        if (ssl) {
            char buf[65536];
            int n;
            while ((n = SSL_read(ssl, buf, sizeof(buf))) > 0)
                rawResp.append(buf, n);
        } else {
            rawResp = sockRecv(fd);
        }

        if (ssl) { SSL_free(ssl); SSL_CTX_free(sslCtx); }
        close(fd);

        // Parse response
        size_t headerEnd = rawResp.find("\r\n\r\n");
        if (headerEnd == std::string::npos) {
            resp.error = true; resp.errorMsg = "malformed response";
            return resp;
        }

        std::string headerSection = rawResp.substr(0, headerEnd);
        std::string bodySection = rawResp.substr(headerEnd + 4);

        // Status line: "HTTP/1.1 200 OK"
        size_t sp1 = headerSection.find(' ');
        size_t sp2 = sp1 != std::string::npos ? headerSection.find(' ', sp1 + 1) : std::string::npos;
        if (sp2 != std::string::npos) {
            resp.statusCode = std::stoi(headerSection.substr(sp1 + 1, sp2 - sp1 - 1));
        }

        // Headers
        std::istringstream hdrStream(headerSection.substr(headerSection.find('\n') + 1));
        std::string line;
        while (std::getline(hdrStream, line)) {
            size_t colon = line.find(':');
            if (colon != std::string::npos) {
                std::string key = line.substr(0, colon);
                std::string val = line.substr(colon + 1);
                if (!val.empty() && val.back() == '\r') val.pop_back();
                if (!val.empty() && val.front() == ' ') val = val.substr(1);
                resp.headers[key] = val;
            }
        }

        // Handle chunked transfer encoding
        auto it = resp.headers.find("Transfer-Encoding");
        if (it != resp.headers.end() && it->second == "chunked") {
            std::string unchunked;
            std::istringstream chunkStream(bodySection);
            std::string chunkLine;
            while (std::getline(chunkStream, chunkLine)) {
                if (chunkLine.empty() || chunkLine == "\r") continue;
                // Remove \r
                if (!chunkLine.empty() && chunkLine.back() == '\r') chunkLine.pop_back();
                unsigned long chunkSize = strtoul(chunkLine.c_str(), nullptr, 16);
                if (chunkSize == 0) break;
                std::vector<char> chunkData(chunkSize);
                chunkStream.read(chunkData.data(), chunkSize);
                unchunked.append(chunkData.data(), chunkSize);
                // Skip trailing \r\n
                if (chunkStream.peek() == '\r') chunkStream.get();
                if (chunkStream.peek() == '\n') chunkStream.get();
            }
            resp.body = std::move(unchunked);
        } else {
            resp.body = std::move(bodySection);
        }

        return resp;
    }

    bool downloadSocket(const std::string& url, const std::string& outputPath,
                        ProgressCallback progress) {
        auto resp = exec({ HttpMethod::GET, url });
        if (resp.error || resp.statusCode != 200) return false;
        std::ofstream ofs(outputPath, std::ios::binary);
        if (!ofs) return false;
        ofs.write(resp.body.data(), resp.body.size());
        if (progress) progress(resp.body.size(), resp.body.size());
        return true;
    }
#endif
};

// ─── HttpClient public API ───────────────────────────────────────────────────

HttpClient::HttpClient() : m_impl(std::make_unique<Impl>()) {}
HttpClient::~HttpClient() = default;

void HttpClient::setToken(const std::string& token) { m_impl->token = token; }
void HttpClient::setUserAgent(const std::string& ua) { m_impl->userAgent = ua; }
void HttpClient::setTimeoutSec(int sec) { m_impl->timeoutSec = sec; }
void HttpClient::setLogCallback(LogCallback cb) { m_impl->logCb = std::move(cb); }

HttpResponse HttpClient::request(const HttpRequest& req) { return m_impl->exec(req); }

HttpResponse HttpClient::get(const std::string& url,
                              std::map<std::string, std::string> extraHeaders) {
    HttpRequest req;
    req.method = HttpMethod::GET;
    req.url = url;
    req.headers = std::move(extraHeaders);
    return m_impl->exec(req);
}

HttpResponse HttpClient::post(const std::string& url, const std::string& body,
                               std::map<std::string, std::string> extraHeaders) {
    HttpRequest req;
    req.method = HttpMethod::POST;
    req.url = url;
    req.body = body;
    req.headers = std::move(extraHeaders);
    if (!req.body.empty() && req.headers.find("Content-Type") == req.headers.end()) {
        req.headers["Content-Type"] = "application/json";
    }
    return m_impl->exec(req);
}

bool HttpClient::download(const std::string& url, const std::string& outputPath,
                           ProgressCallback progress) {
    return m_impl->downloadFile(url, outputPath, std::move(progress));
}

} // namespace github
} // namespace ravex
