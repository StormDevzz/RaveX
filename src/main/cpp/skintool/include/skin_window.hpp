#pragma once
#include <GL/glew.h>
#include <QOpenGLWidget>
#include <QMainWindow>
#include <string>

class QLabel;
class QPushButton;
class QTimer;

struct Camera;
struct SkinModel;
struct SkinTexture;

class SkinRenderer : public QOpenGLWidget {
    Q_OBJECT
public:
    explicit SkinRenderer(const std::string& skinPath, QWidget* parent = nullptr);
    ~SkinRenderer() override;

    void toggleWireframe()  { mWireframe = !mWireframe; update(); }
    void toggleAutoRotate() { mAutoRotate = !mAutoRotate; }
    void resetCamera();
    void loadSkin(const std::string& path);

protected:
    void initializeGL() override;
    void paintGL() override;
    void resizeGL(int w, int h) override;
    void mousePressEvent(QMouseEvent* e) override;
    void mouseMoveEvent(QMouseEvent* e) override;
    void mouseReleaseEvent(QMouseEvent* e) override;
    void wheelEvent(QWheelEvent* e) override;

private:
    void buildProjection(float* m, float aspect) const;

    Camera*       mCam = nullptr;
    SkinModel*    mModel = nullptr;
    SkinTexture*  mTex = nullptr;
    unsigned int  mProg = 0;
    bool          mWireframe = false;
    bool          mAutoRotate = true;
    double        mLastX = 0, mLastY = 0;
    bool          mMouseDown = false;
    std::string   mSkinPath;
    QTimer*       mTimer = nullptr;
};

class SkinWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit SkinWindow(const std::string& skinPath, QWidget* parent = nullptr);

private:
    SkinRenderer* mRenderer;
    QLabel*       mInfoLabel;
    QPushButton*  mWireBtn;
    QPushButton*  mRotBtn;
    QPushButton*  mResetBtn;
    QPushButton*  mLoadBtn;
};
