#include "include/event_types.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace event {

ProgressMsg *alloc_progress_msg(LauncherState *state, const char *text, double fraction) {
    ProgressMsg *msg = new ProgressMsg;
    msg->state = state;
    msg->fraction = fraction;
    strncpy(msg->text, text, sizeof(msg->text) - 1);
    msg->text[sizeof(msg->text) - 1] = '\0';
    return msg;
}

} 
} 
} 
} 
