#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {


std::string http_get(const std::string& url);


bool http_download(const std::string& url, const std::string& dest_path);

}
}
}
}
