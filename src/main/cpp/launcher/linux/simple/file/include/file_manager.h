#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace file {

// гарантирует существование директории на системе
void ensure_directory(const std::string& path);

// проверяет существование файла
bool file_exists(const std::string& path);

// создаёт директорию (включая родительские)
bool create_directory(const std::string& path);

// записывает строку в файл (создаёт если нет)
bool write_file(const std::string& path, const std::string& content);

// читает весь файл в строку
std::string read_file(const std::string& path);

// копирует файл
bool copy_file(const std::string& src, const std::string& dst);

// удаляет файл
bool remove_file(const std::string& path);

} // namespace file
} // namespace simple
} // namespace launcher
} // namespace ravex
