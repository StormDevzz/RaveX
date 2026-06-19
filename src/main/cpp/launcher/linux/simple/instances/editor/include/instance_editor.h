#pragma once
#include <string>
#include <gtk/gtk.h>
#include "../../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace instance {

using InstanceInfo = simple::InstanceInfo;

bool show_instance_editor(GtkWindow* parent, InstanceInfo& info, bool createMode);

} // namespace instance
} // namespace simple
} // namespace launcher
} // namespace ravex
