#include "include/window_init.hpp"
#include "include/main_window.hpp"
#include "../../window/background/custom/include/ui_styles.hpp"
#include <gtk/gtk.h>
#include <signal.h>
#include <unistd.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void init_and_run(LauncherState* state) {
    int argc = 0;
    char **argv = nullptr;
    gtk_init(&argc, &argv);

    apply_css();
    create_window(state);
    gtk_widget_show_all(state->window);
    gtk_main();

    pid_t pid = state->game_pid.load();
    if (pid > 0) {
        kill(-pid, SIGKILL);
    }
}

}
}
}
}
