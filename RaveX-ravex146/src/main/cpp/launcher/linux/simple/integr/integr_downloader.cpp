#include "include/integr_utils.hpp"
#include <vector>
#include <string>
#include <cstdlib>
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {


static void parallelDownloadLibs(const std::vector<std::string>& urls,
                                  const std::vector<std::string>& dests) {
    for (size_t i = 0; i < urls.size(); i += 16) {
        std::string batch;
        size_t end = std::min(i + 16, urls.size());
        for (size_t j = i; j < end; j++) {
            struct stat st;
            if (stat(dests[j].c_str(), &st) == 0) continue;
            size_t slash = dests[j].find_last_of('/');
            if (slash != std::string::npos) {
                batch += "mkdir -p \"" + dests[j].substr(0, slash) + "\" 2>/dev/null\n";
            }
            batch += "curl -sL -o \"" + dests[j] + "\" \"" + urls[j] + "\" 2>/dev/null &\n";
        }
        if (!batch.empty()) {
            batch += "wait\n";
            system(batch.c_str());
        }
    }
}

}
}
}
}
