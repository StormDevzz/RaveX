#pragma once
#include <string>
#include <vector>

namespace ravex {

struct ModuleGuiData {
    std::string name;
    bool enabled;
};

void start_gui(const std::vector<ModuleGuiData>& modules);
void update_gui_state(const std::string& name, bool enabled);
void stop_gui();

}
