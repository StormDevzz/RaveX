#include "include/bg_loader.hpp"
#include "../../../file/include/file_manager.hpp"
#include <sys/stat.h>
#include <unistd.h>
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

static GtkCssProvider *bg_provider = nullptr;

void apply_background(LauncherState *state) {
    std::string bg_path = state->kickx_dir + "/background.jpg";
    struct stat buffer;
    bool exists = (stat(bg_path.c_str(), &buffer) == 0);

    if (!exists) return;

    if (bg_provider) {
        gtk_style_context_remove_provider_for_screen(
            gdk_screen_get_default(),
            GTK_STYLE_PROVIDER(bg_provider)
        );
        g_object_unref(bg_provider);
        bg_provider = nullptr;
    }

    bg_provider = gtk_css_provider_new();
    std::string css = "window { background-image: url('" + bg_path + "'); background-size: cover; background-repeat: no-repeat; }";

    gtk_css_provider_load_from_data(bg_provider, css.c_str(), -1, NULL);
    gtk_style_context_add_provider_for_screen(
        gdk_screen_get_default(),
        GTK_STYLE_PROVIDER(bg_provider),
        GTK_STYLE_PROVIDER_PRIORITY_APPLICATION + 1
    );
}

void change_background(LauncherState *state) {
    GtkWidget *dialog = gtk_file_chooser_dialog_new("Choose Background Image",
        GTK_WINDOW(state->window),
        GTK_FILE_CHOOSER_ACTION_OPEN,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Open", GTK_RESPONSE_ACCEPT,
        NULL);

    GtkFileFilter *filter = gtk_file_filter_new();
    gtk_file_filter_set_name(filter, "Images (*.jpg, *.jpeg, *.png)");
    gtk_file_filter_add_mime_type(filter, "image/jpeg");
    gtk_file_filter_add_mime_type(filter, "image/png");
    gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(dialog), filter);

    int res = gtk_dialog_run(GTK_DIALOG(dialog));
    if (res == GTK_RESPONSE_ACCEPT) {
        char *filename = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        if (filename) {
            std::string src(filename);
            std::string dest = state->kickx_dir + "/background.jpg";

            if (file::copy_file(src, dest)) {
                apply_background(state);
            }
            g_free(filename);
        }
    }
    gtk_widget_destroy(dialog);
}

}
}
}
}
