#include "../include/display.hpp"
#include <cstdio>
#include <algorithm>

void printHexDump(const uint8_t* data, size_t len, int bpl) {
    for (size_t i = 0; i < len; i += bpl) {
        printf("  %04zx  ", i);
        size_t end = std::min(i + size_t(bpl), len);

        for (size_t j = i; j < end; j++)
            printf("%02x ", data[j]);
        for (size_t j = end; j < size_t(i + bpl); j++)
            printf("   ");

        printf(" |");
        for (size_t j = i; j < end; j++)
            printf("%c", (data[j] >= 32 && data[j] < 127) ? char(data[j]) : '.');
        printf("|\n");
    }
}
