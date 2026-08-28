#pragma once
#include <cstddef>
#include <string>

namespace ravex {

std::string sha256File(const std::wstring& path);
std::string sha256Data(const void* data, std::size_t len);

}