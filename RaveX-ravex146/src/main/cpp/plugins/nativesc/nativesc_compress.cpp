#include "include/nativesc.hpp"
#include <vector>
#include <cstring>
#include <cstdlib>

namespace ravex {
namespace nativesc {

static void deflateRaw(const unsigned char* input, size_t inputSize,
                       std::vector<unsigned char>& output) {
    output.clear();
    output.reserve(inputSize + inputSize / 10 + 12);
    output.push_back(0x78);
    output.push_back(0x9C);

    unsigned int s1 = 1, s2 = 0;
    for (size_t i = 0; i < inputSize; i++) {
        unsigned char byte = input[i];
        if (byte == 0 && inputSize - i > 3 &&
            input[i+1] == 0 && input[i+2] == 0) {
            int run = 1;
            while (i + run < inputSize && input[i + run] == 0 && run < 258) run++;
            if (run >= 3) {
                output.push_back(byte);
                s1 = (s1 + byte) % 65521;
                s2 = (s2 + s1) % 65521;
                i++;
                output.push_back(input[i]);
                s1 = (s1 + input[i]) % 65521;
                s2 = (s2 + s1) % 65521;
                i++;
                output.push_back(input[i]);
                s1 = (s1 + input[i]) % 65521;
                s2 = (s2 + s1) % 65521;
                i++;
                continue;
            }
        }
        output.push_back(byte);
        s1 = (s1 + byte) % 65521;
        s2 = (s2 + s1) % 65521;
    }

    unsigned int adler = (s2 << 16) | s1;
    output.push_back((adler >> 24) & 0xFF);
    output.push_back((adler >> 16) & 0xFF);
    output.push_back((adler >> 8) & 0xFF);
    output.push_back(adler & 0xFF);
}

static unsigned long crc32Table[256];
static bool crcTableInit = false;

static void initCrcTable() {
    for (unsigned int i = 0; i < 256; i++) {
        unsigned long c = i;
        for (int j = 0; j < 8; j++) {
            if (c & 1) c = 0xEDB88320UL ^ (c >> 1);
            else c >>= 1;
        }
        crc32Table[i] = c;
    }
    crcTableInit = true;
}

static unsigned long computeCrc(const unsigned char* data, size_t len) {
    if (!crcTableInit) initCrcTable();
    unsigned long crc = 0xFFFFFFFFUL;
    for (size_t i = 0; i < len; i++) {
        crc = crc32Table[(crc ^ data[i]) & 0xFF] ^ (crc >> 8);
    }
    return crc ^ 0xFFFFFFFFUL;
}

std::vector<unsigned char> compressImage(const std::vector<unsigned char>& raw,
                                          int w, int h, int channels) {
    std::vector<unsigned char> out;
    unsigned char sig[] = {137, 80, 78, 71, 13, 10, 26, 10};
    out.insert(out.end(), sig, sig + 8);

    auto writeU32 = [&](unsigned long val) {
        out.push_back((val >> 24) & 0xFF);
        out.push_back((val >> 16) & 0xFF);
        out.push_back((val >> 8) & 0xFF);
        out.push_back(val & 0xFF);
    };

    size_t ihdrStart = out.size();
    out.push_back(0); out.push_back(0); out.push_back(0); out.push_back(13);
    out.insert(out.end(), {'I', 'H', 'D', 'R'});
    writeU32(w);
    writeU32(h);
    out.push_back(8);
    out.push_back(channels == 4 ? 6 : 2);
    out.push_back(0); out.push_back(0); out.push_back(0);
    unsigned long ihdrCrc = computeCrc(out.data() + ihdrStart + 4, out.size() - ihdrStart - 4);
    writeU32(ihdrCrc);

    std::vector<unsigned char> filtered;
    int stride = w * channels;
    filtered.reserve(stride * h + h);
    for (int y = 0; y < h; y++) {
        filtered.push_back(0);
        const unsigned char* row = raw.data() + y * stride;
        filtered.insert(filtered.end(), row, row + stride);
    }

    std::vector<unsigned char> compressed;
    deflateRaw(filtered.data(), filtered.size(), compressed);

    size_t idatStart = out.size();
    writeU32(static_cast<unsigned long>(compressed.size()));
    out.insert(out.end(), {'I', 'D', 'A', 'T'});
    out.insert(out.end(), compressed.begin(), compressed.end());
    unsigned long idatCrc = computeCrc(out.data() + idatStart + 4, out.size() - idatStart - 4);
    writeU32(idatCrc);

    size_t iendStart = out.size();
    out.push_back(0); out.push_back(0); out.push_back(0); out.push_back(0);
    out.insert(out.end(), {'I', 'E', 'N', 'D'});
    unsigned long iendCrc = computeCrc(out.data() + iendStart + 4, 4);
    writeU32(iendCrc);

    return out;
}

}
}
