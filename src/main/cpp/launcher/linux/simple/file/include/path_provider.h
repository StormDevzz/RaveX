#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace file {

// возвращает абсолютный путь к папке .kickx в домашней директории
std::string get_kickx_dir();

// возвращает абсолютный путь к папке модов
std::string get_mods_dir();

} // namespace file
} // namespace simple
} // namespace launcher
} // namespace ravex
