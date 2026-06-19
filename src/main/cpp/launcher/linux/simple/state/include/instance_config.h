#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace state {

// настройки инстанса игры
struct InstanceConfig {
    std::string name;
    std::string version;
    int ram_mb = 4096;
};

// загрузка конфигурации инстанса из файла
InstanceConfig load_instance_config(const std::string& path);

// сохранение конфигурации инстанса в файл
void save_instance_config(const std::string& path, const InstanceConfig& config);

} // namespace state
} // namespace simple
} // namespace launcher
} // namespace ravex
