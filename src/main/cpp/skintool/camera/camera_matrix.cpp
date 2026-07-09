#include "../include/camera.hpp"
#include <cmath>

void Camera::viewMatrix(float* m) const {
    float cyaw = cosf(yaw * 0.017453f);
    float syaw = sinf(yaw * 0.017453f);
    float cpitch = cosf(pitch * 0.017453f);
    float spitch = sinf(pitch * 0.017453f);

    float ex = cx + distance * (cyaw * cpitch);
    float ey = cy + distance * spitch;
    float ez = cz + distance * (syaw * cpitch);

    float fx = cx - ex, fy = cy - ey, fz = cz - ez;
    float fl = sqrtf(fx*fx + fy*fy + fz*fz);
    fx /= fl; fy /= fl; fz /= fl;

    float ux = 0.f, uy = 1.f, uz = 0.f;
    float rx = uy*fz - uz*fy;
    float ry = uz*fx - ux*fz;
    float rz = ux*fy - uy*fx;
    float rl = sqrtf(rx*rx + ry*ry + rz*rz);
    rx /= rl; ry /= rl; rz /= rl;

    ux = ry*fz - rz*fy;
    uy = rz*fx - rx*fz;
    uz = rx*fy - ry*fx;

    m[0]=rx; m[1]=ux; m[2]=-fx; m[3]=0;
    m[4]=ry; m[5]=uy; m[6]=-fy; m[7]=0;
    m[8]=rz; m[9]=uz; m[10]=-fz; m[11]=0;
    m[12]=-(rx*ex+ry*ey+rz*ez);
    m[13]=-(ux*ex+uy*ey+uz*ez);
    m[14]= fx*ex+fy*ey+fz*ez;
    m[15]=1;
}
