#ifndef RAVEX_ANTIBOT_H
#define RAVEX_ANTIBOT_H

#include <string>
#include <cmath>
#include <cstring>
#include <regex>

namespace ravex {

struct BotAnalysis {
    double confidence; // 0.0 to 1.0
};

inline bool isSuspiciousName(const std::string& name) {
    std::string lower = name;
    for (auto& c : lower) c = std::tolower(c);

    const char* patterns[] = {"bot", "npc", "entity", "test", "dummy", "npc_", "bot_"};
    for (auto p : patterns) {
        if (lower.find(p) != std::string::npos) return true;
    }
    if (lower.length() > 24) return true;
    bool allDigits = true;
    for (auto c : lower) {
        if (!std::isdigit(c)) { allDigits = false; break; }
    }
    if (allDigits) return true;
    return false;
}

inline double analyzeMovement(double x, double y, double z, double mx, double my, double mz, int ticks) {
    if (ticks < 20) return 0.0;
    double hSpeed = std::sqrt(mx * mx + mz * mz);
    if (hSpeed < 0.001 && std::abs(my) < 0.001) return 0.6;
    if (hSpeed > 0.5) return 0.0;
    if (std::abs(my) > 0.5) return 0.3;
    return 0.0;
}

inline double analyze(const std::string& name, int ticks,
                      double x, double y, double z,
                      double mx, double my, double mz, double dist,
                      bool pingCheck, bool nameCheck, bool moveCheck)
{
    double confidence = 0.0;
    int factors = 0;

    if (nameCheck) {
        factors++;
        if (isSuspiciousName(name)) confidence += 0.4;
    }

    if (moveCheck) {
        factors++;
        confidence += analyzeMovement(x, y, z, mx, my, mz, ticks);
    }

    if (factors == 0) return 0.0;
    return confidence / static_cast<double>(factors);
}

} // namespace ravex

#endif
