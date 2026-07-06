package ravex.parameter;

public class ActionParameter extends Parameter<Runnable> {
    public ActionParameter(String name, Runnable action) {
        super(name, action);
    }
}
