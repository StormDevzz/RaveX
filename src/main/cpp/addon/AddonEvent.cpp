#include "include/AddonEvent.h"

namespace ravex {
namespace addon {

AddonEvent::AddonEvent(const std::string& name) : name(name), cancelled(false) {}
std::string AddonEvent::getName() const { return name; }
bool AddonEvent::isCancelled() const { return cancelled; }
void AddonEvent::setCancelled(bool cancel) { cancelled = cancel; }

}
}
