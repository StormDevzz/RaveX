#pragma once
#include <cmath>



struct TntDamageConfig {
    double tntX, tntY, tntZ;
    double targetX, targetY, targetZ;
    double targetHealth;
    int    armorPoints;
    int    armorToughness;
    int    blastProtLevel;
    bool   hasResistance;
    int    resistanceAmplifier;
};

struct TntDamageResult {
    double rawDamage;
    double finalDamage;
    double killProbability;
    bool   lethal;
};


TntDamageResult calculateTntDamage(const TntDamageConfig& config);


double estimateExposure(
    double tntX, double tntY, double tntZ,
    double targetX, double targetY, double targetZ,
    double distance
);
