#include "brand.hpp"
#include <algorithm>

namespace ravex {
namespace plugins {
namespace brand {

std::string BrandFormatter::formatBrand(const std::string& rawBrand) {
    if (rawBrand.empty()) {
        return "Unknown";
    }

    std::string brand = rawBrand;
    std::string lowerBrand = brand;
    std::transform(lowerBrand.begin(), lowerBrand.end(), lowerBrand.begin(), ::tolower);

    if (lowerBrand.find("vanilla") != std::string::npos) {
        return "\u00A7fVanilla";
    } else if (lowerBrand.find("paper") != std::string::npos) {
        return "\u00A7bPaper";
    } else if (lowerBrand.find("spigot") != std::string::npos) {
        return "\u00A7eSpigot";
    } else if (lowerBrand.find("purpur") != std::string::npos) {
        return "\u00A7dPurpur";
    } else if (lowerBrand.find("velocity") != std::string::npos) {
        return "\u00A73Velocity";
    } else if (lowerBrand.find("bungeecord") != std::string::npos) {
        return "\u00A7cBungeeCord";
    } else if (lowerBrand.find("fabric") != std::string::npos) {
        return "\u00A7eFabric";
    } else if (lowerBrand.find("forge") != std::string::npos) {
        return "\u00A76Forge";
    }

    if (!brand.empty() && brand[0] >= 'a' && brand[0] <= 'z') {
        brand[0] = brand[0] - 'a' + 'A';
    }
    return "\u00A77" + brand;
}

}
}
}
