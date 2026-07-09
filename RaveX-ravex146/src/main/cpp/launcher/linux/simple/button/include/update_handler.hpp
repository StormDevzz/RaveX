#pragma once
#include "../../state/include/launcher_state.hpp"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace button {


struct UpdateTaskData {
    LauncherState *state;
    std::string remote_version;
    std::string download_url;
};

struct ProgressPulseData {
    GtkWidget *progress_bar;
    bool *active;
};

} 
} 
} 
} 
