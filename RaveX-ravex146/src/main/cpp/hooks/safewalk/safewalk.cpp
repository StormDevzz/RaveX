#include "safewalk.hpp"
#include <cmath>

static bool isSolid(int x, int y, int z, int* blocks, int count) {
    for (int i = 0; i + 2 < count; i += 3) {
        if (blocks[i] == x && blocks[i+1] == y && blocks[i+2] == z)
            return true;
    }
    return false;
}

bool isNearEdge(double playerX, double playerY, double playerZ,
                int* solidBlocks, int count,
                double threshold) {
    int bx = (int)std::floor(playerX);
    int by = (int)std::floor(playerY);
    int bz = (int)std::floor(playerZ);

    
    struct Check {
        int x, z;
    } checks[4] = {
        {bx - 1, bz}, {bx + 1, bz},
        {bx, bz - 1}, {bx, bz + 1}
    };

    for (auto& c : checks) {
        
        
        if (isSolid(c.x, by, c.z, solidBlocks, count)) {
            if (!isSolid(c.x, by - 1, c.z, solidBlocks, count)) {
                return true; 
            }
        }
    }

    
    struct Diag {
        int x, z;
    } diags[4] = {
        {bx - 1, bz - 1}, {bx + 1, bz - 1},
        {bx - 1, bz + 1}, {bx + 1, bz + 1}
    };

    for (auto& d : diags) {
        if (isSolid(d.x, by, d.z, solidBlocks, count)) {
            if (!isSolid(d.x, by - 1, d.z, solidBlocks, count)) {
                return true;
            }
        }
    }

    return false;
}
