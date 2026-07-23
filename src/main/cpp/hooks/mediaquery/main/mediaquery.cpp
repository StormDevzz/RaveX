#include "mediaquery.hpp"

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <winuser.h>
#include <winhttp.h>
#include <string>
#include <vector>

namespace ravex {

static std::string getWindowTitle(HWND hwnd) {
    int len = GetWindowTextLengthW(hwnd);
    if (len <= 0) return {};
    std::vector<wchar_t> buf(len + 1);
    GetWindowTextW(hwnd, buf.data(), len + 1);
    int size = WideCharToMultiByte(CP_UTF8, 0, buf.data(), -1, nullptr, 0, nullptr, nullptr);
    if (size <= 0) return {};
    std::string out(size - 1, '\0');
    WideCharToMultiByte(CP_UTF8, 0, buf.data(), -1, out.data(), size, nullptr, nullptr);
    return out;
}

static bool isMediaPlayer(const std::string& cls) {
    std::string lower;
    lower.reserve(cls.size());
    for (char c : cls) lower.push_back(std::tolower(c));
    return lower.find("spotify") != std::string::npos ||
           lower.find("vlc") != std::string::npos ||
           lower.find("wmplayer") != std::string::npos ||
           lower.find("chrome") != std::string::npos ||
           lower.find("firefox") != std::string::npos ||
           lower.find("opera") != std::string::npos ||
           lower.find("music") != std::string::npos;
}

MediaInfo queryNowPlaying() {
    MediaInfo info{};
    info.valid = false;

    const char* targets[] = {
        "Spotify",
        "Spotify Premium",
        "Google Play Music",
        "YouTube Music",
        nullptr
    };

    HWND fg = GetForegroundWindow();
    if (fg) {
        char cls[256] = {};
        GetClassNameA(fg, cls, sizeof(cls));
        if (isMediaPlayer(cls)) {
            info.title = getWindowTitle(fg);
            info.status = "Playing";
            info.valid = true;
            return info;
        }
    }

    for (int i = 0; targets[i]; i++) {
        HWND hwnd = FindWindowA(nullptr, targets[i]);
        if (hwnd && IsWindowVisible(hwnd)) {
            info.title = getWindowTitle(hwnd);
            info.status = "Playing";
            info.valid = true;
            return info;
        }
    }

    HWND shell = GetShellWindow();
    if (shell) {
        HWND hwnd = nullptr;
        while ((hwnd = FindWindowExA(nullptr, hwnd, "MediaPlayer", nullptr)) != nullptr ||
               (hwnd = FindWindowExA(nullptr, hwnd, "Chrome_WidgetWin_1", nullptr)) != nullptr) {
            if (IsWindowVisible(hwnd) && GetWindowTextLengthW(hwnd) > 0) {
                info.title = getWindowTitle(hwnd);
                info.status = "Playing";
                info.valid = true;
                return info;
            }
        }
    }

    return info;
}

std::vector<uint8_t> downloadArt(const std::string& url) {
    if (url.empty()) return {};
    HINTERNET sess = WinHttpOpen(L"RaveX/1.0", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, nullptr, nullptr, 0);
    if (!sess) return {};
    URL_COMPONENTS uc = { sizeof(uc) };
    uc.dwHostNameLength = 1;
    uc.dwUrlPathLength = 1;
    WinHttpCrackUrl(std::wstring(url.begin(), url.end()).c_str(), 0, 0, &uc);
    std::wstring host(uc.lpszHostName, uc.dwHostNameLength);
    std::wstring path(uc.lpszUrlPath, uc.dwUrlPathLength);
    HINTERNET conn = WinHttpConnect(sess, host.c_str(), uc.nPort, 0);
    if (!conn) { WinHttpCloseHandle(sess); return {}; }
    HINTERNET req = WinHttpOpenRequest(conn, L"GET", path.c_str(), nullptr, nullptr, nullptr, uc.nScheme == 2 ? WINHTTP_FLAG_SECURE : 0);
    if (!req) { WinHttpCloseHandle(conn); WinHttpCloseHandle(sess); return {}; }
    std::vector<uint8_t> result;
    if (WinHttpSendRequest(req, WINHTTP_NO_ADDITIONAL_HEADERS, 0, nullptr, 0, 0, 0) && WinHttpReceiveResponse(req, nullptr)) {
        uint8_t buf[8192];
        DWORD read = 0;
        while (WinHttpReadData(req, buf, sizeof(buf), &read) && read > 0) {
            result.insert(result.end(), buf, buf + read);
        }
    }
    WinHttpCloseHandle(req); WinHttpCloseHandle(conn); WinHttpCloseHandle(sess);
    return result;
}

}
#else
#include <curl/curl.h>
#include <dbus/dbus.h>
#include <cstring>

static size_t writeCallback(char* data, size_t size, size_t nmemb, void* user) {
    auto* vec = static_cast<std::vector<uint8_t>*>(user);
    size_t total = size * nmemb;
    vec->insert(vec->end(), (uint8_t*)data, (uint8_t*)data + total);
    return total;
}

namespace ravex {

static std::string dbusStr(DBusMessageIter* iter) {
    if (!iter) return {};
    int type = dbus_message_iter_get_arg_type(iter);
    if (type != DBUS_TYPE_STRING && type != DBUS_TYPE_OBJECT_PATH) return {};
    const char* val = nullptr;
    dbus_message_iter_get_basic(iter, &val);
    return val ? std::string(val) : "";
}

static std::string getPlayerStatus(DBusConnection* conn, const char* player) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return "";

