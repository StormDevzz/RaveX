#pragma once

#include "packet_types.hpp"
#include <string>
#include <cstring>

namespace packet {

struct Config {
    std::string interface = "any";
    std::string filter = "";
    std::string targetHost = "";
    int targetPort = 25565;
    bool captureCS = true;
    bool captureSC = true;
    bool showHex = false;
    bool showAscii = false;
    bool verbose = false;
    std::string outputFile;
    std::string outputFormat = "text";
    int snapLen = 65536;
    int timeout = 1000;

    static Config fromArgs(int argc, char** argv) {
        Config cfg;
        for (int i = 1; i < argc; ++i) {
            if (std::strcmp(argv[i], "-i") == 0 && i+1 < argc) cfg.interface = argv[++i];
            else if (std::strcmp(argv[i], "-f") == 0 && i+1 < argc) cfg.filter = argv[++i];
            else if (std::strcmp(argv[i], "-o") == 0 && i+1 < argc) cfg.outputFile = argv[++i];
            else if (std::strcmp(argv[i], "--hex") == 0) cfg.showHex = true;
            else if (std::strcmp(argv[i], "--verbose") == 0) cfg.verbose = true;
            else if (std::strcmp(argv[i], "--json") == 0) cfg.outputFormat = "json";
        }
        return cfg;
    }

    static Config defaults() { return Config(); }
};

}
