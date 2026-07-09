#include "../include/main_window.hpp"
#include <QTextEdit>
#include <ctime>

void MainWindow::onPacketClicked(const QModelIndex& idx) {
    int row = idx.row();
    if (row < 0 || row >= int(m_packets.size())) return;

    auto& p = m_packets[row];
    QString info;

    if (p.valid) {
        info += QString("ID: 0x%1 (%2)\n")
                    .arg(p.info.id, 2, 16, QChar('0'))
                    .arg(p.info.id);
        info += QString("Name: %1\n").arg(QString::fromStdString(p.info.name));
        info += QString("State: %1\n").arg(QString::fromStdString(p.info.state));
    } else {
        info += "Non-Minecraft packet\n";
    }

    info += QString("Direction: %1\n")
                .arg(p.rec.fromServer ? "Server -> Client" : "Client -> Server");
    info += QString("Length: %1 bytes\n").arg(p.rec.data.size());

    double ts = p.rec.timestamp;
    time_t sec = time_t(ts);
    struct tm* tm = localtime(&sec);
    char tbuf[32];
    strftime(tbuf, sizeof(tbuf), "%Y-%m-%d %H:%M:%S", tm);
    info += QString("Time: %1.%2\n")
                .arg(tbuf)
                .arg(int((ts - double(sec)) * 1000));

    info += "Ports: ";
    info += p.rec.fromServer ? "server" : "client";
    info += QString(":%1 -> ").arg(p.rec.srcPort);
    info += p.rec.fromServer ? "client" : "server";
    info += QString(":%1\n\n").arg(p.rec.dstPort);
    info += "--- Hex Dump ---\n";
    info += formatHex(p.rec.data.data(), p.rec.data.size());

    m_hexView->setText(info);
}
