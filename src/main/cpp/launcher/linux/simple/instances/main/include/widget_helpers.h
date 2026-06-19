#pragma once
#include <gtk/gtk.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

// вспомогательные inline методы для создания стандартных gtk виджетов
inline GtkWidget* create_styled_button(const char* label) {
    return gtk_button_new_with_label(label);
}

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex
