#include "../include/skin_model.hpp"
#include <cstdio>
#include <cstring>

struct Vert { float x, y, z, nx, ny, nz, u, v; };

static void quad(Vert*& dst, float x0, float y0, float z0, float x1, float y1, float z1,
                 float u0, float v0, float u1, float v1,
                 float nx, float ny, float nz)
{

    dst->x = x0; dst->y = y0; dst->z = z1; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u0; dst->v = v1; dst++;
    dst->x = x1; dst->y = y0; dst->z = z1; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u1; dst->v = v1; dst++;
    dst->x = x0; dst->y = y1; dst->z = z0; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u0; dst->v = v0; dst++;

    dst->x = x1; dst->y = y0; dst->z = z1; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u1; dst->v = v1; dst++;
    dst->x = x1; dst->y = y1; dst->z = z0; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u1; dst->v = v0; dst++;
    dst->x = x0; dst->y = y1; dst->z = z0; dst->nx = nx; dst->ny = ny; dst->nz = nz; dst->u = u0; dst->v = v0; dst++;
}


static int buildHead(Vert* buf) {
    Vert* v = buf;

    quad(v, -4,24,-4, -4,32,4,  8.f/64,24.f/64, 16.f/64,32.f/64,  1,0,0);

    quad(v,  4,24,-4,  4,32,4,  0.f/64,24.f/64,  8.f/64,32.f/64, -1,0,0);

    quad(v, -4,32,-4,  4,32,4,  8.f/64, 8.f/64, 16.f/64,16.f/64,  0,1,0);

    quad(v, -4,24,-4,  4,24,4, 16.f/64, 8.f/64, 24.f/64,16.f/64,  0,-1,0);

    quad(v, -4,24, 4,  4,32,4,  8.f/64, 8.f/64, 16.f/64,16.f/64,  0,0,1);

    quad(v, -4,24,-4,  4,32,-4, 24.f/64, 8.f/64, 32.f/64,16.f/64,  0,0,-1);
    return int(v - buf);
}


static int buildBody(Vert* buf) {
    Vert* v = buf;
    quad(v, -4,12,-2, -4,24,2, 16.f/64,28.f/64, 20.f/64,40.f/64, 1,0,0);
    quad(v,  4,12,-2,  4,24,2, 28.f/64,28.f/64, 32.f/64,40.f/64, -1,0,0);
    quad(v, -4,24,-2,  4,24,2, 20.f/64,16.f/64, 28.f/64,20.f/64, 0,1,0);
    quad(v, -4,12,-2,  4,12,2, 24.f/64,20.f/64, 32.f/64,24.f/64, 0,-1,0);
    quad(v, -4,12, 2,  4,24,2, 20.f/64,20.f/64, 28.f/64,32.f/64, 0,0,1);
    quad(v, -4,12,-2,  4,24,-2, 32.f/64,20.f/64, 40.f/64,32.f/64, 0,0,-1);
    return int(v - buf);
}


static int buildRightArm(Vert* buf) {
    Vert* v = buf;
    quad(v, -8,12,-2, -8,24,2, 40.f/64,28.f/64, 44.f/64,40.f/64, 1,0,0);
    quad(v, -4,12,-2, -4,24,2, 48.f/64,28.f/64, 52.f/64,40.f/64, -1,0,0);
    quad(v, -8,24,-2, -4,24,2, 44.f/64,16.f/64, 48.f/64,20.f/64, 0,1,0);
    quad(v, -8,12,-2, -4,12,2, 48.f/64,20.f/64, 52.f/64,24.f/64, 0,-1,0);
    quad(v, -8,12, 2, -4,24,2, 44.f/64,20.f/64, 48.f/64,32.f/64, 0,0,1);
    quad(v, -8,12,-2, -4,24,-2, 52.f/64,20.f/64, 56.f/64,32.f/64, 0,0,-1);
    return int(v - buf);
}


