#include "icon_manager.h"
#include "icon_data.h"
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace icon {

bool set_app_icon(void* gtk_window) {
    GdkPixbufLoader* loader = gdk_pixbuf_loader_new();
    if (!loader) return false;

    if (!gdk_pixbuf_loader_write(loader,
            assets_launcher_icon_png,
            assets_launcher_icon_png_len, nullptr)) {
        g_object_unref(loader);
        return false;
    }
    gdk_pixbuf_loader_close(loader, nullptr);

    GdkPixbuf* pixbuf = gdk_pixbuf_loader_get_pixbuf(loader);
    if (!pixbuf) {
        g_object_unref(loader);
        return false;
    }

    gtk_window_set_icon(GTK_WINDOW(gtk_window), pixbuf);
    g_object_unref(loader);
    return true;
}

} // namespace icon
} // namespace launcher
} // namespace ravex
