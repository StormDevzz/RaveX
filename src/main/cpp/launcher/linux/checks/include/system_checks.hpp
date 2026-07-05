#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace checks {


std::string getKernelVersion();


bool isClientDownloaded(const std::string& modsDir, const std::string& version);

} 
} 
} 
