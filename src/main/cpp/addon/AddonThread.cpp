#include "include/AddonThread.h"

namespace ravex {
namespace addon {

void AddonThread::lock() { mtx.lock(); }
void AddonThread::unlock() { mtx.unlock(); }

}
}
