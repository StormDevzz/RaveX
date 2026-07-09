#include "../include/skin_window.hpp"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QPushButton>
#include <QLabel>
#include <QFileDialog>
#include <QMessageBox>

SkinWindow::SkinWindow(const std::string& skinPath, QWidget* parent)
    : QMainWindow(parent)
{
    setWindowTitle("Skintool — Minecraft Skin Viewer");
    resize(1000, 750);

    auto* central = new QWidget(this);
    auto* layout = new QVBoxLayout(central);

    mRenderer = new SkinRenderer(skinPath, this);
    layout->addWidget(mRenderer, 1);

    auto* ctrl = new QHBoxLayout;
    mInfoLabel = new QLabel(this);
    ctrl->addWidget(mInfoLabel, 1);

    mWireBtn = new QPushButton("Wireframe (W)", this);
    mRotBtn = new QPushButton("Auto-Rotate (R)", this);
    mResetBtn = new QPushButton("Reset Camera (Space)", this);
    mLoadBtn = new QPushButton("Load Skin (O)", this);

    mRotBtn->setCheckable(true);
    mRotBtn->setChecked(true);

    ctrl->addWidget(mWireBtn);
    ctrl->addWidget(mRotBtn);
    ctrl->addWidget(mResetBtn);
    ctrl->addWidget(mLoadBtn);
    layout->addLayout(ctrl);

    setCentralWidget(central);

    connect(mWireBtn, &QPushButton::clicked, this, [this]{
        mRenderer->toggleWireframe();
        mWireBtn->setText(mWireBtn->text().startsWith("Wire") ? "Wireframe ON" : "Wireframe (W)");
    });
    connect(mRotBtn, &QPushButton::clicked, this, [this]{
        mRenderer->toggleAutoRotate();
        mRotBtn->setText(mRotBtn->isChecked() ? "Auto-Rotate (R)" : "Auto-Rotate OFF");
    });
    connect(mResetBtn, &QPushButton::clicked, this, [this]{ mRenderer->resetCamera(); });
    connect(mLoadBtn, &QPushButton::clicked, this, [this]{
        QString path = QFileDialog::getOpenFileName(this, "Open Skin PNG", "", "PNG (*.png)");
        if (!path.isEmpty()) {
            mRenderer->loadSkin(path.toStdString());
            mInfoLabel->setText("Loaded: " + path);
        }
    });

    mInfoLabel->setText("Skin: " + QString::fromStdString(skinPath));
}
