#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {


bool download_client_jar(const std::string& url, const std::string& dest_path);


bool download_library_file(const std::string& url, const std::string& dest_path);

}
}
}
}
