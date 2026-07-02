#include "portalbuild.hpp"
#define _USE_MATH_DEFINES
#include <cmath>
#include <algorithm>
#include <limits>

namespace ravex {

PortalPos findBestPortalPos(
    double playerX, double playerY, double playerZ,
    double playerYaw,
    double minDistFromPlayer,
    double maxDistFromPlayer,
    double avoidPortalRange,
    const double* existingPortalPositions,
    int portalCount,
    double* groundHeights,
    int groundCount) {

    Vec3 playerPos(playerX, playerY, playerZ);
    PortalPos best;
    best.score = -1.0;

    
    float yawRad = playerYaw * (M_PI / 180.0f);
    Vec3 facingDir(-std::sin(yawRad), 0, std::cos(yawRad));

    
    double searchRadius = maxDistFromPlayer;
    int steps = (int)(searchRadius * 2);

    for (int i = 0; i < steps; i++) {
        for (int j = 0; j < steps; j++) {
            double wx = playerX - searchRadius + (2.0 * searchRadius * i / steps);
            double wz = playerZ - searchRadius + (2.0 * searchRadius * j / steps);

            
            int bx = (int)std::round(wx);
            int bz = (int)std::round(wz);

            Vec3 candidate(bx + 0.5, playerY, bz + 0.5);
            double dist = candidate.distTo(playerPos);

            if (dist < minDistFromPlayer || dist > maxDistFromPlayer) continue;

            
            bool tooCloseToPortal = false;
            for (int p = 0; p < portalCount; p++) {
                Vec3 portalPos(
                    existingPortalPositions[p * 3],
                    existingPortalPositions[p * 3 + 1],
                    existingPortalPositions[p * 3 + 2]
                );
                if (candidate.distTo(portalPos) < avoidPortalRange) {
                    tooCloseToPortal = true;
                    break;
                }
            }
            if (tooCloseToPortal) continue;

            
            Vec3 toCandidate = candidate - playerPos;
            toCandidate.y = 0;
            double toLen = toCandidate.distTo({0,0,0});
            if (toLen < 0.01) continue;
            toCandidate = toCandidate * (1.0 / toLen);

            double dot = facingDir.x * toCandidate.x + facingDir.z * toCandidate.z;
            double score = dot * (1.0 - dist / maxDistFromPlayer);

            if (score > best.score) {
                best = PortalPos(bx, (int)std::round(playerY), bz, score);
            }
        }
    }

    return best;
}

} 
