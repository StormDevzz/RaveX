package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;

public class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();

    private NameTags() {
        super("NameTags", Category.RENDER);
    }
}
