#pragma once

struct NameTagsLayout {
    double scale;
    double totalWidth;
    double totalHeight;
    double armorRowY;
    double mainRowY;
    double ownerRowY;
    double textYOffset;
    double mainRowWidth;
    double armorRowWidth;
};

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
);
