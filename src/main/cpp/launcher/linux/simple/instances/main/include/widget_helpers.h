#pragma once
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {


inline GtkWidget* create_styled_button(const char* label) {
    return gtk_button_new_with_label(label);
}

} 
} 
} 
} 
