#include "optimize.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <cmath>
#include <algorithm>

#ifdef _WIN32
#include "optimize_windows.hpp"
#else
#include <unistd.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <sched.h>
#include <malloc.h>
#include <pthread.h>
#endif

namespace ravex {
namespace plugins {
namespace optimize {

void GuiOptimizer::optimizeGuiAndGame() {
#ifdef _WIN32
    StartupBoost::boostAll();
    MemoryBoost::optimizeAll();
    SystemConfig::checkAll();
#else
    
    
    setpriority(PRIO_PROCESS, 0, -20);

    
    pthread_t thread = pthread_self();
    int policy;
    struct sched_param param;
    if (pthread_getschedparam(thread, &policy, &param) == 0) {
        param.sched_priority = sched_get_priority_max(SCHED_RR);
        pthread_setschedparam(thread, SCHED_RR, &param);
    }

    
    long numCPU = sysconf(_SC_NPROCESSORS_ONLN);
    if (numCPU > 0) {
        cpu_set_t cpuset;
        CPU_ZERO(&cpuset);
        int limit = (numCPU > 16) ? 12 : (numCPU > 8 ? 8 : numCPU);
        for (int i = 0; i < limit; i++) {
            CPU_SET(i, &cpuset);
        }
        pthread_setaffinity_np(thread, sizeof(cpu_set_t), &cpuset);
    }

    
    malloc_trim(0);
#endif
}


static void multiplyMatrices(const float* A, const float* B, float* C) {
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            float sum = 0.0f;
            for (int k = 0; k < 4; ++k) {
                sum += A[k * 4 + row] * B[col * 4 + k];
            }
            C[col * 4 + row] = sum;
        }
    }
}


static bool projectPoint(const float* Combined, double dx, double dy, double dz, double& outX, double& outY, double& outZ) {
    float x = static_cast<float>(dx);
    float y = static_cast<float>(dy);
    float z = static_cast<float>(dz);

    float w = Combined[3] * x + Combined[7] * y + Combined[11] * z + Combined[15];
    if (w == 0.0f) return false;

    float rx = Combined[0] * x + Combined[4] * y + Combined[8] * z + Combined[12];
    float ry = Combined[1] * x + Combined[5] * y + Combined[9] * z + Combined[13];
    float rz = Combined[2] * x + Combined[6] * y + Combined[10] * z + Combined[14];

    outX = rx / w;
    outY = ry / w;
    outZ = rz / w;
    return true;
}

int NameTagsOptimizer::optimizeNameTags(
    const double* cameraPos,
    const float* modelView,
    const float* projection,
    const double* playerViewVec,
    const double* positions,
    const double* textWidths,
    const int* booleans,
    const int* armorCounts,
    int count,
    double scaleParam,
    bool distanceScaling,
    double maxDistance,
    int guiWidth,
    int guiHeight,
    double* outLayouts,
    int* outIndices
) {
    
    float Combined[16];
    multiplyMatrices(projection, modelView, Combined);

    int renderedCount = 0;

    double camX = cameraPos[0];
    double camY = cameraPos[1];
    double camZ = cameraPos[2];

    double lookX = playerViewVec[0];
    double lookY = playerViewVec[1];
    double lookZ = playerViewVec[2];

    for (int i = 0; i < count; ++i) {
        
        double bx_w = positions[i * 9 + 0];
        double by_w = positions[i * 9 + 1];
        double bz_w = positions[i * 9 + 2];

        double hx_w = positions[i * 9 + 3];
        double hy_w = positions[i * 9 + 4];
        double hz_w = positions[i * 9 + 5];

        double sx_w = positions[i * 9 + 6];
        double sy_w = positions[i * 9 + 7];
        double sz_w = positions[i * 9 + 8];

        
        double dx = bx_w - camX;
        double dy = by_w - camY;
        double dz = bz_w - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        
        double distance = std::sqrt(distSq);
        if (distance > maxDistance) {
            continue;
        }

        
        if (distance > 0.0) {
            double ndx = dx / distance;
            double ndy = dy / distance;
            double ndz = dz / distance;
            double dot = ndx * lookX + ndy * lookY + ndz * lookZ;
            if (dot <= 0.0) {
                continue;
            }
        }

        
        double projBaseX, projBaseY, projBaseZ;
        double projHeadX, projHeadY, projHeadZ;
        double projSideX, projSideY, projSideZ;

        if (!projectPoint(Combined, dx, dy, dz, projBaseX, projBaseY, projBaseZ) ||
            !projectPoint(Combined, hx_w - camX, hy_w - camY, hz_w - camZ, projHeadX, projHeadY, projHeadZ) ||
            !projectPoint(Combined, sx_w - camX, sy_w - camY, sz_w - camZ, projSideX, projSideY, projSideZ)) {
            continue;
        }

        
        double sx_base = (projBaseX + 1.0) / 2.0 * guiWidth;
        double sy_base = (1.0 - projBaseY) / 2.0 * guiHeight;
        double sx_head = (projHeadX + 1.0) / 2.0 * guiWidth;
        double sy_head = (1.0 - projHeadY) / 2.0 * guiHeight;
        double sx_side = (projSideX + 1.0) / 2.0 * guiWidth;

        
        if ((sx_base < 0 || sx_base > guiWidth || sy_base < 0 || sy_base > guiHeight) &&
            (sx_head < 0 || sx_head > guiWidth || sy_head < 0 || sy_head > guiHeight)) {
            continue;
        }

        
        double tw = textWidths[i * 2 + 0];
        double ow = textWidths[i * 2 + 1];

        bool showArmor   = booleans[i * 5 + 0] != 0;
        bool showHands   = booleans[i * 5 + 1] != 0;
        bool hasOwner    = booleans[i * 5 + 2] != 0;
        bool hasMainHand = booleans[i * 5 + 3] != 0;
        bool hasOffHand  = booleans[i * 5 + 4] != 0;

        int armorCount = armorCounts[i];

        
        double scale = scaleParam;
        if (distanceScaling) {
            scale = scaleParam * (distance * 0.15);
            if (scale < 0.5) scale = 0.5;
            if (scale > 3.0) scale = 3.0;
        }

        
        const double is = 16.0;
        const double gap = 2.0;
        const double padding = 3.0;

        
        double armorRowWidth = 0.0;
        bool hasArmorRow = showArmor && (armorCount > 0);
        if (hasArmorRow) {
            armorRowWidth = armorCount * is + (armorCount - 1) * gap;
        }

        double mainRowWidth = tw;
        if (showHands && hasMainHand) {
            mainRowWidth += is + gap;
        }
        if (showHands && hasOffHand) {
            mainRowWidth += gap + is;
        }

        double ownerRowWidth = hasOwner ? ow : 0.0;
        double totalWidth = std::max(mainRowWidth, std::max(armorRowWidth, ownerRowWidth));

        
        double mainRowHeight = 9.0;
        double textYOffset = 0.0;
        if ((showHands && hasMainHand) || (showHands && hasOffHand)) {
            mainRowHeight = is;
            textYOffset = (is - 9.0) / 2.0;
        }

        
        double totalHeight = 0.0;
        if (hasArmorRow) {
            totalHeight += is + gap;
        }
        bool hasMainRow = (tw > 0);
        if (hasMainRow) {
            totalHeight += mainRowHeight;
        }
        if (hasOwner) {
            if (hasMainRow || hasArmorRow) {
                totalHeight += gap + 9.0;
            } else {
                totalHeight += 9.0;
            }
        }
        totalHeight += 2 * padding;

        
        double bgBottom = -2.0;
        double bgTop = bgBottom - totalHeight;
        double contentTop = bgTop + padding;

        double armorRowY = 0.0;
        if (hasArmorRow) {
            armorRowY = contentTop;
            contentTop += is + gap;
        }

        double mainRowY = 0.0;
        if (hasMainRow) {
            mainRowY = contentTop;
            contentTop += mainRowHeight;
        }

        double ownerRowY = 0.0;
        if (hasOwner) {
            if (hasMainRow || hasArmorRow) {
                ownerRowY = contentTop + gap;
            } else {
                ownerRowY = contentTop;
            }
        }

        
        int idx = renderedCount * 16;
        outLayouts[idx + 0] = scale;
        outLayouts[idx + 1] = totalWidth;
        outLayouts[idx + 2] = totalHeight;
        outLayouts[idx + 3] = armorRowY;
        outLayouts[idx + 4] = mainRowY;
        outLayouts[idx + 5] = ownerRowY;
        outLayouts[idx + 6] = textYOffset;
        outLayouts[idx + 7] = mainRowWidth;
        outLayouts[idx + 8] = armorRowWidth;
        outLayouts[idx + 9] = sx_base;
        outLayouts[idx + 10] = sy_base;
        outLayouts[idx + 11] = sy_head;
        outLayouts[idx + 12] = sx_side;
        outLayouts[idx + 13] = distance;
        outLayouts[idx + 14] = 0.0; 
        outLayouts[idx + 15] = 0.0; 

        
        outIndices[renderedCount] = i;

        renderedCount++;
    }

    return renderedCount;
}

void HudOptimizer::updateAnimations(
    float* displayXs,
    float* displayYs,
    const int* targetXs,
    const int* targetYs,
    unsigned char* animInitializeds,
    int count,
    float speed
) {
    for (int i = 0; i < count; ++i) {
        if (!animInitializeds[i]) {
            displayXs[i] = static_cast<float>(targetXs[i]);
            displayYs[i] = static_cast<float>(targetYs[i]);
            animInitializeds[i] = 1; 
        }
        displayXs[i] += (targetXs[i] - displayXs[i]) * speed;
        displayYs[i] += (targetYs[i] - displayYs[i]) * speed;
        if (std::abs(targetXs[i] - displayXs[i]) < 0.3f) {
            displayXs[i] = static_cast<float>(targetXs[i]);
        }
        if (std::abs(targetYs[i] - displayYs[i]) < 0.3f) {
            displayYs[i] = static_cast<float>(targetYs[i]);
        }
    }
}

void TracersOptimizer::optimizeTracers(
    const double* cameraPos,
    const float* modelView,
    const float* projection,
    const double* positions,
    int count,
    int guiWidth,
    int guiHeight,
    double* outPoints
) {
    float Combined[16];
    multiplyMatrices(projection, modelView, Combined);

    double camX = cameraPos[0];
    double camY = cameraPos[1];
    double camZ = cameraPos[2];

    double cx = guiWidth / 2.0;
    double cy = guiHeight / 2.0;

    for (int i = 0; i < count; ++i) {
        double bx_w = positions[i * 6 + 0];
        double by_w = positions[i * 6 + 1];
        double bz_w = positions[i * 6 + 2];

        double hx_w = positions[i * 6 + 3];
        double hy_w = positions[i * 6 + 4];
        double hz_w = positions[i * 6 + 5];

        double dx_b = bx_w - camX;
        double dy_b = by_w - camY;
        double dz_b = bz_w - camZ;

        double dx_h = hx_w - camX;
        double dy_h = hy_w - camY;
        double dz_h = hz_w - camZ;

        double projBaseX, projBaseY, projBaseZ;
        double projHeadX, projHeadY, projHeadZ;

        if (!projectPoint(Combined, dx_b, dy_b, dz_b, projBaseX, projBaseY, projBaseZ) ||
            !projectPoint(Combined, dx_h, dy_h, dz_h, projHeadX, projHeadY, projHeadZ)) {
            outPoints[i * 3 + 2] = 0.0; 
            continue;
        }

        double ex = (projBaseX + 1.0) / 2.0 * guiWidth;
        double ey_base = (1.0 - projBaseY) / 2.0 * guiHeight;
        double ey_head = (1.0 - projHeadY) / 2.0 * guiHeight;
        double ey = (ey_base + ey_head) / 2.0;

        bool isBehind = projBaseZ < 0;
        bool isOffscreen = isBehind || ex < 0 || ex > guiWidth || ey < 0 || ey > guiHeight;

        if (isOffscreen) {
            double dx = ex - cx;
            double dy = ey - cy;
            double tX = 1e30;
            double tY = 1e30;
            double borderPadding = 2.0;

            if (dx > 0) {
                tX = (guiWidth - borderPadding - cx) / dx;
            } else if (dx < 0) {
                tX = (borderPadding - cx) / dx;
            }

            if (dy > 0) {
                tY = (guiHeight - borderPadding - cy) / dy;
            } else if (dy < 0) {
                tY = (borderPadding - cy) / dy;
            }

            double t = std::min(tX, tY);
            if (t > 0.0 && t < 1.0) {
                ex = cx + t * dx;
                ey = cy + t * dy;
            } else {
                double len = std::sqrt(dx * dx + dy * dy);
                if (len > 0.0) {
                    ex = cx + (dx / len) * (cx - borderPadding);
                    ey = cy + (dy / len) * (cy - borderPadding);
                }
            }
        }

        outPoints[i * 3 + 0] = ex;
        outPoints[i * 3 + 1] = ey;
        outPoints[i * 3 + 2] = 1.0; 
    }
}

} 
} 
} 
