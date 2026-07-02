#pragma once

#include <cmath>
#include <vector>
#include <array>
#include <string>
#include <algorithm>

namespace ravex {


struct Vec3 {
    double x, y, z;

    Vec3() : x(0), y(0), z(0) {}
    Vec3(double x, double y, double z) : x(x), y(y), z(z) {}

    Vec3 operator+(const Vec3& o) const { return {x+o.x, y+o.y, z+o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x-o.x, y-o.y, z-o.z}; }
    Vec3 operator*(double s) const { return {x*s, y*s, z*s}; }

    double lengthSq() const { return x*x + y*y + z*z; }
    double length()   const { return std::sqrt(lengthSq()); }

    double distanceTo(const Vec3& o) const { return (*this - o).length(); }
    double distanceToSq(const Vec3& o) const { return (*this - o).lengthSq(); }
};


struct EntityStats {
    double armorValue;
    double toughness;
    double blastProtectionEpf;
    double protectionEpf;
    double resistanceLevel;
    double weaknessLevel;
    double strengthLevel;
    double helmetDurability;
    double chestplateDurability;
    double leggingsDurability;
    double bootsDurability;
    double motionX;
    double motionY;
    double motionZ;
    double totemCount; 
};


struct CrystalPlacement {
    Vec3    blockPos;       
    Vec3    crystalPos;     
    double  targetDamage;   
    double  selfDamage;     
    double  score;          
    bool    valid;          
};


struct AutoCrystalConfig {
    double placeRange       = 4.5;
    double placeWallRange   = 3.5;
    double breakRange       = 4.5;
    double breakWallRange   = 3.5;
    double minTargetDamage  = 4.0;
    double maxSelfDamage    = 8.0;
    double selfDamageWeight = 1.2;
    double placeDelay       = 100.0;
    double breakDelay       = 50.0;
    bool   antiSuicide      = true;
    bool   antiSuicideCheckBreaking = true;
    bool   antiSuicideIgnoreWithTotem = false;
    bool   requireExposed   = false;
    double targetHealthBonus= 2.0;
    
    
    bool   armorBreaker     = true;
    double armorPercent     = 15.0;
    double predictTicks     = 1.0;
    bool   totemDetection   = true;
    bool   totemCheckTarget = true;
    bool   placeAirPlace    = false;
    bool   placeMultiPlace  = false;
    bool   suicide          = false;
    bool   grimAC           = false;
    bool   ncpBypass        = false;
    bool   bgBlockScanner   = true;
    bool   kbPrediction     = true;
    bool   collateralPop    = true;
};


struct CrystalEntity {
    int    entityId;
    Vec3   pos;
    bool   shouldBreak;
};


struct AutoCrystalResult {
    bool             shouldPlace;    
    CrystalPlacement bestPlacement;  
    bool             shouldPlace2;   
    CrystalPlacement secondPlacement;
    bool             shouldBreak;    
    int              breakEntityId;  
    Vec3             breakPos;       
    double           breakDamage;    
    std::string      debugInfo;      
};


class AutoCrystalMath {
public:
    static AutoCrystalResult tick(
        const Vec3&                   playerPos,
        double                        playerHealth,
        double                        playerAbsorption,
        const EntityStats&            playerStats,
        const Vec3&                   targetPos,
        double                        targetHealth,
        double                        targetAbsorption,
        const EntityStats&            targetStats,
        const std::vector<Vec3>&      obby,
        const std::vector<CrystalEntity>& activeCrystals,
        const AutoCrystalConfig&      config
    );

    static double calcExplosionDamage(
        const Vec3& explosionPos,
        const Vec3& entityPos,
        double      entityHealth,
        double      entityAbsorption,
        const EntityStats& stats,
        const std::vector<Vec3>& blocks
    );

    static CrystalPlacement findBestPlacement(
        const Vec3&               playerPos,
        double                    playerHealth,
        double                    playerAbsorption,
        const EntityStats&        playerStats,
        const Vec3&               targetPos,
        double                    targetHealth,
        double                    targetAbsorption,
        const EntityStats&        targetStats,
        const std::vector<Vec3>&  blocks,
        const AutoCrystalConfig&  config,
        bool                      excludePos = false,
        const Vec3&               posToExclude = {}
    );

    static bool findBestBreak(
        const std::vector<CrystalEntity>& crystals,
        const Vec3&  playerPos,
        double       playerHealth,
        double       playerAbsorption,
        const EntityStats& playerStats,
        const Vec3&  targetPos,
        double       targetHealth,
        double       targetAbsorption,
        const EntityStats& targetStats,
        const std::vector<Vec3>& blocks,
        const AutoCrystalConfig& config,
        int&         outEntityId,
        Vec3&        outPos,
        double&      outDamage
    );

    static bool isValidBase(const Vec3& pos, const std::vector<Vec3>& blocks);
    static bool isOccupied(const Vec3& crystalPos, const std::vector<CrystalEntity>& crystals);
    static double distanceToCenter(const Vec3& playerPos, const Vec3& blockPos);
};

} 