    const char* iface = "org.mpris.MediaPlayer2.Player";
    const char* prop = "PlaybackStatus";
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop,
                             DBUS_TYPE_INVALID);

    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);

    if (!reply) {
        dbus_error_free(&err);
        return "";
    }

    std::string result;
    DBusMessageIter iter, sub;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_VARIANT) {
        dbus_message_iter_recurse(&iter, &sub);
        result = dbusStr(&sub);
    }
    dbus_message_unref(reply);
    return result;
}

static std::string getMetadataStr(DBusConnection* conn, const char* player, const char* key) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return "";

    const char* iface = "org.mpris.MediaPlayer2.Player";
    const char* prop = "Metadata";
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop,
                             DBUS_TYPE_INVALID);

    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);

    if (!reply) {
        dbus_error_free(&err);
        return {};
    }

    std::string result;
    DBusMessageIter iter, sub, dict, entry, variant, val;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_VARIANT) {
        dbus_message_unref(reply);
        return {};
    }
    dbus_message_iter_recurse(&iter, &sub);
    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_ARRAY) {
        dbus_message_unref(reply);
        return {};
    }
    dbus_message_iter_recurse(&sub, &dict);
    while (dbus_message_iter_get_arg_type(&dict) == DBUS_TYPE_DICT_ENTRY) {
        dbus_message_iter_recurse(&dict, &entry);
        std::string entryKey = dbusStr(&entry);
        dbus_message_iter_next(&entry);

        if (dbus_message_iter_get_arg_type(&entry) == DBUS_TYPE_VARIANT) {
            dbus_message_iter_recurse(&entry, &variant);

            if (entryKey == key) {
                if (dbus_message_iter_get_arg_type(&variant) == DBUS_TYPE_STRING) {
                    result = dbusStr(&variant);
                } else if (dbus_message_iter_get_arg_type(&variant) == DBUS_TYPE_ARRAY) {
                    dbus_message_iter_recurse(&variant, &val);
                    if (dbus_message_iter_get_arg_type(&val) == DBUS_TYPE_STRING) {
                        result = dbusStr(&val);
                    }
                }
                dbus_message_unref(reply);
                return result;
            }
        }
        dbus_message_iter_next(&dict);
    }

    dbus_message_unref(reply);
    return result;
}

MediaInfo queryNowPlaying() {
    MediaInfo info{};
    info.valid = false;

    DBusError err;
    dbus_error_init(&err);
    DBusConnection* conn = dbus_bus_get(DBUS_BUS_SESSION, &err);
    if (!conn) {
        dbus_error_free(&err);
        return info;
    }

    DBusMessage* msg = dbus_message_new_method_call(
        "org.freedesktop.DBus", "/",
        "org.freedesktop.DBus", "ListNames");
    if (!msg) {
        dbus_connection_unref(conn);
        return info;
    }

    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 1000, &err);
    dbus_message_unref(msg);
    if (!reply) {
        dbus_error_free(&err);
        dbus_connection_unref(conn);
        return info;
    }

    std::vector<std::string> players;
    DBusMessageIter iter, sub;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_ARRAY) {
        dbus_message_iter_recurse(&iter, &sub);
        while (dbus_message_iter_get_arg_type(&sub) == DBUS_TYPE_STRING) {
            std::string name = dbusStr(&sub);
            if (name.find("org.mpris.MediaPlayer2.") == 0) {
                players.push_back(name);
            }
            dbus_message_iter_next(&sub);
        }
    }
    dbus_message_unref(reply);

    static const std::vector<std::string> blocked = {
        "tdesktop", "discord", "slack", "teams", "signal",
        "whatsapp", "zoom", "skype", "pidgin", "hexchat"
    };
    static const std::vector<std::string> priority = {
        "spotify", "spotifyd", "vlc", "mpv", "strawberry", "audacious",
        "rhythmbox", "clementine", "amarok", "deadbeef", "pragha",
        "firefox", "chromium", "chrome", "opera", "brave", "webkit",
    };

    int fallbackIdx = -1;
    int selectedIdx = -1;

    for (int i = 0; i < (int)players.size(); i++) {
        const auto& player = players[i];
        std::string status = getPlayerStatus(conn, player.c_str());
        if (status.empty() || status == "Stopped") continue;

        std::string playerName = player.substr(strlen("org.mpris.MediaPlayer2."));

        int blockedFlag = 0;
        for (const auto& b : blocked) {
            if (playerName.find(b) != std::string::npos) {
                blockedFlag = 1;
                break;
            }
        }
        if (blockedFlag) continue;

        int isPriority = 0;
        for (const auto& p : priority) {
            if (playerName.find(p) != std::string::npos) {
                isPriority = 1;
                break;
            }
        }
        if (isPriority) {
            selectedIdx = i;
            break;
        }

        if (fallbackIdx < 0) {
            fallbackIdx = i;
        }
    }

    int useIdx = selectedIdx >= 0 ? selectedIdx : fallbackIdx;
    if (useIdx >= 0) {
        const auto& player = players[useIdx];
        std::string status = getPlayerStatus(conn, player.c_str());
        info.title = getMetadataStr(conn, player.c_str(), "xesam:title");
        info.artist = getMetadataStr(conn, player.c_str(), "xesam:artist");
        info.album = getMetadataStr(conn, player.c_str(), "xesam:album");
        info.artUrl = getMetadataStr(conn, player.c_str(), "mpris:artUrl");
        info.status = status;
        info.valid = true;
    }

    dbus_connection_unref(conn);
    dbus_error_free(&err);
    return info;
}

std::vector<uint8_t> downloadArt(const std::string& url) {
    if (url.empty()) return {};
    std::vector<uint8_t> result;
    CURL* curl = curl_easy_init();
    if (!curl) return {};
    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &result);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 5L);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "RaveX/1.0");
    curl_easy_perform(curl);
    curl_easy_cleanup(curl);
    return result;
}

}
#endif
