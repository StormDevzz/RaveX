package ravex.utility.font;

import ravex.utility.render.RavexFontRenderer;

public class FontRenderers {

    private static RavexFontRenderer sfMedium;
    private static RavexFontRenderer comfortaa;
    private static RavexFontRenderer sfBold;

    public static RavexFontRenderer getSfMedium() {
        if (sfMedium == null) {
            sfMedium = RavexFontRenderer.getSfMedium();
        }
        return sfMedium;
    }

    public static RavexFontRenderer getComfortaa() {
        if (comfortaa == null) {
            comfortaa = RavexFontRenderer.getComfortaa();
        }
        return comfortaa;
    }

    public static RavexFontRenderer getSfBold() {
        if (sfBold == null) {
            sfBold = RavexFontRenderer.getSfBold();
        }
        return sfBold;
    }
}
