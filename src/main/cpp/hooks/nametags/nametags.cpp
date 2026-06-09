#include "nametags.h"
#include <algorithm>

NameTagsLayout calculateNameTagsLayout(
    double distance,
    double scaleParam,
    bool distanceScaling,
    bool showArmor,
    bool showHands,
    bool hasOwner,
    double tw,
    double ow,
    bool hasMainHand,
    bool hasOffHand,
    int armorCount
) {
    // 1. Calculate Scale
    double scale = scaleParam;
    if (distanceScaling) {
        scale = scaleParam * (distance * 0.15);
        if (scale < 0.5) scale = 0.5;
        if (scale > 3.0) scale = 3.0;
    }

    // 2. Constants
    const double is = 16.0;
    const double gap = 2.0;
    const double padding = 3.0;

    // 3. Row Widths
    double armorRowWidth = 0.0;
    bool hasArmorRow = showArmor && (armorCount > 0);
    if (hasArmorRow) {
        armorRowWidth = armorCount * is + (armorCount - 1) * gap;
    }

    double mainRowWidth = tw;
    bool hasMainHandItem = showHands && hasMainHand;
    bool hasOffHandItem = showHands && hasOffHand;
    if (hasMainHandItem) {
        mainRowWidth += is + gap;
    }
    if (hasOffHandItem) {
        mainRowWidth += gap + is;
    }

    double ownerRowWidth = hasOwner ? ow : 0.0;

    double totalWidth = std::max(mainRowWidth, std::max(armorRowWidth, ownerRowWidth));

    // 4. Row Heights
    double mainRowHeight = 9.0;
    double textYOffset = 0.0;
    if (hasMainHandItem || hasOffHandItem) {
        mainRowHeight = is;
        textYOffset = (is - 9.0) / 2.0; // Center text vertically
    }

    // 5. Total Height Calculation
    double totalHeight = 0.0;
    if (hasArmorRow) {
        totalHeight += is + gap;
    }
    // Main row (always present for NameTags display, unless only showing owner for some reason)
    // Let's assume main row is present if tw > 0
    bool hasMainRow = (tw > 0);
    if (hasMainRow) {
        totalHeight += mainRowHeight;
    }
    if (hasOwner) {
        if (hasMainRow || hasArmorRow) {
            totalHeight += gap + 9.0;
        } else {
            totalHeight += 9.0;
        }
    }
    totalHeight += 2 * padding;

    // 6. Y offsets relative to projected head coordinate (where Y = 0 is the head)
    double bgBottom = -2.0;
    double bgTop = bgBottom - totalHeight;
    double contentTop = bgTop + padding;

    double armorRowY = 0.0;
    if (hasArmorRow) {
        armorRowY = contentTop;
        contentTop += is + gap;
    }

    double mainRowY = 0.0;
    if (hasMainRow) {
        mainRowY = contentTop;
        contentTop += mainRowHeight;
    }

    double ownerRowY = 0.0;
    if (hasOwner) {
        if (hasMainRow || hasArmorRow) {
            ownerRowY = contentTop + gap;
        } else {
            ownerRowY = contentTop;
        }
    }

    NameTagsLayout layout;
    layout.scale = scale;
    layout.totalWidth = totalWidth;
    layout.totalHeight = totalHeight;
    layout.armorRowY = armorRowY;
    layout.mainRowY = mainRowY;
    layout.ownerRowY = ownerRowY;
    layout.textYOffset = textYOffset;
    layout.mainRowWidth = mainRowWidth;
    layout.armorRowWidth = armorRowWidth;

    return layout;
}
