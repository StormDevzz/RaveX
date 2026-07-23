#include "mediaquery.h"
#include "dbus_util.h"
#include "fs_util.h"
#include <dbus/dbus.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

MediaInfo* mediaquery_query(void) {
    DBusError err;
    dbus_error_init(&err);
    DBusConnection* conn = dbus_bus_get(DBUS_BUS_SESSION, &err);
    if (!conn) { dbus_error_free(&err); return NULL; }

    DBusMessage* msg = dbus_message_new_method_call(
        "org.freedesktop.DBus", "/",
        "org.freedesktop.DBus", "ListNames");
    if (!msg) { dbus_connection_unref(conn); return NULL; }

    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 1000, &err);
    dbus_message_unref(msg);
    if (!reply) { dbus_error_free(&err); dbus_connection_unref(conn); return NULL; }

    char** names = NULL;
    int nnames = 0;
    if (!dbus_message_get_args(reply, &err, DBUS_TYPE_ARRAY, DBUS_TYPE_STRING, &names, &nnames, DBUS_TYPE_INVALID)) {
        dbus_message_unref(reply);
        dbus_error_free(&err);
        dbus_connection_unref(conn);
        return NULL;
    }

    static const char* priority[] = {
        "spotify", "spotifyd", "vlc", "mpv", "strawberry", "audacious",
        "rhythmbox", "clementine", "amarok", "deadbeef", "pragha",
        "qmmp", "cantata", "nuvola", "yarock", "sayonara",
        "kodi", "plex", "emby", "jellyfin",
        "firefox", "chromium", "chrome", "opera", "brave", "webkit",
        "plasma-browser-integration",
        NULL
    };
    static const char* blocked[] = {
        "tdesktop", "discord", "slack", "teams", "signal",
        "whatsapp", "zoom", "skype", "pidgin", "hexchat",
        NULL
    };

    MediaInfo* fallback = NULL;
    MediaInfo* info = NULL;

    for (int i = 0; i < nnames; i++) {
        if (strstr(names[i], "org.mpris.MediaPlayer2.") != names[i]) continue;
        char* status = get_prop(conn, names[i], "org.mpris.MediaPlayer2.Player", "PlaybackStatus");
        if (!status || strcmp(status, "Stopped") == 0) { free(status); continue; }

        const char* prefix = "org.mpris.MediaPlayer2.";
        const char* shortName = names[i] + strlen(prefix);

        char* desktopEntry = get_prop(conn, names[i], "org.mpris.MediaPlayer2", "DesktopEntry");
        const char* playerName = desktopEntry ? desktopEntry : shortName;

        int blockedIdx = 0;
        int isBlocked = 0;
        while (blocked[blockedIdx]) {
            if (strstr(playerName, blocked[blockedIdx]) != NULL) {
                isBlocked = 1;
                break;
            }
            blockedIdx++;
        }
        if (isBlocked) {
            free(desktopEntry);
            free(status);
            continue;
        }

        MediaInfo* candidate = (MediaInfo*)calloc(1, sizeof(MediaInfo));
        if (!candidate) { free(desktopEntry); free(status); break; }
        candidate->title = get_metadata_str(conn, names[i], "xesam:title");
        candidate->artist = get_metadata_str(conn, names[i], "xesam:artist");
        candidate->album = get_metadata_str(conn, names[i], "xesam:album");
        candidate->artUrl = get_metadata_str(conn, names[i], "mpris:artUrl");
        candidate->playerName = desktopEntry ? desktopEntry : strdup(shortName);
        if (!candidate->playerName && shortName) candidate->playerName = strdup(shortName);
        candidate->status = status;
        candidate->position = get_prop_int64(conn, names[i], "org.mpris.MediaPlayer2.Player", "Position");
        candidate->length = get_metadata_int64(conn, names[i], "mpris:length");
        if (candidate->position < 0) candidate->position = 0;
        if (candidate->length < 0) candidate->length = 0;
        candidate->valid = (candidate->title != NULL);

        int priorityIdx = 0;
        int matchedPriority = 0;
        while (priority[priorityIdx]) {
            if (strstr(playerName, priority[priorityIdx]) != NULL) {
                matchedPriority = 1;
                break;
            }
            priorityIdx++;
        }
        if (matchedPriority) {
            if (info) mediaquery_free(info);
            info = candidate;
            break;
        }

        if (!fallback) {
            fallback = candidate;
        } else {
            mediaquery_free(candidate);
        }
    }

    if (!info && fallback) {
        info = fallback;
    }

    dbus_message_unref(reply);
    dbus_connection_unref(conn);
    dbus_error_free(&err);
    return info;
}

