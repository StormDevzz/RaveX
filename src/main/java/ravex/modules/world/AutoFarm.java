package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.FarmUtility;

import java.util.ArrayList;
import java.util.List;

@Module(name = "AutoFarm", category = "World")
public class AutoFarm {
    @Parameter(name = "Target", modes = {"Range", "Aim"})
    public String targetMode = "Range";
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.5;
    @Parameter(name = "Harvest")
    public boolean harvest = true;
    @Parameter(name = "Replant")
    public boolean replant = true;
    @Parameter(name = "Delay", min = 50, max = 1000, step = 50)
    public double delay = 150;
    @Parameter(name = "SilentSwap")
    public boolean silentSwap = true;
    @Parameter(name = "Plants", options = {
        "Wheat", "Carrots", "Potatoes", "Beetroots", "NetherWart",
        "Pumpkin", "Melon", "SugarCane", "Cactus", "Bamboo", "Berries"
    })
    public List<String> plants = new ArrayList<>(List.of(
        "Wheat", "Carrots", "Potatoes", "Beetroots", "NetherWart",
        "Pumpkin", "Melon", "SugarCane", "Cactus", "Bamboo", "Berries"
    ));
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0xFF40A9FF;
    @Parameter(name = "Highlight")
    public boolean highlight = true;
    @Parameter(name = "HighlightDuration", min = 100, max = 5000, step = 100, visible = "highlight")
    public int highlightDuration = 1000;
    @Parameter(name = "HighlightColor", color = true, visible = "highlight")
    public int highlightColor = 0xFF40A9FF;
    @Parameter(name = "HighlightFilled", visible = "highlight")
    public boolean highlightFilled = true;
    @Parameter(name = "HighlightWireframe", visible = "highlight")
    public boolean highlightWireframe = true;

    public void onEnable() {
        FarmUtility.reset();
    }

    public void onDisable() {
        FarmUtility.reset();
    }

    public void onTick() {
        FarmUtility.configure(targetMode, range, harvest, replant, delay, silentSwap, plants);
        FarmUtility.tick();
    }
}
