#pragma once

#include "github_types.h"
#include <memory>
#include <functional>
#include <string>

namespace ravex {
namespace github {








class HttpClient {
public:
    HttpClient();
    ~HttpClient();

    HttpClient(const HttpClient&) = delete;
    HttpClient& operator=(const HttpClient&) = delete;

    
    void setToken(const std::string& token);
    void setUserAgent(const std::string& ua);
    void setTimeoutSec(int sec);
    void setLogCallback(LogCallback cb);

    
    HttpResponse request(const HttpRequest& req);

    
    HttpResponse get(const std::string& url,
                     std::map<std::string, std::string> extraHeaders = {});
    HttpResponse post(const std::string& url, const std::string& body,
                      std::map<std::string, std::string> extraHeaders = {});

    
    bool download(const std::string& url, const std::string& outputPath,
                  ProgressCallback progress = nullptr);

private:
    struct Impl;
    std::unique_ptr<Impl> m_impl;
};

} 
} 