void mediaquery_free(MediaInfo* info) {
    if (!info) return;
    free(info->title); free(info->artist); free(info->album);
    free(info->artUrl); free(info->playerName); free(info->status);
    free(info);
}

uint8_t* mediaquery_extract_icon(const char* de, size_t* out_len) {
    char* path = find_icon_file(de);
    if (!path) { *out_len = 0; return NULL; }
    uint8_t* data = read_file(path, out_len);
    free(path);
    return data;
}

/* ---- merged from image_util.c ---- */

uint8_t* decode_to_rgba(const uint8_t* data, size_t data_len, int* w, int* h) {
    if (!data || data_len == 0) return NULL;
    int n = 0;
    return stbi_load_from_memory(data, (int)data_len, w, h, &n, STBI_rgb_alpha);
}

uint8_t* resize_rgba(const uint8_t* rgba, int w, int h, int tw, int th) {
    if (!rgba || tw <= 0 || th <= 0) return NULL;
    uint8_t* out = (uint8_t*)calloc((size_t)tw * th, 4);
    if (!out) return NULL;
    for (int y = 0; y < th; y++) {
        for (int x = 0; x < tw; x++) {
            int sx = x * w / tw;
            int sy = y * h / th;
            if (sx >= w) sx = w - 1;
            if (sy >= h) sy = h - 1;
            int si = (sy * w + sx) * 4;
            int di = (y * tw + x) * 4;
            out[di + 0] = rgba[si + 0];
            out[di + 1] = rgba[si + 1];
            out[di + 2] = rgba[si + 2];
            out[di + 3] = rgba[si + 3];
        }
    }
    return out;
}

void free_image(uint8_t* pixels) {
    if (pixels) stbi_image_free(pixels);
}

/* ---- merged from mediaquery_art.c ---- */

static size_t write_cb(void* data, size_t size, size_t nmemb, void* user) {
    size_t* ctx = (size_t*)user;
    size_t total = size * nmemb;
    size_t cap = ctx[0], used = ctx[1];
    uint8_t* buf = (uint8_t*)ctx[2];
    if (used + total > cap) {
        size_t newcap = cap ? cap * 2 : 65536;
        if (newcap < used + total) newcap = used + total + 4096;
        uint8_t* nd = realloc(buf, newcap);
        if (!nd) return 0;
        buf = nd;
        ctx[0] = newcap;
        ctx[2] = (size_t)buf;
    }
    memcpy(buf + used, data, total);
    ctx[1] = used + total;
    return total;
}

uint8_t* mediaquery_download_art(const char* url, size_t* out_len) {
    *out_len = 0;
    if (!url || !*url) return NULL;
    if (strncmp(url, "file://", 7) == 0) {
        const char* path = url + 7;
        return read_file(path, out_len);
    }
    CURL* curl = curl_easy_init();
    if (!curl) return NULL;
    size_t ctx[3] = {65536, 0, 0};
    uint8_t* buf = malloc(ctx[0]);
    if (!buf) { curl_easy_cleanup(curl); return NULL; }
    ctx[2] = (size_t)buf;
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, ctx);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 5L);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "RaveX/1.0");
    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);
    if (res != CURLE_OK) { free((void*)ctx[2]); return NULL; }
    *out_len = ctx[1];
    return (uint8_t*)ctx[2];
}
