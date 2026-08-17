#include "damage.hpp"
#include <cmath>
#include <algorithm>

#ifdef __AVX2__
#include <immintrin.h>
#endif

namespace ravex {

static constexpr double CRYSTAL_EXPLOSION_POWER = 6.0;

#ifdef __AVX2__

static inline bool isBlockedBatch(
    __m256d ox, __m256d oy, __m256d oz,
    __m256d dx, __m256d dy, __m256d dz,
    __m256d len, int step, int maxSteps,
    const BlockSet& blocks
) {
    double stepRatio = (double)step / maxSteps;
    __m256d t = _mm256_mul_pd(_mm256_set1_pd(stepRatio), len);
    __m256d px = _mm256_fmadd_pd(dx, t, ox);
    __m256d py = _mm256_fmadd_pd(dy, t, oy);
    __m256d pz = _mm256_fmadd_pd(dz, t, oz);

    __m256d floorx = _mm256_floor_pd(px);
    __m256d floory = _mm256_floor_pd(py);
    __m256d floorz = _mm256_floor_pd(pz);

    double fx[4], fy[4], fz[4];
    _mm256_storeu_pd(fx, floorx);
    _mm256_storeu_pd(fy, floory);
    _mm256_storeu_pd(fz, floorz);

    for (int i = 0; i < 4; i++) {
        Vec3 lookup(fx[i], fy[i], fz[i]);
        if (blocks.find(lookup) != blocks.end()) return true;
    }
    return false;
}

#endif

double DamageCalc::calcExposure(const Vec3& explosionPos, const Vec3& entityPos, const BlockSet& blocks) {

    const double W = 0.3;
    const double H = 1.8;

    Vec3 samplePoints[9] = {
        {entityPos.x - W, entityPos.y,       entityPos.z - W},
        {entityPos.x + W, entityPos.y,       entityPos.z - W},
        {entityPos.x - W, entityPos.y,       entityPos.z + W},
        {entityPos.x + W, entityPos.y,       entityPos.z + W},
        {entityPos.x - W, entityPos.y + H/2, entityPos.z - W},
        {entityPos.x + W, entityPos.y + H/2, entityPos.z - W},
        {entityPos.x - W, entityPos.y + H/2, entityPos.z + W},
        {entityPos.x + W, entityPos.y + H/2, entityPos.z + W},
        {entityPos.x,     entityPos.y + H/2, entityPos.z    }
    };

    int unblocked = 0;
    const int STEPS = 12;

#ifdef __AVX2__
    __m256d epx = _mm256_set1_pd(explosionPos.x);
    __m256d epy = _mm256_set1_pd(explosionPos.y);
    __m256d epz = _mm256_set1_pd(explosionPos.z);

    int batch = 0;
    bool blocked4[4] = {false, false, false, false};

    for (int i = 0; i < 9; i += 4) {
        int count = (i + 4 <= 9) ? 4 : (9 - i);

        double spx[4], spy[4], spz[4];
        for (int j = 0; j < count; j++) {
            spx[j] = samplePoints[i + j].x;
            spy[j] = samplePoints[i + j].y;
            spz[j] = samplePoints[i + j].z;
        }
        for (int j = count; j < 4; j++) {
            spx[j] = spx[count - 1];
            spy[j] = spy[count - 1];
            spz[j] = spz[count - 1];
        }

        __m256d spxv = _mm256_loadu_pd(spx);
        __m256d spyv = _mm256_loadu_pd(spy);
        __m256d spzv = _mm256_loadu_pd(spz);

        __m256d dx = _mm256_sub_pd(spxv, epx);
        __m256d dy = _mm256_sub_pd(spyv, epy);
        __m256d dz = _mm256_sub_pd(spzv, epz);

        __m256d dx2 = _mm256_mul_pd(dx, dx);
        __m256d dy2 = _mm256_mul_pd(dy, dy);
        __m256d dz2 = _mm256_mul_pd(dz, dz);
        __m256d lenSq = _mm256_add_pd(_mm256_add_pd(dx2, dy2), dz2);
        __m256d len = _mm256_sqrt_pd(lenSq);

        __m256d mask = _mm256_cmp_pd(len, _mm256_set1_pd(0.001), _CMP_GT_OQ);
        int maskI = _mm256_movemask_pd(mask);

        for (int j = 0; j < count; j++) {
            if (!(maskI & (1 << j))) {
                unblocked++;
                blocked4[j] = true;
            } else {
                blocked4[j] = false;
            }
        }

        __m256d invLen = _mm256_div_pd(_mm256_set1_pd(1.0), len);
        __m256d ndx = _mm256_mul_pd(dx, invLen);
        __m256d ndy = _mm256_mul_pd(dy, invLen);
        __m256d ndz = _mm256_mul_pd(dz, invLen);

        for (int step = 1; step <= STEPS; step++) {
            if (_mm256_movemask_pd(_mm256_loadu_pd((double*)blocked4)) == (1 << count) - 1) break;

            double stepRatio = (double)step / STEPS;
            __m256d t = _mm256_mul_pd(_mm256_set1_pd(stepRatio), len);
            __m256d px = _mm256_fmadd_pd(ndx, t, epx);
            __m256d py = _mm256_fmadd_pd(ndy, t, epy);
            __m256d pz = _mm256_fmadd_pd(ndz, t, epz);

            __m256d fx = _mm256_floor_pd(px);
            __m256d fy = _mm256_floor_pd(py);
            __m256d fz = _mm256_floor_pd(pz);

            double fxs[4], fys[4], fzs[4];
            _mm256_storeu_pd(fxs, fx);
            _mm256_storeu_pd(fys, fy);
            _mm256_storeu_pd(fzs, fz);

            for (int j = 0; j < count; j++) {
                if (blocked4[j]) continue;
                Vec3 lookup(fxs[j], fys[j], fzs[j]);
                if (blocks.find(lookup) != blocks.end()) {
                    blocked4[j] = true;
                }
            }
        }

        for (int j = 0; j < count; j++) {
            if (!blocked4[j]) unblocked++;
        }
    }
#else
    for (const Vec3& samplePoint : samplePoints) {
        Vec3 dir = samplePoint - explosionPos;
        double len = dir.length();
        if (len < 0.001) {
            unblocked++;
            continue;
        }

        Vec3 normDir = dir * (1.0 / len);
        bool blocked = false;
        for (int step = 1; step <= STEPS; step++) {
            double t = (double)step / STEPS * len;
            Vec3 p = explosionPos + normDir * t;

            int bx = (int)std::floor(p.x);
            int by = (int)std::floor(p.y);
            int bz = (int)std::floor(p.z);

            Vec3 lookup((double)bx, (double)by, (double)bz);
            if (blocks.find(lookup) != blocks.end()) {
                blocked = true;
                break;
            }
        }
        if (!blocked) unblocked++;
    }
#endif

    return (double)unblocked / 9.0;
}

double DamageCalc::calcExposure(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks) {
    BlockSet blockSet(blocks.begin(), blocks.end());
    return calcExposure(explosionPos, entityPos, blockSet);
}

double DamageCalc::calcRawExplosionDamage(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks) {
    Vec3 center = {entityPos.x, entityPos.y + 0.9, entityPos.z};
    double distance = explosionPos.distanceTo(center);
    double maxDistance = CRYSTAL_EXPLOSION_POWER * 2.0;

    if (distance > maxDistance) return 0.0;

    double exposure = calcExposure(explosionPos, entityPos, blocks);
    double impact = (1.0 - distance / maxDistance) * exposure;

    double baseDamage = (impact * impact + impact) / 2.0 * 7.0 * maxDistance + 1.0;
    return baseDamage;
}

}