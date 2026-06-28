#pragma once
#include <string>
#include <cstdint>
#include <vector>

struct DiscordButton {
    std::string label;
    std::string url;
};

struct DiscordRichPresence {
    std::string state;
    std::string details;
    int64_t startTimestamp;
    std::string largeImageKey;
    std::string largeImageText;
    std::vector<DiscordButton> buttons;
};

class DiscordRPC {
public:
    static void initialize(const std::string& clientId);
    static void shutdown();
    static void updatePresence(const DiscordRichPresence& presence);
    static bool isConnected();
};
