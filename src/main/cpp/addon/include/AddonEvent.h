#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonEvent {
private:
    std::string name;
    bool cancelled;
public:
    AddonEvent(const std::string& name);
    std::string getName() const;
    bool isCancelled() const;
    void setCancelled(bool cancel);
};

}
}
