#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace checks {

// получает версию ядра linux
std::string getKernelVersion();

// проверяет скачан ли клиент определенной версии
bool isClientDownloaded(const std::string& modsDir, const std::string& version);

} // namespace checks
} // namespace launcher
} // namespace ravex
