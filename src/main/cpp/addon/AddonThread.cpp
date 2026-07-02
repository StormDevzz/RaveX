#include "include/AddonThread.hpp"

namespace ravex {
namespace addon {

void AddonThread::lock() { mtx.lock(); }
void AddonThread::unlock() { mtx.unlock(); }

}
}
