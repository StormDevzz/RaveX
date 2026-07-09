#include "../include/skin_model.hpp"

static void drawPart(const SkinModelPart& part) {
    if (!part.vao) return;
    glBindVertexArray(part.vao);
    glDrawArrays(GL_TRIANGLES, 0, part.count);
}

void SkinModel::draw(GLuint prog, bool wireframe) const {
    (void)prog;
    glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);
    drawPart(head);
    drawPart(body);
    drawPart(rightArm);
    drawPart(leftArm);
    drawPart(rightLeg);
    drawPart(leftLeg);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
}
