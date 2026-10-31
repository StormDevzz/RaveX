package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "MoreChatHistory", category = "Misc")
public class MoreChatHistory {
    @Parameter(name = "Limit", min = 100, max = 1000, step = 50)
    public double limit = 1000;
}
