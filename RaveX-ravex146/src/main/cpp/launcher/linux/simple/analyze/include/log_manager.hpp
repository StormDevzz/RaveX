#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

void init_logs(const std::string &kickx_dir);
void write_log(const std::string &section, const std::string &message);
void write_log_nothrow(const std::string &section, const std::string &message);
std::string read_log(const std::string &section);
void rotate_logs(const std::string &section, size_t max_size);

} 
} 
} 
} 
