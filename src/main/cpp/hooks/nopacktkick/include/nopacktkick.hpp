#pragma once

void initRateLimiter(int packetsPerSecond, int burstSize);
bool shouldAllowPacket();
int getCurrentRate();
void resetRateLimiter();
