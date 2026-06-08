#pragma once

#include <cmath>
#include <vector>
#include <array>
#include <string>
#include <algorithm>

namespace ravex {

// ── Вектор 3D ────────────────────────────────────────────────────────────────
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

// ── Характеристики сущности (эффекты, броня, прочность, движение и тотемы) ───
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
    double totemCount; // Добавлено: количество тотемов у сущности
};

// ── Описание позиции для установки кристалла ─────────────────────────────────
struct CrystalPlacement {
    Vec3    blockPos;       // Позиция блока куда ставим кристалл
    Vec3    crystalPos;     // Итоговая позиция кристалла
    double  targetDamage;   // Урон цели
    double  selfDamage;     // Урон себе
    double  score;          // Итоговая оценка
    bool    valid;          // Прошла ли позиция все проверки
};

// ── Параметры расчётов ───────────────────────────────────────────────────────
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
    
    // Новые настраиваемые параметры
    bool   armorBreaker     = true;
    double armorPercent     = 15.0;
    double predictTicks     = 1.0;
    bool   totemDetection   = true;
    bool   totemCheckTarget = true;
    bool   placeAirPlace    = false;
    bool   placeMultiPlace  = false;
};

// ── Состояние кристалла на поле ──────────────────────────────────────────────
struct CrystalEntity {
    int    entityId;
    Vec3   pos;
    bool   shouldBreak;
};

// ── Результат AutoCrystal-тика ────────────────────────────────────────────────
struct AutoCrystalResult {
    bool             shouldPlace;    // Нужно ли ставить кристалл
    CrystalPlacement bestPlacement;  // Лучшая позиция для установки
    bool             shouldPlace2;   // Нужно ли ставить второй кристалл
    CrystalPlacement secondPlacement;// Вторая лучшая позиция
    bool             shouldBreak;    // Нужно ли подрывать кристалл
    int              breakEntityId;  // ID кристалла для подрыва
    Vec3             breakPos;       // Позиция кристалла для подрыва
    double           breakDamage;    // Ожидаемый урон от подрыва
    std::string      debugInfo;      // Отладочная информация
};

// ── Основной класс расчётов ───────────────────────────────────────────────────
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

} // namespace ravex
