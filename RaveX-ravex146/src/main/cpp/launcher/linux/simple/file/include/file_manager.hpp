#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace file {


void ensure_directory(const std::string& path);


bool file_exists(const std::string& path);


bool create_directory(const std::string& path);


bool write_file(const std::string& path, const std::string& content);


std::string read_file(const std::string& path);


bool copy_file(const std::string& src, const std::string& dst);


bool remove_file(const std::string& path);

}
}
}
}
