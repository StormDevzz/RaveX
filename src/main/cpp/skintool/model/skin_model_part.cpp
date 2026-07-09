#include "../include/skin_model.hpp"

static void destroyPart(SkinModelPart& part) {
    if (part.vbo) { glDeleteBuffers(1, &part.vbo); part.vbo = 0; }
    if (part.vao) { glDeleteVertexArrays(1, &part.vao); part.vao = 0; }
    part.count = 0;
}

void SkinModel::destroy() {
    destroyPart(head);
    destroyPart(body);
    destroyPart(rightArm);
    destroyPart(leftArm);
    destroyPart(rightLeg);
    destroyPart(leftLeg);
}
