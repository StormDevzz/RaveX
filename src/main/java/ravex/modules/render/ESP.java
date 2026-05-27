package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;

public class ESP extends Module {
    public static final ESP INSTANCE = new ESP();

    public final ModeParameter mode = new ModeParameter("Mode", "Outline", java.util.List.of("Outline", "Box2D"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    // Custom colors for Box2D mode
    public final ColorParameter playerColor = new ColorParameter("Player Color", 0xFFFF3333);
    public final ColorParameter mobColor    = new ColorParameter("Mob Color",    0xFF33FF33);

    private ESP() {
        super("ESP", Category.RENDER);
        addParameter(mode);
        addParameter(players);
        addParameter(monsters);
        addParameter(playerColor);
        addParameter(mobColor);
        // Only show color pickers in Box2D mode
        playerColor.setVisible(() -> mode.getValue().equals("Box2D") && players.getValue());
        mobColor.setVisible(() -> mode.getValue().equals("Box2D") && monsters.getValue());
    }
}
