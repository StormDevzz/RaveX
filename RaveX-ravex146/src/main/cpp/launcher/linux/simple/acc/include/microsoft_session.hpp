#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

struct MicrosoftSession {
    std::string refresh_token;
    std::string access_token;
    long long expires_in = 0;
};

}
}
}
}
