#pragma once
#include "../../state/include/launcher_state.h"
#include <cstring>

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

struct ProgressMsg {
    LauncherState *state;
    char text[256];
    double fraction;
};

enum class EventKind {
    ProgressShow,
    ProgressUpdate,
    ProgressHide,
    LogMessage,
    GameStart,
    GameStop,
};

ProgressMsg *alloc_progress_msg(LauncherState *state, const char *text, double fraction);

} 
} 
} 
} 
