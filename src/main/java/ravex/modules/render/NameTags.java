package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

public class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();

    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final BooleanParameter handItems = new BooleanParameter("Hand Items", true);

    private NameTags() {
        super("NameTags", Category.RENDER);
        addParameter(armor);
        addParameter(handItems);
    }
}
