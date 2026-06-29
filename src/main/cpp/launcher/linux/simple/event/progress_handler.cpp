#include "include/progress_handler.h"
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

static GtkWidget *progress_window = nullptr;
static GtkWidget *progress_label = nullptr;
static GtkWidget *progress_bar = nullptr;

void show_progress(LauncherState *state, const char *text) {
    if (progress_window) {
        gtk_label_set_text(GTK_LABEL(progress_label), text);
        gtk_window_present(GTK_WINDOW(progress_window));
        return;
    }

    progress_window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(progress_window), "Downloading...");
    gtk_window_set_default_size(GTK_WINDOW(progress_window), 460, 120);
    gtk_window_set_resizable(GTK_WINDOW(progress_window), FALSE);
    gtk_window_set_position(GTK_WINDOW(progress_window), GTK_WIN_POS_CENTER);
    gtk_window_set_transient_for(GTK_WINDOW(progress_window), GTK_WINDOW(state->window));
    gtk_window_set_modal(GTK_WINDOW(progress_window), TRUE);
    gtk_window_set_destroy_with_parent(GTK_WINDOW(progress_window), TRUE);

    
    gtk_style_context_add_class(gtk_widget_get_style_context(progress_window), "progress-window");

    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 20);
    gtk_container_add(GTK_CONTAINER(progress_window), vbox);

    progress_label = gtk_label_new(text);
    gtk_widget_set_halign(progress_label, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), progress_label, FALSE, FALSE, 0);

    progress_bar = gtk_progress_bar_new();
    gtk_progress_bar_set_show_text(GTK_PROGRESS_BAR(progress_bar), TRUE);
    gtk_box_pack_start(GTK_BOX(vbox), progress_bar, FALSE, FALSE, 0);

    g_signal_connect(progress_window, "destroy", G_CALLBACK(gtk_widget_destroyed), &progress_window);

    gtk_widget_show_all(progress_window);
}

void update_progress(LauncherState *state, const char *text, double fraction) {
    if (!progress_window) {
        show_progress(state, text);
        return;
    }
    if (progress_label) {
        gtk_label_set_text(GTK_LABEL(progress_label), text);
    }
    if (progress_bar) {
        gtk_progress_bar_set_fraction(GTK_PROGRESS_BAR(progress_bar), fraction);
    }
}

void hide_progress(LauncherState *) {
    if (progress_window) {
        gtk_widget_destroy(progress_window);
        progress_window = nullptr;
        progress_label = nullptr;
        progress_bar = nullptr;
    }
}

} 
} 
} 
} 
