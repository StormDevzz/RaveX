#pragma once
#include "../../state/include/launcher_state.h"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// функция асинхронной загрузки всех необходимых файлов minecraft
// возвращает true если все скачано успешно
bool download_minecraft_version(LauncherState *state, const std::string& version);

// получить путь к java на системе пользователя
std::string detect_java_path();

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex
