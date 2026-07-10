#pragma once
#include <string>
#include <vector>
#include <cstdint>

namespace ravex {

struct MediaInfo {
    std::string title;
    std::string artist;
    std::string album;
    std::string artUrl;
    std::string status;
    bool valid;
};

MediaInfo queryNowPlaying();
std::vector<uint8_t> downloadArt(const std::string& url);

}
