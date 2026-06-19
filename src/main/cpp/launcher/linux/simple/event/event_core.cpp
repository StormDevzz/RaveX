#include "include/event_types.h"
#include "include/progress_handler.h"
#include "include/event_queue.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

void init_events() {
}

void shutdown_events() {
    hide_progress(nullptr);
}

} // namespace event
} // namespace simple
} // namespace launcher
} // namespace ravex
