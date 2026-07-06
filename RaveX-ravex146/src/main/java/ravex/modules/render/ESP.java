package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class ESP extends Module {
    public static final ESP INSTANCE = new ESP();

    public final ModeParameter mode = new ModeParameter("Mode", "Outline", java.util.List.of("Outline", "Box2D"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);
    public final BooleanParameter frames = new BooleanParameter("Frames", false);

    public final NumberParameter maxDistance = new NumberParameter("Distance", 100.0, 10.0, 300.0, 10.0);

    public final ColorParameter playerColor = new ColorParameter("Player Color", 0xFFFF3333);
    public final ColorParameter mobColor    = new ColorParameter("Mob Color",    0xFF33FF33);
    public final ColorParameter animalColor = new ColorParameter("Animal Color", 0xFF33FF55);
    public final ColorParameter itemColor   = new ColorParameter("Item Color",   0xFFFFFF33);
    public final ColorParameter frameColor  = new ColorParameter("Frame Color",  0xFFFF9933);

    private ESP() {
        super("ESP", Category.RENDER);
        addParameter(mode);
        addParameter(players);
        addParameter(monsters);
        addParameter(animals);
        addParameter(items);
        addParameter(frames);
        addParameter(maxDistance);
        addParameter(playerColor);
        addParameter(mobColor);
        addParameter(animalColor);
        addParameter(itemColor);
        addParameter(frameColor);

        playerColor.setVisible(players::getValue);
        mobColor.setVisible(monsters::getValue);
        animalColor.setVisible(animals::getValue);
        itemColor.setVisible(items::getValue);
        frameColor.setVisible(frames::getValue);
    }
}
