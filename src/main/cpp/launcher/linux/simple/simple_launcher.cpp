#include "simple_launcher.h"
#include "state/include/launcher_state.h"
#include "instances/main/include/window_init.h"
#include "acc/include/account_manager.h"
#include "file/include/file_manager.h"
#include "file/include/path_provider.h"
#include "event/include/event_queue.h"
#include "analyze/include/crash_logger.h"
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
    file::ensure_directory(kickxDir + "/instances/default");

    LauncherState *state = new LauncherState();
    state->mods_dir = modsDir;
    state->ravex_dir = ravexDir;
    state->kickx_dir = kickxDir;

    analyze::init_analyze(kickxDir);
    analyze::log_crash("=== launcher started ===");

    acc::load_accounts(state);

    window::init_and_run(state);

    analyze::shutdown_analyze();
    delete state;
}

} // namespace simple
} // namespace launcher
} // namespace ravex
