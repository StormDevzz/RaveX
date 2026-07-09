#pragma once
#include <QMainWindow>
#include <QTreeView>
#include <QTextEdit>
#include <QLabel>
#include <QStandardItemModel>
#include <QString>
#include "pcap_reader.hpp"
#include "packet_analysis.hpp"

struct PktEntry {
    PacketRecord rec;
    PacketInfo info;
    bool valid = false;
};

class MainWindow : public QMainWindow {
    Q_OBJECT
public:
    MainWindow(const QString& pcapPath);

private:
    void loadPcap(const QString& path);
    void onPacketClicked(const QModelIndex& idx);

    QTreeView* m_tree;
    QTextEdit* m_hexView;
    QLabel* m_statusLabel;
    QStandardItemModel* m_model;

    std::vector<PktEntry> m_packets;
    int m_totalScanned = 0;
    int m_serverPkts = 0, m_clientPkts = 0;
};

QString formatHex(const uint8_t* data, size_t len);
