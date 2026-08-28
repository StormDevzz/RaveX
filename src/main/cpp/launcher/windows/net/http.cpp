#include "net/include/http.hpp"
#include "core/include/util.hpp"
#include <windows.h>
#include <winhttp.h>
#include <string>

namespace ravex::net {

namespace {

struct UrlParts {
    std::wstring host;
    std::wstring path;
    DWORD port = INTERNET_DEFAULT_HTTPS_PORT;
    bool secure = true;
};

bool crackUrl(const std::string& url, UrlParts& out) {
    std::wstring wurl = fromUtf8(url);
    URL_COMPONENTSW parts{};
    parts.dwStructSize = sizeof(parts);
    wchar_t host[256]{};
    wchar_t path[2048]{};
    parts.lpszHostName = host;
    parts.dwHostNameLength = 256;
    parts.lpszUrlPath = path;
    parts.dwUrlPathLength = 2048;
    if (!WinHttpCrackUrl(wurl.c_str(), static_cast<DWORD>(wurl.size()), 0, &parts)) return false;
    out.host = host;
    out.path = path;
    if (parts.lpszExtraInfo) out.path += parts.lpszExtraInfo;
    out.port = parts.nPort ? parts.nPort : INTERNET_DEFAULT_HTTPS_PORT;
    out.secure = (parts.nScheme == INTERNET_SCHEME_HTTPS);
    return true;
}

HINTERNET openSession() {
    return WinHttpOpen(L"kickx_launcher/1.0", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
                       WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
}

HINTERNET openConnection(HINTERNET session, const UrlParts& url) {
    return WinHttpConnect(session, url.host.c_str(), url.port, 0);
}

HINTERNET openRequest(HINTERNET conn, const wchar_t* method, const UrlParts& url) {
    DWORD flags = url.secure ? WINHTTP_FLAG_SECURE : 0;
    return WinHttpOpenRequest(conn, method, url.path.c_str(), nullptr, WINHTTP_NO_REFERER,
                              WINHTTP_DEFAULT_ACCEPT_TYPES, flags);
}

std::string readResponse(HINTERNET req) {
    std::string body;
    DWORD available = 0;
    do {
        available = 0;
        if (!WinHttpQueryDataAvailable(req, &available)) break;
        if (available == 0) break;
        std::string chunk(available, '\0');
        DWORD read = 0;
        if (!WinHttpReadData(req, chunk.data(), available, &read)) break;
        body.append(chunk.data(), read);
    } while (available > 0);
    return body;
}

std::string doRequest(const wchar_t* method, const std::string& url, const std::string& body,
                      const std::string& contentType, const std::string& bearer, std::string* error) {
    UrlParts parts;
    if (!crackUrl(url, parts)) {
        *error = "Invalid URL";
        return {};
    }
    HINTERNET session = openSession();
    if (!session) {
        *error = "Failed to initialize HTTP";
        return {};
    }
    DWORD resolveTimeout = 5000;
    DWORD connectTimeout = 10000;
    DWORD sendTimeout = 15000;
    DWORD receiveTimeout = 30000;
    WinHttpSetTimeouts(session, resolveTimeout, connectTimeout, sendTimeout, receiveTimeout);
    HINTERNET conn = openConnection(session, parts);
    if (!conn) {
        *error = "Failed to connect";
        WinHttpCloseHandle(session);
        return {};
    }
    HINTERNET req = openRequest(conn, method, parts);
    if (!req) {
        *error = "Failed to create request";
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return {};
    }
    std::wstring headers;
    headers += L"User-Agent: kickx_launcher/1.0\r\n";
    if (!bearer.empty()) headers += L"Authorization: Bearer " + fromUtf8(bearer) + L"\r\n";
    if (!contentType.empty()) headers += L"Content-Type: " + fromUtf8(contentType) + L"\r\n";
    if (url.find("api.github.com") != std::string::npos) {
        headers += L"Accept: application/vnd.github+json\r\n";
        headers += L"X-GitHub-Api-Version: 2022-11-28\r\n";
    } else {
        headers += L"Accept: application/json\r\n";
    }
    const void* bodyPtr = body.empty() ? WINHTTP_NO_REQUEST_DATA : body.data();
    DWORD bodyLen = body.empty() ? 0 : static_cast<DWORD>(body.size());
    if (!WinHttpSendRequest(req, headers.empty() ? WINHTTP_NO_ADDITIONAL_HEADERS : headers.c_str(),
                            static_cast<DWORD>(headers.size()), const_cast<void*>(bodyPtr), bodyLen, bodyLen, 0)) {
        *error = "Request failed";
        WinHttpCloseHandle(req);
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return {};
    }
    if (!WinHttpReceiveResponse(req, nullptr)) {
        *error = "No response";
        WinHttpCloseHandle(req);
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return {};
    }
    DWORD statusCode = 0;
    DWORD statusLen = sizeof(statusCode);
    WinHttpQueryHeaders(req, WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                        WINHTTP_HEADER_NAME_BY_INDEX, &statusCode, &statusLen, WINHTTP_NO_HEADER_INDEX);
    std::string result = readResponse(req);
    WinHttpCloseHandle(req);
    WinHttpCloseHandle(conn);
    WinHttpCloseHandle(session);
    if (statusCode < 200 || statusCode >= 300) {
        std::string msg = "HTTP " + std::to_string(statusCode);
        if (!result.empty()) {
            std::string preview = result.substr(0, 300);
            for (char& c : preview) if (c == '\r' || c == '\n') c = ' ';
            msg += ": " + preview;
            if (statusCode == 403 && url.find("api.github.com") != std::string::npos) {
                msg += " (GitHub rate-limit? Try again later or check https://api.github.com/rate_limit)";
            }
        }
        *error = msg;
        return {};
    }
    return result;
}

}

std::string httpGet(const std::string& url, std::string* error) {
    for (int attempt = 0; attempt < 3; ++attempt) {
        std::string result = doRequest(L"GET", url, {}, {}, {}, error);
        if (!result.empty()) return result;
        if (error && error->find("rate") != std::string::npos) return result;
        if (attempt < 2) Sleep(1500 * (attempt + 1));
    }
    return {};
}

std::string httpGetWithBearer(const std::string& url, const std::string& bearer, std::string* error) {
    return doRequest(L"GET", url, {}, {}, bearer, error);
}

std::string httpPost(const std::string& url, const std::string& body, const std::string& contentType, std::string* error) {
    return doRequest(L"POST", url, body, contentType, {}, error);
}

bool downloadFile(const std::string& url, const std::wstring& dest,
                  const std::function<void(const Progress&)>& progress, const bool* cancelled, std::string* error) {
    for (int attempt = 0; attempt < 3; ++attempt) {
        if (attempt > 0) {
            if (cancelled && *cancelled) {
                if (error) *error = "Cancelled";
                return false;
            }
            Sleep(1000 * attempt);
        }
        UrlParts parts;
        if (!crackUrl(url, parts)) {
            *error = "Invalid URL";
            return false;
        }
        HINTERNET session = openSession();
        if (!session) {
            *error = "Failed to initialize HTTP";
            return false;
        }
        WinHttpSetTimeouts(session, 5000, 10000, 15000, 60000);
        HINTERNET conn = openConnection(session, parts);
        if (!conn) {
            *error = "Failed to connect";
            WinHttpCloseHandle(session);
            continue;
        }
        HINTERNET req = openRequest(conn, L"GET", parts);
        if (!req) {
            *error = "Failed to create request";
            WinHttpCloseHandle(conn);
            WinHttpCloseHandle(session);
            continue;
        }
        std::wstring dlHeaders = L"User-Agent: kickx_launcher/1.0\r\n";
        if (!WinHttpSendRequest(req, dlHeaders.c_str(), static_cast<DWORD>(dlHeaders.size()),
                                WINHTTP_NO_REQUEST_DATA, 0, 0, 0)) {
            WinHttpCloseHandle(req);
            WinHttpCloseHandle(conn);
            WinHttpCloseHandle(session);
            continue;
        }
        if (!WinHttpReceiveResponse(req, nullptr)) {
            WinHttpCloseHandle(req);
            WinHttpCloseHandle(conn);
            WinHttpCloseHandle(session);
            continue;
        }
        DWORD contentLength = 0;
        DWORD clLen = sizeof(contentLength);
        WinHttpQueryHeaders(req, WINHTTP_QUERY_CONTENT_LENGTH | WINHTTP_QUERY_FLAG_NUMBER,
                            WINHTTP_HEADER_NAME_BY_INDEX, &contentLength, &clLen, WINHTTP_NO_HEADER_INDEX);
        HANDLE hFile = CreateFileW(dest.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS,
                                   FILE_ATTRIBUTE_NORMAL, nullptr);
        if (hFile == INVALID_HANDLE_VALUE) {
            *error = "Failed to create file";
            WinHttpCloseHandle(req);
            WinHttpCloseHandle(conn);
            WinHttpCloseHandle(session);
            continue;
        }
        long long total = contentLength > 0 ? contentLength : 0;
        long long downloaded = 0;
        bool ok = true;
        DWORD available = 0;
        while (true) {
            if (cancelled && *cancelled) {
                ok = false;
                break;
            }
            available = 0;
            if (!WinHttpQueryDataAvailable(req, &available)) break;
            if (available == 0) break;
            std::string chunk(available, '\0');
            DWORD read = 0;
            if (!WinHttpReadData(req, chunk.data(), available, &read)) {
                ok = false;
                break;
            }
            DWORD written = 0;
            if (!WriteFile(hFile, chunk.data(), read, &written, nullptr) || written != read) {
                ok = false;
                break;
            }
            downloaded += read;
            if (progress) {
                Progress p;
                p.downloaded = downloaded;
                p.total = total;
                p.url = url;
                progress(p);
            }
        }
        CloseHandle(hFile);
        WinHttpCloseHandle(req);
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        if (!ok) {
            DeleteFileW(dest.c_str());
            if (cancelled && *cancelled) {
                *error = "Cancelled";
                return false;
            }
            continue;
        }
        return true;
    }
    *error = "Download failed after retries";
    return false;
}

}
