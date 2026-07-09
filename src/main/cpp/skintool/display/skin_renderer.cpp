#include "../include/skin_window.hpp"
#include "../include/shader.hpp"
#include "../include/camera.hpp"
#include "../include/skin_model.hpp"
#include "../include/skin_texture.hpp"
#include <QMouseEvent>
#include <QWheelEvent>
#include <QTimer>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <GL/glew.h>

SkinRenderer::SkinRenderer(const std::string& skinPath, QWidget* parent)
    : QOpenGLWidget(parent), mSkinPath(skinPath)
{
    setFocusPolicy(Qt::StrongFocus);
    mTimer = new QTimer(this);
    connect(mTimer, &QTimer::timeout, this, [this]{ update(); });
    mTimer->start(16);
}

SkinRenderer::~SkinRenderer() {
    makeCurrent();
    if (mModel)  { mModel->destroy(); delete mModel; }
    if (mTex)    { mTex->destroy(); delete mTex; }
    if (mCam)    { delete mCam; }
    if (mProg)   { glDeleteProgram(mProg); }
    doneCurrent();
}

void SkinRenderer::resetCamera() {
    if (mCam) mCam->reset();
    update();
}

void SkinRenderer::loadSkin(const std::string& path) {
    makeCurrent();
    SkinTexture* tmp = new SkinTexture;
    if (tmp->load(path)) {
        if (mTex) { mTex->destroy(); delete mTex; }
        mTex = tmp;
    } else {
        delete tmp;
    }
    doneCurrent();
    update();
}

void SkinRenderer::initializeGL() {
    glewExperimental = GL_TRUE;
    if (glewInit() != GLEW_OK) {
        fprintf(stderr, "GLEW init failed\n");
        return;
    }

    mCam = new Camera;
    mProg = makeProgram();
    if (!mProg) return;

    mModel = new SkinModel;
    if (!mModel->build()) return;

    mTex = new SkinTexture;
    mTex->load(mSkinPath);

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glClearColor(0.15f, 0.15f, 0.2f, 1.f);
}

void SkinRenderer::paintGL() {
    if (!mProg) return;

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    if (mAutoRotate) {
        mCam->yaw += 0.3f;
        if (mCam->yaw > 3600.f) mCam->yaw = fmodf(mCam->yaw, 360.f);
    }

    glUseProgram(mProg);

    float proj[16];
    buildProjection(proj, float(width()) / float(height()));
    setProj(mProg, proj);

    float view[16];
    mCam->viewMatrix(view);
    setView(mProg, view);

    float model[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    setModel(mProg, model);

    setLightDir(mProg, 0.5f, 0.8f, 0.6f);
    setWireframe(mProg, mWireframe);
    setTextureUnit(mProg, 0);

    if (mTex && mTex->id) mTex->bind(0);
    mModel->draw(mProg, mWireframe);
}

void SkinRenderer::resizeGL(int w, int h) {
    glViewport(0, 0, w, h);
}

void SkinRenderer::buildProjection(float* m, float aspect) const {
    memset(m, 0, sizeof(float) * 16);
    float fov = 45.f, fnear = 0.1f, ffar = 100.f;
    float ftan = tanf(fov * 0.0087266f);
    m[0] = 1.f / (aspect * ftan);
    m[5] = 1.f / ftan;
    m[10] = -(ffar + fnear) / (ffar - fnear);
    m[11] = -1.f;
    m[14] = -2.f * fnear * ffar / (ffar - fnear);
}

void SkinRenderer::mousePressEvent(QMouseEvent* e) {
    if (e->button() == Qt::LeftButton) {
        mMouseDown = true;
        mLastX = e->position().x();
        mLastY = e->position().y();
        setCursor(Qt::ClosedHandCursor);
    }
}

void SkinRenderer::mouseMoveEvent(QMouseEvent* e) {
    if (mMouseDown && mCam) {
        double x = e->position().x();
        double y = e->position().y();
        float dx = float(x - mLastX);
        float dy = float(y - mLastY);
        mCam->orbit(dx * 0.3f, dy * 0.3f);
        mLastX = x;
        mLastY = y;
    }
}

void SkinRenderer::mouseReleaseEvent(QMouseEvent* e) {
    if (e->button() == Qt::LeftButton) {
        mMouseDown = false;
        setCursor(Qt::ArrowCursor);
    }
}

void SkinRenderer::wheelEvent(QWheelEvent* e) {
    if (mCam) {
        mCam->zoom(float(-e->angleDelta().y()) * 0.005f);
    }
}
