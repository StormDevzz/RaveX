#pragma once
#include <cmath>

// ─── TNT Damage Calculation ──────────────────────────────────────────────────

struct TntDamageConfig {
    double tntX, tntY, tntZ;       // TNT position (center of block)
    double targetX, targetY, targetZ;
    double targetHealth;
    int    armorPoints;             // total armor defense points
    int    armorToughness;          // total armor toughness
    int    blastProtLevel;          // total blast protection level (sum)
    bool   hasResistance;           // resistance potion effect
    int    resistanceAmplifier;     // amplifier (0 = Resistance I)
};

struct TntDamageResult {
    double rawDamage;               // damage before armor/enchant reduction
    double finalDamage;             // damage after all reductions
    double killProbability;         // estimated kill probability (0.0-1.0)
    bool   lethal;                  // finalDamage >= targetHealth
};

/**
 * Calculate expected TNT explosion damage on a target.
 * Uses vanilla explosion damage formula:
 *   impact = (1 - distance/radius) * exposure
 *   rawDamage = floor((impact^2 + impact) * 7 * radius + 1)
 * Then applies armor, enchantment, and potion reductions.
 */
TntDamageResult calculateTntDamage(const TntDamageConfig& config);

/**
 * Estimate exposure: the fraction of the target's bounding box
 * that is not occluded by solid blocks from the TNT center.
 * Simplified version: 1.0 if no blocks between, scaled down otherwise.
 */
double estimateExposure(
    double tntX, double tntY, double tntZ,
    double targetX, double targetY, double targetZ,
    double distance
);
