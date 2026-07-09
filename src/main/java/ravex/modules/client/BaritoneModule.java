package ravex.modules.client;
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class BaritoneModule extends Module {
    private final BaritoneIntegration baritone = new BaritoneIntegration();
    public final ColorParameter colorCurrentPath = new ColorParameter("CurrentPath", 0xFF00AA00);
    public final ColorParameter colorNextPath = new ColorParameter("NextPath", 0xFF005500);
    public final ColorParameter colorBlocksToBreak = new ColorParameter("BlocksToBreak", 0xFFFF0000);
    public final ColorParameter colorBlocksToPlace = new ColorParameter("BlocksToPlace", 0xFF0000FF);
    public final ColorParameter colorBlocksToWalkInto = new ColorParameter("BlocksToWalkInto", 0xFFFF00FF);
    public final ColorParameter colorBestPathSoFar = new ColorParameter("BestPathSoFar", 0xFF0000FF);
    public final ColorParameter colorMostRecentConsidered = new ColorParameter("RecentConsidered", 0xFFFF8800);
    public final ColorParameter colorGoalBox = new ColorParameter("GoalBox", 0xFFFF0000);
    public final ColorParameter colorInvertedGoalBox = new ColorParameter("InvertedGoalBox", 0xFFFF00FF);
    public final ColorParameter colorSelection = new ColorParameter("Selection", 0xFFFFFF00);
    public final ColorParameter colorSelectionPos1 = new ColorParameter("SelectionPos1", 0xFFFF0000);
    public final ColorParameter colorSelectionPos2 = new ColorParameter("SelectionPos2", 0xFFFF00FF);
    public final BooleanParameter allowBreak = new BooleanParameter("AllowBreak", true);
    public final BooleanParameter allowPlace = new BooleanParameter("AllowPlace", true);
    public final BooleanParameter allowSprint = new BooleanParameter("AllowSprint", true);
    public final BooleanParameter allowParkour = new BooleanParameter("Parkour", true);
    public final BooleanParameter allowParkourPlace = new BooleanParameter("ParkourPlace", false);
    public final BooleanParameter allowParkourAscend = new BooleanParameter("ParkourAscend", true);
    public final BooleanParameter allowDiagonalAscend = new BooleanParameter("DiagonalAscend", true);
    public final BooleanParameter allowDiagonalDescend = new BooleanParameter("DiagonalDescend", true);
    public final BooleanParameter allowVines = new BooleanParameter("ClimbVines", true);
    public final BooleanParameter allowInventory = new BooleanParameter("AllowInventory", false);
    public final BooleanParameter allowWaterBucketFall = new BooleanParameter("WaterBucketFall", true);
    public final BooleanParameter sprintAscends = new BooleanParameter("SprintAscends", true);
    public final BooleanParameter assumeStep = new BooleanParameter("AssumeStep", false);
    public final BooleanParameter walkWhileBreaking = new BooleanParameter("WalkWhileBreaking", true);
    public final BooleanParameter antiCheatCompatibility = new BooleanParameter("AntiCheat", false);
    public final BooleanParameter cancelOnGoalInvalidation = new BooleanParameter("CancelOnInvalidate", true);
    public final BooleanParameter blockFreeLook = new BooleanParameter("BlockFreeLook", true);
    public final BooleanParameter elytraFreeLook = new BooleanParameter("ElytraFreeLook", true);
    public final BooleanParameter elytraSmoothLook = new BooleanParameter("ElytraSmoothLook", true);
    public final BooleanParameter elytraConserveFireworks = new BooleanParameter("ConserveFireworks", false);
    public final BooleanParameter elytraAutoJump = new BooleanParameter("ElytraAutoJump", true);
    public final BooleanParameter elytraAutoSwap = new BooleanParameter("ElytraAutoSwap", true);
    public final BooleanParameter elytraAllowEmergencyLand = new BooleanParameter("EmergencyLand", true);
    public final BooleanParameter elytraAllowLandOnNetherFortress = new BooleanParameter("LandOnFortress", true);
    public final BooleanParameter renderPath = new BooleanParameter("RenderPath", true);
    public final BooleanParameter renderGoal = new BooleanParameter("RenderGoal", true);
    private int cc, np, cbb, cbp, cbw, bp, mr, gb, ig, sel, s1, s2;
    private boolean ab, ap, asp, apk, app, apa, ada, add_, av, ai, awb, sa, as_, wwb, acc, cog, bfl;
    private boolean elf, esl, ecf, eaj, eas, eael, ealnf, rp, rg;
    private BaritoneModule() {
        super("Baritone");
        setVisibleCondition(BaritoneIntegration::isBaritonePresent);
        if (isVisible()) {
            super.setEnabled(true);
        }
    }
    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            if (!getEnabled()) {
                super.setEnabled(true);
            }
            if (baritone.init()) {
                syncAll();
            }
        }
    }
    @Override
    public void toggle() {
        if (!getEnabled()) {
            super.setEnabled(true);
        }
    }
    @Override
    protected boolean hasToggleSound() {
        return false;
    }
    @Override
    protected void onEnable() {
        if (baritone.init()) {
            syncAll();
        }
    }
    @Override
    protected void onDisable() {
    }
    @Override
    public void onTick() {
        if (!baritone.isAvailable()) {
            if (getEnabled() && baritone.init()) {
                syncAll();
            }
            return;
        }
        syncChanged();
    }
    private void syncAll() {
        cc = colorCurrentPath.getValue();
        np = colorNextPath.getValue();
        cbb = colorBlocksToBreak.getValue();
        cbp = colorBlocksToPlace.getValue();
        cbw = colorBlocksToWalkInto.getValue();
        bp = colorBestPathSoFar.getValue();
        mr = colorMostRecentConsidered.getValue();
        gb = colorGoalBox.getValue();
        ig = colorInvertedGoalBox.getValue();
        sel = colorSelection.getValue();
        s1 = colorSelectionPos1.getValue();
        s2 = colorSelectionPos2.getValue();
        ab = allowBreak.getValue();
        ap = allowPlace.getValue();
        asp = allowSprint.getValue();
        apk = allowParkour.getValue();
        app = allowParkourPlace.getValue();
        apa = allowParkourAscend.getValue();
        ada = allowDiagonalAscend.getValue();
        add_ = allowDiagonalDescend.getValue();
        av = allowVines.getValue();
        ai = allowInventory.getValue();
        awb = allowWaterBucketFall.getValue();
        sa = sprintAscends.getValue();
        as_ = assumeStep.getValue();
        wwb = walkWhileBreaking.getValue();
        acc = antiCheatCompatibility.getValue();
        cog = cancelOnGoalInvalidation.getValue();
        bfl = blockFreeLook.getValue();
        elf = elytraFreeLook.getValue();
        esl = elytraSmoothLook.getValue();
        ecf = elytraConserveFireworks.getValue();
        eaj = elytraAutoJump.getValue();
        eas = elytraAutoSwap.getValue();
        eael = elytraAllowEmergencyLand.getValue();
        ealnf = elytraAllowLandOnNetherFortress.getValue();
        rp = renderPath.getValue();
        rg = renderGoal.getValue();
        baritone.applyColor("colorCurrentPath", cc);
        baritone.applyColor("colorNextPath", np);
        baritone.applyColor("colorBlocksToBreak", cbb);
        baritone.applyColor("colorBlocksToPlace", cbp);
        baritone.applyColor("colorBlocksToWalkInto", cbw);
        baritone.applyColor("colorBestPathSoFar", bp);
        baritone.applyColor("colorMostRecentConsidered", mr);
        baritone.applyColor("colorGoalBox", gb);
        baritone.applyColor("colorInvertedGoalBox", ig);
        baritone.applyColor("colorSelection", sel);
        baritone.applyColor("colorSelectionPos1", s1);
        baritone.applyColor("colorSelectionPos2", s2);
        syncBoolean("allowBreak", ab);
        syncBoolean("allowPlace", ap);
        syncBoolean("allowSprint", asp);
        syncBoolean("allowParkour", apk);
        syncBoolean("allowParkourPlace", app);
        syncBoolean("allowParkourAscend", apa);
        syncBoolean("allowDiagonalAscend", ada);
        syncBoolean("allowDiagonalDescend", add_);
        syncBoolean("allowVines", av);
        syncBoolean("allowInventory", ai);
        syncBoolean("allowWaterBucketFall", awb);
        syncBoolean("sprintAscends", sa);
        syncBoolean("assumeStep", as_);
        syncBoolean("walkWhileBreaking", wwb);
        syncBoolean("antiCheatCompatibility", acc);
        syncBoolean("cancelOnGoalInvalidation", cog);
        syncBoolean("blockFreeLook", bfl);
        syncBoolean("elytraFreeLook", elf);
        syncBoolean("elytraSmoothLook", esl);
        syncBoolean("elytraConserveFireworks", ecf);
        syncBoolean("elytraAutoJump", eaj);
        syncBoolean("elytraAutoSwap", eas);
        syncBoolean("elytraAllowEmergencyLand", eael);
        syncBoolean("elytraAllowLandOnNetherFortress", ealnf);
        syncBoolean("renderPath", rp);
        syncBoolean("renderGoal", rg);
    }
    private void syncChanged() {
        int v;
        v = colorCurrentPath.getValue();
        if (v != cc) { cc = v; baritone.applyColor("colorCurrentPath", v); }
        v = colorNextPath.getValue();
        if (v != np) { np = v; baritone.applyColor("colorNextPath", v); }
        v = colorBlocksToBreak.getValue();
        if (v != cbb) { cbb = v; baritone.applyColor("colorBlocksToBreak", v); }
        v = colorBlocksToPlace.getValue();
        if (v != cbp) { cbp = v; baritone.applyColor("colorBlocksToPlace", v); }
        v = colorBlocksToWalkInto.getValue();
        if (v != cbw) { cbw = v; baritone.applyColor("colorBlocksToWalkInto", v); }
        v = colorBestPathSoFar.getValue();
        if (v != bp) { bp = v; baritone.applyColor("colorBestPathSoFar", v); }
        v = colorMostRecentConsidered.getValue();
        if (v != mr) { mr = v; baritone.applyColor("colorMostRecentConsidered", v); }
        v = colorGoalBox.getValue();
        if (v != gb) { gb = v; baritone.applyColor("colorGoalBox", v); }
        v = colorInvertedGoalBox.getValue();
        if (v != ig) { ig = v; baritone.applyColor("colorInvertedGoalBox", v); }
        v = colorSelection.getValue();
        if (v != sel) { sel = v; baritone.applyColor("colorSelection", v); }
        v = colorSelectionPos1.getValue();
        if (v != s1) { s1 = v; baritone.applyColor("colorSelectionPos1", v); }
        v = colorSelectionPos2.getValue();
        if (v != s2) { s2 = v; baritone.applyColor("colorSelectionPos2", v); }
        boolean b;
        b = allowBreak.getValue(); if (b != ab) { ab = b; baritone.applyBoolean("allowBreak", b); }
        b = allowPlace.getValue(); if (b != ap) { ap = b; baritone.applyBoolean("allowPlace", b); }
        b = allowSprint.getValue(); if (b != asp) { asp = b; baritone.applyBoolean("allowSprint", b); }
        b = allowParkour.getValue(); if (b != apk) { apk = b; baritone.applyBoolean("allowParkour", b); }
        b = allowParkourPlace.getValue(); if (b != app) { app = b; baritone.applyBoolean("allowParkourPlace", b); }
        b = allowParkourAscend.getValue(); if (b != apa) { apa = b; baritone.applyBoolean("allowParkourAscend", b); }
        b = allowDiagonalAscend.getValue(); if (b != ada) { ada = b; baritone.applyBoolean("allowDiagonalAscend", b); }
        b = allowDiagonalDescend.getValue(); if (b != add_) { add_ = b; baritone.applyBoolean("allowDiagonalDescend", b); }
        b = allowVines.getValue(); if (b != av) { av = b; baritone.applyBoolean("allowVines", b); }
        b = allowInventory.getValue(); if (b != ai) { ai = b; baritone.applyBoolean("allowInventory", b); }
        b = allowWaterBucketFall.getValue(); if (b != awb) { awb = b; baritone.applyBoolean("allowWaterBucketFall", b); }
        b = sprintAscends.getValue(); if (b != sa) { sa = b; baritone.applyBoolean("sprintAscends", b); }
        b = assumeStep.getValue(); if (b != as_) { as_ = b; baritone.applyBoolean("assumeStep", b); }
        b = walkWhileBreaking.getValue(); if (b != wwb) { wwb = b; baritone.applyBoolean("walkWhileBreaking", b); }
        b = antiCheatCompatibility.getValue(); if (b != acc) { acc = b; baritone.applyBoolean("antiCheatCompatibility", b); }
        b = cancelOnGoalInvalidation.getValue(); if (b != cog) { cog = b; baritone.applyBoolean("cancelOnGoalInvalidation", b); }
        b = blockFreeLook.getValue(); if (b != bfl) { bfl = b; baritone.applyBoolean("blockFreeLook", b); }
        b = elytraFreeLook.getValue(); if (b != elf) { elf = b; baritone.applyBoolean("elytraFreeLook", b); }
        b = elytraSmoothLook.getValue(); if (b != esl) { esl = b; baritone.applyBoolean("elytraSmoothLook", b); }
        b = elytraConserveFireworks.getValue(); if (b != ecf) { ecf = b; baritone.applyBoolean("elytraConserveFireworks", b); }
        b = elytraAutoJump.getValue(); if (b != eaj) { eaj = b; baritone.applyBoolean("elytraAutoJump", b); }
        b = elytraAutoSwap.getValue(); if (b != eas) { eas = b; baritone.applyBoolean("elytraAutoSwap", b); }
        b = elytraAllowEmergencyLand.getValue(); if (b != eael) { eael = b; baritone.applyBoolean("elytraAllowEmergencyLand", b); }
        b = elytraAllowLandOnNetherFortress.getValue(); if (b != ealnf) { ealnf = b; baritone.applyBoolean("elytraAllowLandOnNetherFortress", b); }
        b = renderPath.getValue(); if (b != rp) { rp = b; baritone.applyBoolean("renderPath", b); }
        b = renderGoal.getValue(); if (b != rg) { rg = b; baritone.applyBoolean("renderGoal", b); }
    }
    private void syncBoolean(String name, boolean value) {
        baritone.applyBoolean(name, value);
    }

    public static BaritoneModule itz() {
        return ModuleManager.get(BaritoneModule.class);
    }
}
