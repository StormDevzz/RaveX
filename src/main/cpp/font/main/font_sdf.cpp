#define STB_TRUETYPE_IMPLEMENTATION
#include "font_sdf.hpp"
#include "stb_truetype.hpp"

namespace ravex {
namespace font {

namespace {
constexpr int kAtlasWidth = 1024;
constexpr int kAtlasMaxHeight = 2048;
constexpr int kSdfPadding = 8;
constexpr unsigned char kSdfOnEdge = 180;
constexpr float kSdfDistScale = 180.0f / 8.0f;
}

bool bakeAtlas(const unsigned char* ttfData, int ttfSize, int pixelHeight, BakedAtlas& out) {
    if (!ttfData || ttfSize <= 0 || pixelHeight <= 0) return false;

    stbtt_fontinfo info;
    if (!stbtt_InitFont(&info, ttfData, 0)) return false;

    float scale = stbtt_ScaleForPixelHeight(&info, (float)pixelHeight);

    int fontAscent = 0, fontDescent = 0, fontGap = 0;
    stbtt_GetFontVMetrics(&info, &fontAscent, &fontDescent, &fontGap);
    float ascentPx = fontAscent * scale;

    struct Cell {
        int codepoint;
        int w;
        int h;
        int xoff;
        int yoff;
        int advance;
        unsigned char* sdf;
    };
    std::vector<Cell> cells;
    cells.reserve(320);

    int ranges[3][2] = {{32, 126}, {160, 255}, {1024, 1119}};
    for (int r = 0; r < 3; r++) {
    for (int cp = ranges[r][0]; cp <= ranges[r][1]; cp++) {
        int w = 0, h = 0, xoff = 0, yoff = 0;
        unsigned char* sdf = stbtt_GetCodepointSDF(&info, scale, cp, kSdfPadding, kSdfOnEdge, kSdfDistScale, &w, &h, &xoff, &yoff);
        if (!sdf || w <= 0 || h <= 0) {
            if (sdf) stbtt_FreeSDF(sdf, nullptr);
            continue;
        }
        int advance = 0, lsb = 0;
        stbtt_GetCodepointHMetrics(&info, cp, &advance, &lsb);
        cells.push_back({cp, w, h, xoff, (int)(ascentPx + yoff + 0.5f), (int)(advance * scale + 0.5f), sdf});
    }
    }

    if (cells.empty()) return false;

    std::vector<int> posX(cells.size(), 0);
    std::vector<int> posY(cells.size(), 0);
    int cursorX = 0;
    int cursorY = 0;
    int rowH = 0;
    for (size_t i = 0; i < cells.size(); i++) {
        if (cursorX + cells[i].w > kAtlasWidth) {
            cursorY += rowH;
            cursorX = 0;
            rowH = 0;
        }
        posX[i] = cursorX;
        posY[i] = cursorY;
        cursorX += cells[i].w;
        if (cells[i].h > rowH) rowH = cells[i].h;
    }
    int atlasH = cursorY + rowH;
    if (atlasH > kAtlasMaxHeight) {
        for (auto& c : cells) stbtt_FreeSDF(c.sdf, nullptr);
        return false;
    }

    out.width = kAtlasWidth;
    out.height = atlasH;
    out.pixelHeight = pixelHeight;
    out.rgba.assign((size_t)kAtlasWidth * (size_t)atlasH * 4, 0);

    for (size_t i = 0; i < cells.size(); i++) {
        const Cell& c = cells[i];
        for (int row = 0; row < c.h; row++) {
            for (int col = 0; col < c.w; col++) {
                unsigned char a = c.sdf[(size_t)row * (size_t)c.w + (size_t)col];
                size_t dst = ((size_t)(posY[i] + row) * (size_t)kAtlasWidth + (size_t)(posX[i] + col)) * 4;
                out.rgba[dst] = 255;
                out.rgba[dst + 1] = 255;
                out.rgba[dst + 2] = 255;
                out.rgba[dst + 3] = a;
            }
        }
    }

    out.metrics.clear();
    out.metrics.reserve(4 + cells.size() * 10);
    out.metrics.push_back(kAtlasWidth);
    out.metrics.push_back(atlasH);
    out.metrics.push_back(pixelHeight);
    out.metrics.push_back((int)cells.size());
    for (size_t i = 0; i < cells.size(); i++) {
        const Cell& c = cells[i];
        out.metrics.push_back(c.codepoint);
        out.metrics.push_back(posX[i]);
        out.metrics.push_back(posY[i]);
        out.metrics.push_back(c.w);
        out.metrics.push_back(c.h);
        out.metrics.push_back(c.w);
        out.metrics.push_back(c.h);
        out.metrics.push_back(c.xoff);
        out.metrics.push_back(c.yoff);
        out.metrics.push_back(c.advance);
    }

    for (auto& c : cells) stbtt_FreeSDF(c.sdf, nullptr);
    return true;
}

}
}