static int buildLeftArm(Vert* buf) {
    Vert* v = buf;
    quad(v, 4,12,-2, 4,24,2, 32.f/64,28.f/64, 36.f/64,40.f/64, -1,0,0);
    quad(v, 8,12,-2, 8,24,2, 40.f/64,28.f/64, 44.f/64,40.f/64, 1,0,0);
    quad(v, 4,24,-2, 8,24,2, 36.f/64,16.f/64, 40.f/64,20.f/64, 0,1,0);
    quad(v, 4,12,-2, 8,12,2, 40.f/64,20.f/64, 44.f/64,24.f/64, 0,-1,0);
    quad(v, 4,12, 2, 8,24,2, 36.f/64,20.f/64, 40.f/64,32.f/64, 0,0,1);
    quad(v, 4,12,-2, 8,24,-2, 44.f/64,20.f/64, 48.f/64,32.f/64, 0,0,-1);
    return int(v - buf);
}


static int buildRightLeg(Vert* buf) {
    Vert* v = buf;
    quad(v, -4,0,-2, -4,12,2, 0.f/64,28.f/64, 4.f/64,40.f/64, 1,0,0);
    quad(v,  0,0,-2,  0,12,2, 8.f/64,28.f/64, 12.f/64,40.f/64, -1,0,0);
    quad(v, -4,12,-2,  0,12,2, 4.f/64,16.f/64, 8.f/64,20.f/64, 0,1,0);
    quad(v, -4,0,-2,  0,0,2, 8.f/64,20.f/64, 12.f/64,24.f/64, 0,-1,0);
    quad(v, -4,0, 2,  0,12,2, 4.f/64,20.f/64, 8.f/64,32.f/64, 0,0,1);
    quad(v, -4,0,-2,  0,12,-2, 12.f/64,20.f/64, 16.f/64,32.f/64, 0,0,-1);
    return int(v - buf);
}


static int buildLeftLeg(Vert* buf) {
    Vert* v = buf;
    quad(v, 0,0,-2, 0,12,2, 16.f/64,28.f/64, 20.f/64,40.f/64, -1,0,0);
    quad(v, 4,0,-2, 4,12,2, 24.f/64,28.f/64, 28.f/64,40.f/64, 1,0,0);
    quad(v, 0,12,-2, 4,12,2, 20.f/64,16.f/64, 24.f/64,20.f/64, 0,1,0);
    quad(v, 0,0,-2, 4,0,2, 24.f/64,20.f/64, 28.f/64,24.f/64, 0,-1,0);
    quad(v, 0,0, 2, 4,12,2, 20.f/64,20.f/64, 24.f/64,32.f/64, 0,0,1);
    quad(v, 0,0,-2, 4,12,-2, 28.f/64,20.f/64, 32.f/64,32.f/64, 0,0,-1);
    return int(v - buf);
}

static void uploadPart(SkinModelPart& part, Vert* verts, int count) {
    glGenVertexArrays(1, &part.vao);
    glGenBuffers(1, &part.vbo);
    glBindVertexArray(part.vao);
    glBindBuffer(GL_ARRAY_BUFFER, part.vbo);
    glBufferData(GL_ARRAY_BUFFER, count * sizeof(Vert), verts, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(Vert), (void*)0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, sizeof(Vert), (void*)offsetof(Vert, nx));
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, sizeof(Vert), (void*)offsetof(Vert, u));
    glEnableVertexAttribArray(2);
    part.count = count;
}

bool SkinModel::build() {
    Vert buf[512];

    uploadPart(head,      buf, buildHead(buf));
    uploadPart(body,      buf, buildBody(buf));
    uploadPart(rightArm,  buf, buildRightArm(buf));
    uploadPart(leftArm,   buf, buildLeftArm(buf));
    uploadPart(rightLeg,  buf, buildRightLeg(buf));
    uploadPart(leftLeg,   buf, buildLeftLeg(buf));

    return true;
}
