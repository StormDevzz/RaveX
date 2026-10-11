#pragma once
#include <functional>
#include <string>

namespace ravex::net {

struct Progress {
    long long downloaded = 0;
    long long total = 0;
    std::string url;
};

std::string httpGet(const std::string& url, std::string* error);
std::string httpGetWithBearer(const std::string& url, const std::string& bearer, std::string* error);
std::string httpPost(const std::string& url, const std::string& body, const std::string& contentType, std::string* error);
bool downloadFile(const std::string& url, const std::wstring& dest, const std::function<void(const Progress&)>& progress, const bool* cancelled, std::string* error);
// burst download for many small files (mod icons): ONE long-lived session is
// reused by all callers (single TLS handshake, keep-alive connections), tight
// timeouts, a single attempt and a real HTTP status check.
bool downloadFileFast(const std::string& url, const std::wstring& dest);

}
