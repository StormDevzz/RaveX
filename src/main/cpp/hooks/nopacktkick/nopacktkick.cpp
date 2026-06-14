#include "nopacktkick.h"
#include <chrono>
#include <atomic>
#include <algorithm>

static std::atomic<int> g_packetsPerSecond{100};
static std::atomic<int> g_burstSize{20};
static std::atomic<int> g_tokens{20};
static std::atomic<long long> g_lastRefill{0};

static long long nowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()
    ).count();
}

void initRateLimiter(int packetsPerSecond, int burstSize) {
    g_packetsPerSecond = std::max(1, packetsPerSecond);
    g_burstSize = std::max(1, burstSize);
    g_tokens = g_burstSize.load();
    g_lastRefill = nowMs();
}

bool shouldAllowPacket() {
    long long now = nowMs();
    long long last = g_lastRefill.load();
    long long elapsed = now - last;

    if (elapsed > 0) {
        long long refillTokens = elapsed * g_packetsPerSecond.load() / 1000;
        if (refillTokens > 0) {
            int oldTokens = g_tokens.load();
            int newTokens = std::min(g_burstSize.load(), oldTokens + (int)refillTokens);
            if (newTokens != oldTokens) {
                g_tokens.store(newTokens);
            }
            g_lastRefill.store(now);
        }
    }

    int tokens = g_tokens.load();
    if (tokens > 0) {
        g_tokens.store(tokens - 1);
        return true;
    }
    return false;
}

int getCurrentRate() {
    return g_packetsPerSecond.load();
}

void resetRateLimiter() {
    g_tokens = g_burstSize.load();
    g_lastRefill = nowMs();
}
