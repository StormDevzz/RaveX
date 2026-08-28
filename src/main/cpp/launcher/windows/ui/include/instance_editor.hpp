#pragma once
#include <windows.h>
#include "core/include/config.hpp"

namespace ravex::ui {

bool showInstanceEditor(HWND parent, ravex::InstanceCfg& cfg, bool isNew);

}