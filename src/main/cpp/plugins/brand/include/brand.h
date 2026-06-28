#pragma once
#include <string>

namespace ravex {
namespace plugins {
namespace brand {

class BrandFormatter {
public:
    static std::string formatBrand(const std::string& rawBrand);
};

} // namespace brand
} // namespace plugins
} // namespace ravex
