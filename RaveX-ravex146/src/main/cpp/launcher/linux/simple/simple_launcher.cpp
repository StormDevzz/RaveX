#include "simple_launcher.hpp"
#include "state/include/launcher_state.hpp"
#include "instances/main/include/window_init.hpp"
#include "acc/include/account_manager.hpp"
#include "file/include/file_manager.hpp"
#include "file/include/path_provider.hpp"
#include "event/include/event_queue.hpp"
#include "analyze/include/crash_logger.hpp"
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {

void SimpleLauncher::run(const std::string& modsDir, const std::string& ravexDir) {
    std::string kickxDir = file::get_kickx_dir();

    file::ensure_directory(kickxDir);
    file::ensure_directory(kickxDir + "/versions");
    file::ensure_directory(kickxDir + "/libraries");
    file::ensure_directory(kickxDir + "/assets");
    std::string instDir = kickxDir + "/instances/default";
    file::ensure_directory(instDir);
    file::ensure_directory(instDir + "/mods");

    LauncherState *state = new LauncherState();
    state->mods_dir = instDir + "/mods";
    state->ravex_dir = ravexDir;
    state->kickx_dir = kickxDir;

    analyze::init_analyze(kickxDir);
    analyze::log_crash("=== launcher started ===");

    acc::load_accounts(state);

    window::init_and_run(state);

    analyze::shutdown_analyze();
    delete state;
}

}
}
}
