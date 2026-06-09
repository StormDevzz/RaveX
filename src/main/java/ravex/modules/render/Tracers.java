package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class Tracers extends Module {
    public static final Tracers INSTANCE = new Tracers();

    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", false);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);

    public final NumberParameter maxDistance = new NumberParameter("Distance", 100.0, 10.0, 300.0, 10.0);
    public final NumberParameter lineWidth = new NumberParameter("Width", 1.0, 0.1, 5.0, 0.1);

    public final ColorParameter playerColor = new ColorParameter("Player Color", 0xFFFF3333);
    public final ColorParameter mobColor = new ColorParameter("Mob Color", 0xFF33FF33);
    public final ColorParameter animalColor = new ColorParameter("Animal Color", 0xFF33FF55);
    public final ColorParameter itemColor = new ColorParameter("Item Color", 0xFFFFFF33);

    private Tracers() {
        super("Tracers", Category.RENDER);
        addParameter(players);
        addParameter(monsters);
        addParameter(animals);
        addParameter(items);
        addParameter(maxDistance);
        addParameter(lineWidth);
        addParameter(playerColor);
        addParameter(mobColor);
        addParameter(animalColor);
        addParameter(itemColor);

        playerColor.setVisible(players::getValue);
        mobColor.setVisible(monsters::getValue);
        animalColor.setVisible(animals::getValue);
        itemColor.setVisible(items::getValue);
    }
}
