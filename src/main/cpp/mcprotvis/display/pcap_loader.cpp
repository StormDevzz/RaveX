#include "../include/main_window.hpp"
#include "../include/pcap_reader.hpp"
#include "../include/packet_analysis.hpp"
#include <QStandardItem>
#include <QStatusBar>
#include <QDateTime>
#include <ctime>
#include <cmath>

void MainWindow::loadPcap(const QString& path) {
    PcapReader reader;
    if (!reader.open(path.toStdString())) {
        statusBar()->showMessage("Failed to open pcap file");
        return;
    }

    PacketRecord rec;
    while (reader.nextPacket(rec)) {
        m_totalScanned++;
        if (!filterMcPacket(rec, reader.linkType)) continue;

        PktEntry e;
        e.rec = std::move(rec);
        size_t pos = 0;
        e.valid = parsePacket(e.rec.data.data(), e.rec.data.size(), pos, e.info);
        if (!e.valid) continue;

        if (e.rec.fromServer) m_serverPkts++;
        else m_clientPkts++;

        int row = m_packets.size();
        m_packets.push_back(std::move(e));
        auto& p = m_packets.back();

        auto* idxItem = new QStandardItem(QString::number(row));

        double ts = p.rec.timestamp;
        time_t sec = time_t(ts);
        int ms = int((ts - double(sec)) * 1000);
        struct tm* tm = localtime(&sec);
        char tbuf[32];
        strftime(tbuf, sizeof(tbuf), "%H:%M:%S", tm);
        auto* timeItem = new QStandardItem(
            QString("%1.%2").arg(tbuf).arg(ms, 3, 10, QChar('0')));

        auto* dirItem = new QStandardItem(
            p.rec.fromServer ? "\xe2\x97\x80" : "\xe2\x96\xb6");

        auto* nameItem = new QStandardItem(
            QString("%1 (0x%2)")
                .arg(QString::fromStdString(p.info.name))
                .arg(p.info.id, 2, 16, QChar('0')));

        m_model->appendRow({idxItem, timeItem, dirItem, nameItem});
    }
    reader.close();

    m_statusLabel->setText(
        QString("Packets: %1 | S->C: %2 | C->S: %3 | Scanned: %4")
            .arg(m_packets.size()).arg(m_serverPkts).arg(m_clientPkts).arg(m_totalScanned));
}
