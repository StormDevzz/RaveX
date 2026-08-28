#include "game/include/accounts.hpp"

#include <windows.h>
#include <winhttp.h>
#include <chrono>
#include <cctype>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <thread>
#include "core/include/util.hpp"
#include "net/include/http.hpp"

namespace ravex::game {

namespace {

const uint32_t kMd5K[64] = {
    0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
    0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
    0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
    0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
    0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
    0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
    0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
    0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
    0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};

const uint32_t kMd5S[64] = {
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21};

uint32_t rotl(uint32_t value, int bits) {
    return (value << bits) | (value >> (32 - bits));
}

void md5Transform(uint32_t state[4], const uint8_t block[64]) {
    uint32_t a = state[0];
    uint32_t b = state[1];
    uint32_t c = state[2];
    uint32_t d = state[3];
    uint32_t x[16];
    for (int i = 0; i < 16; ++i) {
        x[i] = static_cast<uint32_t>(block[i * 4]) |
               (static_cast<uint32_t>(block[i * 4 + 1]) << 8) |
               (static_cast<uint32_t>(block[i * 4 + 2]) << 16) |
               (static_cast<uint32_t>(block[i * 4 + 3]) << 24);
    }
    for (int i = 0; i < 64; ++i) {
        uint32_t f;
        int g;
        if (i < 16) {
            f = (b & c) | (~b & d);
            g = i;
        } else if (i < 32) {
            f = (d & b) | (~d & c);
            g = (5 * i + 1) % 16;
        } else if (i < 48) {
            f = b ^ c ^ d;
            g = (3 * i + 5) % 16;
        } else {
            f = c ^ (b | ~d);
            g = (7 * i) % 16;
        }
        uint32_t tmp = d;
        d = c;
        c = b;
        b = b + rotl(a + f + kMd5K[i] + x[g], static_cast<int>(kMd5S[i]));
        a = tmp;
    }
    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
}

std::string md5Hex(const std::string& input) {
    uint32_t state[4] = {0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476};
    size_t pos = 0;
    size_t len = input.size();
    while (pos + 64 <= len) {
        md5Transform(state, reinterpret_cast<const uint8_t*>(input.data()) + pos);
        pos += 64;
    }
    uint8_t tail[128];
    size_t rem = len - pos;
    std::memcpy(tail, input.data() + pos, rem);
    uint64_t bitLen = static_cast<uint64_t>(len) * 8;
    tail[rem] = 0x80;
    if (rem < 56) {
        std::memset(tail + rem + 1, 0, 56 - rem - 1);
        std::memcpy(tail + 56, &bitLen, 8);
        md5Transform(state, tail);
    } else {
        std::memset(tail + rem + 1, 0, 64 - rem - 1);
        md5Transform(state, tail);
        std::memset(tail, 0, 56);
        std::memcpy(tail + 56, &bitLen, 8);
        md5Transform(state, tail);
    }
    const char* hexDigits = "0123456789abcdef";
    std::string out;
    out.reserve(32);
    for (int word = 0; word < 4; ++word) {
        uint32_t value = state[word];
        for (int byte = 0; byte < 4; ++byte) {
            uint8_t b = static_cast<uint8_t>((value >> (byte * 8)) & 0xFF);
            out += hexDigits[b >> 4];
            out += hexDigits[b & 0x0F];
        }
    }
    return out;
}

std::string offlineUuid(const std::string& name) {
    std::string hex = md5Hex("OfflinePlayer:" + name);
    uint8_t bytes[16];
    for (int i = 0; i < 16; ++i) {
        int hi = std::isdigit(static_cast<unsigned char>(hex[i * 2]))
                     ? hex[i * 2] - '0'
                     : hex[i * 2] - 'a' + 10;
        int lo = std::isdigit(static_cast<unsigned char>(hex[i * 2 + 1]))
                     ? hex[i * 2 + 1] - '0'
                     : hex[i * 2 + 1] - 'a' + 10;
        bytes[i] = static_cast<uint8_t>((hi << 4) | lo);
    }
    bytes[6] = static_cast<uint8_t>((bytes[6] & 0x0F) | 0x30);
    bytes[8] = static_cast<uint8_t>((bytes[8] & 0x3F) | 0x80);
    const char* hexDigits = "0123456789abcdef";
    std::string out;
    out.reserve(36);
    for (int i = 0; i < 16; ++i) {
        if (i == 4 || i == 6 || i == 8 || i == 10) out += '-';
        out += hexDigits[bytes[i] >> 4];
        out += hexDigits[bytes[i] & 0x0F];
    }
    return out;
}

void skipWs(const std::string& text, size_t& i) {
    while (i < text.size() &&
           (text[i] == ' ' || text[i] == '\t' || text[i] == '\n' || text[i] == '\r')) {
        ++i;
    }
}

bool findKey(const std::string& text, const std::string& key, size_t& out) {
    std::string needle = "\"" + key + "\"";
    size_t pos = 0;
    while ((pos = text.find(needle, pos)) != std::string::npos) {
        size_t colon = pos + needle.size();
        skipWs(text, colon);
        if (colon < text.size() && text[colon] == ':') {
            out = colon + 1;
            return true;
        }
        pos = colon;
    }
    return false;
}

std::string readValue(const std::string& text, size_t& i) {
    skipWs(text, i);
    if (i >= text.size()) return "";
    char first = text[i];
    if (first == '"') {
        ++i;
        std::string out;
        while (i < text.size() && text[i] != '"') {
            if (text[i] == '\\' && i + 1 < text.size()) {
                char escaped = text[i + 1];
                switch (escaped) {
                    case 'n':
                        out += '\n';
                        i += 2;
                        break;
                    case 'r':
                        out += '\r';
                        i += 2;
                        break;
                    case 't':
                        out += '\t';
                        i += 2;
                        break;
                    case 'b':
                        out += '\b';
                        i += 2;
                        break;
                    case 'f':
                        out += '\f';
                        i += 2;
                        break;
                    case 'u': {
                        if (i + 5 < text.size()) {
                            unsigned int code = 0;
                            for (int k = 1; k <= 4; ++k) {
                                char h = text[i + 1 + k];
                                code <<= 4;
                                if (h >= '0' && h <= '9') code |= static_cast<unsigned int>(h - '0');
                                else if (h >= 'a' && h <= 'f') code |= static_cast<unsigned int>(h - 'a' + 10);
                                else if (h >= 'A' && h <= 'F') code |= static_cast<unsigned int>(h - 'A' + 10);
                            }
                            if (code < 0x80) {
                                out += static_cast<char>(code);
                            } else if (code < 0x800) {
                                out += static_cast<char>(0xC0 | (code >> 6));
                                out += static_cast<char>(0x80 | (code & 0x3F));
                            } else {
                                out += static_cast<char>(0xE0 | (code >> 12));
                                out += static_cast<char>(0x80 | ((code >> 6) & 0x3F));
                                out += static_cast<char>(0x80 | (code & 0x3F));
                            }
                            i += 6;
                        } else {
                            i += 2;
                        }
                        break;
                    }
                    default:
                        out += escaped;
                        i += 2;
                        break;
                }
            } else {
                out += text[i];
                ++i;
            }
        }
        if (i < text.size()) ++i;
        return out;
    }
    if (first == '{' || first == '[') {
        int depth = 0;
        bool inString = false;
        size_t start = i;
        while (i < text.size()) {
            char c = text[i];
            if (inString) {
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == '"') inString = false;
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{' || c == '[') {
                    ++depth;
                } else if (c == '}' || c == ']') {
                    --depth;
                    if (depth == 0) {
                        ++i;
                        break;
                    }
                }
            }
            ++i;
        }
        return text.substr(start, i - start);
    }
    size_t start = i;
    while (i < text.size() && text[i] != ',' && text[i] != '}' && text[i] != ']') {
        ++i;
    }
    return text.substr(start, i - start);
}

bool jsonGetString(const std::string& json, const std::string& key, std::string& out) {
    size_t pos;
    if (!findKey(json, key, pos)) return false;
    out = readValue(json, pos);
    return true;
}

bool jsonGetInt(const std::string& json, const std::string& key, long long& out) {
    size_t pos;
    if (!findKey(json, key, pos)) return false;
    std::string value = readValue(json, pos);
    if (value.empty()) return false;
    char* end = nullptr;
    long long parsed = std::strtoll(value.c_str(), &end, 10);
    if (end == value.c_str()) return false;
    out = parsed;
    return true;
}

std::string jsonFirstArrayElement(const std::string& json) {
    size_t pos = 0;
    skipWs(json, pos);
    if (pos >= json.size() || json[pos] != '[') return "";
    size_t i = pos + 1;
    skipWs(json, i);
    return readValue(json, i);
}

std::string urlEncode(const std::string& input) {
    const char* hexDigits = "0123456789ABCDEF";
    std::string out;
    out.reserve(input.size());
    for (unsigned char c : input) {
        if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
            out += static_cast<char>(c);
        } else {
            out += '%';
            out += hexDigits[c >> 4];
            out += hexDigits[c & 0x0F];
        }
    }
    return out;
}

std::string urlDecode(const std::string& input) {
    std::string out;
    out.reserve(input.size());
    for (size_t i = 0; i < input.size(); ++i) {
        if (input[i] == '%' && i + 2 < input.size()) {
            auto hexVal = [](char c) -> int {
                if (c >= '0' && c <= '9') return c - '0';
                if (c >= 'a' && c <= 'f') return c - 'a' + 10;
                if (c >= 'A' && c <= 'F') return c - 'A' + 10;
                return -1;
            };
            int hi = hexVal(input[i + 1]);
            int lo = hexVal(input[i + 2]);
            if (hi >= 0 && lo >= 0) {
                out += static_cast<char>((hi << 4) | lo);
                i += 2;
                continue;
            }
        }
        out += input[i];
    }
    return out;
}

std::string httpGetWithBearer(const std::string& url, const std::string& bearer, std::string* error) {
    std::string result;
    HINTERNET session = WinHttpOpen(L"RaveX Launcher/1.0", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
                                    WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (!session) {
        *error = "Failed to initialize HTTP";
        return result;
    }
    URL_COMPONENTSW parts{};
    parts.dwStructSize = sizeof(parts);
    std::wstring wurl = fromUtf8(url);
    wchar_t host[256];
    wchar_t path[1024];
    parts.lpszHostName = host;
    parts.dwHostNameLength = 256;
    parts.lpszUrlPath = path;
    parts.dwUrlPathLength = 1024;
    if (!WinHttpCrackUrl(wurl.c_str(), static_cast<DWORD>(wurl.size()), 0, &parts)) {
        *error = "Invalid URL";
        WinHttpCloseHandle(session);
        return result;
    }
    DWORD port = parts.nPort ? parts.nPort : INTERNET_DEFAULT_HTTPS_PORT;
    HINTERNET conn = WinHttpConnect(session, parts.lpszHostName, port, 0);
    if (!conn) {
        *error = "Failed to connect";
        WinHttpCloseHandle(session);
        return result;
    }
    std::wstring target = parts.lpszUrlPath ? parts.lpszUrlPath : L"/";
    if (parts.lpszExtraInfo) target += parts.lpszExtraInfo;
    HINTERNET req = WinHttpOpenRequest(conn, L"GET", target.c_str(), nullptr, WINHTTP_NO_REFERER,
                                       WINHTTP_DEFAULT_ACCEPT_TYPES, WINHTTP_FLAG_SECURE);
    if (!req) {
        *error = "Failed to create request";
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return result;
    }
    std::wstring header = L"Authorization: Bearer " + fromUtf8(bearer) + L"\r\nAccept: application/json\r\n";
    if (!WinHttpSendRequest(req, header.c_str(), static_cast<DWORD>(header.size()),
                            WINHTTP_NO_REQUEST_DATA, 0, 0, 0)) {
        *error = "Request failed";
        WinHttpCloseHandle(req);
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return result;
    }
    if (!WinHttpReceiveResponse(req, nullptr)) {
        *error = "No response";
        WinHttpCloseHandle(req);
        WinHttpCloseHandle(conn);
        WinHttpCloseHandle(session);
        return result;
    }
    DWORD statusCode = 0;
    DWORD statusLen = sizeof(statusCode);
    WinHttpQueryHeaders(req, WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                        WINHTTP_HEADER_NAME_BY_INDEX, &statusCode, &statusLen, WINHTTP_NO_HEADER_INDEX);
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
    WinHttpCloseHandle(req);
    WinHttpCloseHandle(conn);
    WinHttpCloseHandle(session);
    if (statusCode != 200) {
        *error = "Profile request failed (HTTP " + std::to_string(statusCode) + ")";
        return "";
    }
    return body;
}

}

bool createOfflineAccount(const std::string& name, ravex::Account* out) {
    if (!out || name.empty()) return false;
    out->name = name;
    out->uuid = offlineUuid(name);
    out->accessToken = "00000000-0000-0000-0000-000000000000";
    out->type = "offline";
    return true;
}

bool loginMicrosoft(ravex::Account* out, std::string* error,
                    const std::function<void(const std::string&)>& status) {
    if (!out || !error) return false;
    error->clear();
    std::string err;

    status("Requesting device code...");
    const std::string clientId = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
    std::string deviceBody = "client_id=" + clientId + "&scope=" + urlEncode("XboxLive.signin offline_access");
    std::string deviceResp = ravex::net::httpPost(
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
        deviceBody, "application/x-www-form-urlencoded", &err);
    if (deviceResp.empty()) {
        *error = err.empty() ? "Failed to request device code" : err;
        return false;
    }

    std::string userCode;
    std::string deviceCode;
    std::string verificationUri;
    long long expiresIn = 900;
    long long interval = 5;
    jsonGetString(deviceResp, "user_code", userCode);
    jsonGetString(deviceResp, "device_code", deviceCode);
    jsonGetString(deviceResp, "verification_uri", verificationUri);
    jsonGetInt(deviceResp, "expires_in", expiresIn);
    jsonGetInt(deviceResp, "interval", interval);
    if (deviceCode.empty() || userCode.empty()) {
        *error = "Invalid device code response";
        return false;
    }
    if (expiresIn <= 0 || expiresIn > 900) expiresIn = 900;
    if (interval < 5) interval = 5;

    if (!verificationUri.empty()) {
        ShellExecuteW(nullptr, L"open", fromUtf8(verificationUri).c_str(), nullptr, nullptr, SW_SHOWNORMAL);
    }
    status("Open browser and enter code: " + userCode);

    auto start = std::chrono::steady_clock::now();
    long long pollInterval = interval;
    std::string accessToken;
    bool authorized = false;
    while (!authorized) {
        long long elapsed = std::chrono::duration_cast<std::chrono::seconds>(
                                std::chrono::steady_clock::now() - start)
                                .count();
        if (elapsed >= expiresIn) {
            *error = "Device code expired";
            return false;
        }
        std::string tokenBody = "grant_type=urn:ietf:params:oauth:grant-type:device_code"
                                "&client_id=" + clientId +
                                "&device_code=" + urlEncode(deviceCode);
        std::string tokenResp = ravex::net::httpPost(
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
            tokenBody, "application/x-www-form-urlencoded", &err);
        if (tokenResp.empty()) {
            *error = err.empty() ? "Token request failed" : err;
            return false;
        }
        std::string errCode;
        jsonGetString(tokenResp, "error", errCode);
        if (errCode.empty()) {
            if (!jsonGetString(tokenResp, "access_token", accessToken) || accessToken.empty()) {
                *error = "Invalid token response";
                return false;
            }
            authorized = true;
            break;
        }
        if (errCode == "authorization_pending") {
        } else if (errCode == "slow_down") {
            pollInterval += 5;
        } else if (errCode == "authorization_declined") {
            *error = "Authorization cancelled";
            return false;
        } else if (errCode == "expired_token") {
            *error = "Device code expired";
            return false;
        } else if (errCode == "bad_verification_code") {
            *error = "Invalid credentials";
            return false;
        } else {
            std::string desc;
            jsonGetString(tokenResp, "error_description", desc);
            *error = "Token error: " + errCode +
                     (desc.empty() ? "" : (" - " + urlDecode(desc)));
            return false;
        }
        std::this_thread::sleep_for(std::chrono::seconds(pollInterval));
    }

    status("Authenticating with Xbox Live...");
    std::string xblBody = "{\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\","
                          "\"Properties\":{\"AuthMethod\":\"RPS\","
                          "\"SiteName\":\"user.auth.xboxlive.com\","
                          "\"RpsTicket\":\"d=" + accessToken + "\"}}";
    std::string xblResp = ravex::net::httpPost(
        "https://user.auth.xboxlive.com/user/authenticate", xblBody, "application/json", &err);
    if (xblResp.empty()) {
        *error = err.empty() ? "Xbox Live authentication failed" : err;
        return false;
    }
    std::string xblToken;
    if (!jsonGetString(xblResp, "Token", xblToken) || xblToken.empty()) {
        *error = "Invalid Xbox Live response";
        return false;
    }

    status("Authenticating with Xbox Live (XSTS)...");
    std::string xstsBody = "{\"RelyingParty\":\"rp://api.minecraftservices.com/\","
                           "\"TokenType\":\"JWT\","
                           "\"Properties\":{\"SandboxId\":\"RETAIL\","
                           "\"UserTokens\":[\"" + xblToken + "\"]}}";
    std::string xstsResp = ravex::net::httpPost(
        "https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody, "application/json", &err);
    if (xstsResp.empty()) {
        if (err.find("401") != std::string::npos) {
            *error = "Xbox Live authentication failed: this account has no Xbox Live profile";
        } else {
            *error = err.empty() ? "XSTS authorization failed" : err;
        }
        return false;
    }
    std::string xstsToken;
    std::string uhs;
    if (!jsonGetString(xstsResp, "Token", xstsToken) || xstsToken.empty()) {
        *error = "Invalid XSTS response";
        return false;
    }
    std::string claims;
    if (jsonGetString(xstsResp, "DisplayClaims", claims)) {
        std::string xui;
        if (jsonGetString(claims, "xui", xui)) {
            std::string first = jsonFirstArrayElement(xui);
            if (!first.empty()) jsonGetString(first, "uhs", uhs);
        }
    }
    if (uhs.empty()) {
        *error = "Invalid XSTS response (missing user hash)";
        return false;
    }

    status("Requesting Minecraft token...");
    std::string mcBody = "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}";
    std::string mcResp = ravex::net::httpPost(
        "https://api.minecraftservices.com/authentication/login_with_xbox",
        mcBody, "application/json", &err);
    if (mcResp.empty()) {
        *error = err.empty() ? "Minecraft authentication failed" : err;
        return false;
    }
    std::string mcToken;
    if (!jsonGetString(mcResp, "access_token", mcToken) || mcToken.empty()) {
        *error = "Invalid Minecraft token response";
        return false;
    }

    status("Fetching Minecraft profile...");
    std::string profile = httpGetWithBearer(
        "https://api.minecraftservices.com/minecraft/profile", mcToken, &err);
    if (profile.empty()) {
        *error = err.empty() ? "Failed to fetch Minecraft profile" : err;
        return false;
    }
    std::string profileId;
    std::string profileName;
    if (!jsonGetString(profile, "id", profileId) || profileId.empty() ||
        !jsonGetString(profile, "name", profileName) || profileName.empty()) {
        *error = "Invalid profile response";
        return false;
    }

    std::string formattedUuid;
    formattedUuid.reserve(36);
    for (size_t i = 0; i < profileId.size(); ++i) {
        if (i == 8 || i == 12 || i == 16 || i == 20) formattedUuid += '-';
        formattedUuid += static_cast<char>(std::tolower(static_cast<unsigned char>(profileId[i])));
    }

    out->name = profileName;
    out->uuid = formattedUuid;
    out->accessToken = mcToken;
    out->type = "microsoft";
    status("Logged in as " + profileName);
    return true;
}

}