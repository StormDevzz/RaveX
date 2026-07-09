#include "../include/main_window.hpp"
#include <QSplitter>
#include <QHeaderView>
#include <QFont>
#include <QStatusBar>

MainWindow::MainWindow(const QString& pcapPath) {
    setWindowTitle("mcprotvis - Minecraft Protocol Visualizer");
    resize(1200, 700);

    auto* splitter = new QSplitter(Qt::Horizontal, this);
    setCentralWidget(splitter);

    m_model = new QStandardItemModel(0, 4, this);
    m_model->setHorizontalHeaderLabels({"#", "Time", "Dir", "Packet"});

    m_tree = new QTreeView;
    m_tree->setModel(m_model);
    m_tree->setRootIsDecorated(false);
    m_tree->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_tree->setSelectionMode(QAbstractItemView::SingleSelection);
    m_tree->header()->setStretchLastSection(true);
    m_tree->header()->setSectionResizeMode(0, QHeaderView::ResizeToContents);
    m_tree->header()->setSectionResizeMode(1, QHeaderView::ResizeToContents);
    m_tree->header()->setSectionResizeMode(2, QHeaderView::ResizeToContents);
    connect(m_tree, &QTreeView::clicked, this, &MainWindow::onPacketClicked);

    m_hexView = new QTextEdit;
    m_hexView->setReadOnly(true);
    m_hexView->setFont(QFont("Monospace", 9));
    m_hexView->setLineWrapMode(QTextEdit::NoWrap);

    splitter->addWidget(m_tree);
    splitter->addWidget(m_hexView);
    splitter->setStretchFactor(0, 3);
    splitter->setStretchFactor(1, 2);

    m_statusLabel = new QLabel;
    statusBar()->addWidget(m_statusLabel);

    loadPcap(pcapPath);
}
