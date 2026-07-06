#pragma once
#include <mutex>

namespace ravex {
namespace addon {

class AddonThread {
private:
    std::mutex mtx;
public:
    void lock();
    void unlock();
};

}
}
