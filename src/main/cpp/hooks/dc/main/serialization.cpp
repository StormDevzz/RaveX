#include "serialization.h"
#include <sstream>

namespace DiscordSerialization {

static std::string escapeJson(const std::string& str) {
    std::ostringstream ss;
    for (char c : str) {
        switch (c) {
            case '"':  ss << "\\\""; break;
            case '\\': ss << "\\\\"; break;
            case '\n': ss << "\\n"; break;
            case '\r': ss << "\\r"; break;
            case '\t': ss << "\\t"; break;
            default:
                if (c >= 0 && c < 32) {
                    // Control character
                } else {
                    ss << c;
                }
                break;
        }
    }
    return ss.str();
}

std::string serializeHandshake(const std::string& clientId) {
    return "{\"v\":1,\"client_id\":\"" + escapeJson(clientId) + "\"}";
}

std::string serializeActivity(const DiscordRichPresence& presence, int pid, const std::string& nonce) {
    std::string timestampBlock = "";
    if (presence.startTimestamp > 0) {
        timestampBlock = ",\"timestamps\":{\"start\":" + std::to_string(presence.startTimestamp) + "}";
    }

    std::string detailsStr = escapeJson(presence.details);
    std::string stateStr = escapeJson(presence.state);

    std::string buttonsBlock = "";
    if (!presence.buttons.empty()) {
        buttonsBlock = ",\"buttons\":[";
        size_t limit = presence.buttons.size();
        if (limit > 2) limit = 2;
        for (size_t i = 0; i < limit; i++) {
            if (i > 0) buttonsBlock += ",";
            buttonsBlock += "{\"label\":\"" + escapeJson(presence.buttons[i].label)
                + "\",\"url\":\"" + escapeJson(presence.buttons[i].url) + "\"}";
        }
        buttonsBlock += "]";
    }

    return "{"
        "\"cmd\":\"SET_ACTIVITY\","
        "\"args\":{"
            "\"pid\":" + std::to_string(pid) + ","
            "\"activity\":{"
                "\"details\":\"" + detailsStr + "\","
                "\"state\":\"" + stateStr + "\""
                + timestampBlock +
                ",\"assets\":{"
                    "\"large_image\":\"" + escapeJson(presence.largeImageKey) + "\","
                    "\"large_text\":\"" + escapeJson(presence.largeImageText) + "\""
                "}"
                + buttonsBlock +
            "}"
        "},"
        "\"nonce\":\"" + escapeJson(nonce) + "\""
    "}";
}

std::string serializeClearActivity(int pid, const std::string& nonce) {
    return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + std::to_string(pid) + "},\"nonce\":\"" + escapeJson(nonce) + "\"}";
}

}
