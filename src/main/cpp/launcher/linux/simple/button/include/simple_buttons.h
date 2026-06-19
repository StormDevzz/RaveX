#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace button {

// обработчик нажатия на кнопку проверки обновлений
void on_check_clicked(GtkWidget *widget, gpointer user_data);

// обработчик нажатия на кнопку запуска/убийства игры
void on_launch_clicked(GtkWidget *widget, gpointer user_data);

} // namespace button
} // namespace simple
} // namespace launcher
} // namespace ravex
