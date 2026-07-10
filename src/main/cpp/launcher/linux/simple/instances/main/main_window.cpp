#include "include/main_window.hpp"
#include "include/window_layout.hpp"
#include "../../window/background/custom/include/bg_loader.hpp"
#include "../../../icon/include/icon_manager.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

void create_window(LauncherState *state) {
    state->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(state->window), "KickX Launcher");
    gtk_window_set_default_size(GTK_WINDOW(state->window), 640, 480);
    gtk_window_set_position(GTK_WINDOW(state->window), GTK_WIN_POS_CENTER);

    icon::set_app_icon(state->window);

    g_signal_connect(state->window, "destroy", G_CALLBACK(gtk_main_quit), NULL);

    build_layout(state);

    apply_background(state);
}

}
}
}
}
