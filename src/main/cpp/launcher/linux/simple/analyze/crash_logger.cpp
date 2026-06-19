#include "include/crash_logger.h"
#include "include/log_manager.h"
#include <signal.h>
#include <cstring>
#include <execinfo.h>
#include <iostream>

namespace ravex {
namespace launcher {
namespace simple {
namespace analyze {

static bool logging_enabled = true;

static void crash_signal_handler(int sig, siginfo_t *info, void *ctx) {
    (void)ctx;
    write_log("CRASH", "=== CRASH DETECTED ===");
    write_log("CRASH", "signal: " + std::to_string(sig) + " (" + strsignal(sig) + ")");

    void *bt[64];
    int n = backtrace(bt, 64);
    char **symbols = backtrace_symbols(bt, n);
    for (int i = 0; i < n; i++) {
        write_log("CRASH", "  [" + std::to_string(i) + "] " + symbols[i]);
    }
    free(symbols);

    write_log("CRASH", "=== END ===");
    _exit(128 + sig);
}

void setup_crash_handler() {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = crash_signal_handler;
    sa.sa_flags = SA_SIGINFO;

    sigaction(SIGSEGV, &sa, nullptr);
    sigaction(SIGABRT, &sa, nullptr);
    sigaction(SIGBUS, &sa, nullptr);
    sigaction(SIGFPE, &sa, nullptr);
    sigaction(SIGILL, &sa, nullptr);
}

void log_crash(const std::string &message) {
    write_log("CRASH", message);
}

void set_log_enabled(bool enabled) {
    logging_enabled = enabled;
}

bool is_log_enabled() {
    return logging_enabled;
}

} // namespace analyze
} // namespace simple
} // namespace launcher
} // namespace ravex
