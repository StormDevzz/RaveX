#pragma once
#include <string>
#include <vector>
#include "../../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace instance {

using InstanceInfo = simple::InstanceInfo;

std::vector<InstanceInfo> load_instances(const std::string& kickxDir);
InstanceInfo load_instance(const std::string& instanceDir);
void save_instance(const std::string& kickxDir, const InstanceInfo& inst);
void delete_instance(const std::string& kickxDir, const std::string& name);
InstanceInfo create_default_instance(const std::string& kickxDir);

} // namespace instance
} // namespace simple
} // namespace launcher
} // namespace ravex
