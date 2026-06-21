#pragma once

#include "github_types.h"
#include <memory>
#include <functional>
#include <string>

namespace ravex {
namespace github {

// ─── Cross-platform HTTP client ──────────────────────────────────────────────
//
// Windows: uses WinHTTP (ships with OS, no extra deps)
// Linux:   uses POSIX sockets + OpenSSL (libssl-dev required)
//
// All operations are synchronous. For async use, wrap in std::thread.

class HttpClient {
public:
    HttpClient();
    ~HttpClient();

    HttpClient(const HttpClient&) = delete;
    HttpClient& operator=(const HttpClient&) = delete;

    // Set auth token (Bearer)
    void setToken(const std::string& token);
    void setUserAgent(const std::string& ua);
    void setTimeoutSec(int sec);
    void setLogCallback(LogCallback cb);

    // Execute HTTP request
    HttpResponse request(const HttpRequest& req);

    // Convenience
    HttpResponse get(const std::string& url,
                     std::map<std::string, std::string> extraHeaders = {});
    HttpResponse post(const std::string& url, const std::string& body,
                      std::map<std::string, std::string> extraHeaders = {});

    // Download a file to disk with progress callback
    bool download(const std::string& url, const std::string& outputPath,
                  ProgressCallback progress = nullptr);

private:
    struct Impl;
    std::unique_ptr<Impl> m_impl;
};

} // namespace github
} // namespace ravex
