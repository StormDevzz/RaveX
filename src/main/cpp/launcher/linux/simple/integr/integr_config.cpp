#include "include/integr_config.h"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool integrEnabled = true;
bool fabricEnabled = true;
bool forgeEnabled = false;

bool isIntegrEnabled() { return integrEnabled; }
bool isFabricEnabled() { return fabricEnabled; }
bool isForgeEnabled() { return forgeEnabled; }

void setIntegrEnabled(bool v) { integrEnabled = v; }
void setFabricEnabled(bool v) { fabricEnabled = v; }
void setForgeEnabled(bool v) { forgeEnabled = v; }

} 
} 
} 
} 
