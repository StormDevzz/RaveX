#pragma once
#include <string>
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool setupIntegrations(const std::string& kickxDir, const std::string& mcVersion);
bool needsFabricSetup(const std::string& kickxDir);
bool needsForgeSetup(const std::string& kickxDir);

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
