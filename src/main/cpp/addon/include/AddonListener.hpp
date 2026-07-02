#pragma once
#include "AddonEvent.hpp"

namespace ravex {
namespace addon {

class AddonListener {
public:
    virtual ~AddonListener() = default;
    virtual void onEvent(AddonEvent& event) = 0;
};

}
}
