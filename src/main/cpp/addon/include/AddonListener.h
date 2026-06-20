#pragma once
#include "AddonEvent.h"

namespace ravex {
namespace addon {

class AddonListener {
public:
    virtual ~AddonListener() = default;
    virtual void onEvent(AddonEvent& event) = 0;
};

}
}
