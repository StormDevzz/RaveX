#include "../include/main_window.hpp"
#include <QString>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <algorithm>

QString formatHex(const uint8_t* data, size_t len) {
    QString result;
    char offBuf[16];
    for (size_t off = 0; off < len; off += 16) {
        snprintf(offBuf, sizeof(offBuf), "%04zx  ", off);
        QString line = QString::fromLatin1(offBuf);
        size_t end = std::min(off + 16, len);
        for (size_t j = off; j < end; j++)
            line += QString("%1 ").arg(data[j], 2, 16, QChar('0'));
        for (size_t j = end; j < off + 16; j++)
            line += QStringLiteral("   ");
        line += QStringLiteral("  ");
        for (size_t j = off; j < end; j++)
            line += (data[j] >= 32 && data[j] < 127) ? QChar(data[j]) : QChar('.');
        result += line + QStringLiteral("\n");
    }
    return result;
}
