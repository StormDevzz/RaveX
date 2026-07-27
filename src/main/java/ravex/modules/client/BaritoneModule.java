package ravex.modules.client;

import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.modules.Modules;
@Module(name = "BaritoneModule", category = "Client")
public class BaritoneModule extends ravex.modules.Module {
private final BaritoneIntegration baritone = new BaritoneIntegration();
        @Parameter(name = "CurrentPath", color = true)
    public int colorCurrentPath = 0xFF00AA00;
        @Parameter(name = "NextPath", color = true)
    public int colorNextPath = 0xFF005500;
        @Parameter(name = "BlocksToBreak", color = true)
    public int colorBlocksToBreak = 0xFFFF0000;
        @Parameter(name = "BlocksToPlace", color = true)
    public int colorBlocksToPlace = 0xFF0000FF;
        @Parameter(name = "BlocksToWalkInto", color = true)
    public int colorBlocksToWalkInto = 0xFFFF00FF;
        @Parameter(name = "BestPathSoFar", color = true)
    public int colorBestPathSoFar = 0xFF0000FF;
        @Parameter(name = "RecentConsidered", color = true)
    public int colorMostRecentConsidered = 0xFFFF8800;
        @Parameter(name = "GoalBox", color = true)
    public int colorGoalBox = 0xFFFF0000;
        @Parameter(name = "InvertedGoalBox", color = true)
    public int colorInvertedGoalBox = 0xFFFF00FF;
        @Parameter(name = "Selection", color = true)
    public int colorSelection = 0xFFFFFF00;
        @Parameter(name = "SelectionPos1", color = true)
    public int colorSelectionPos1 = 0xFFFF0000;
        @Parameter(name = "SelectionPos2", color = true)
    public int colorSelectionPos2 = 0xFFFF00FF;
        @Parameter(name = "AllowBreak")
    public boolean allowBreak = true;
        @Parameter(name = "AllowPlace")
    public boolean allowPlace = true;
        @Parameter(name = "AllowSprint")
    public boolean allowSprint = true;
        @Parameter(name = "Parkour")
    public boolean allowParkour = true;
        @Parameter(name = "ParkourPlace")
    public boolean allowParkourPlace = false;
        @Parameter(name = "ParkourAscend")
    public boolean allowParkourAscend = true;
        @Parameter(name = "DiagonalAscend")
    public boolean allowDiagonalAscend = true;
        @Parameter(name = "DiagonalDescend")
    public boolean allowDiagonalDescend = true;
        @Parameter(name = "ClimbVines")
    public boolean allowVines = true;
        @Parameter(name = "AllowInventory")
    public boolean allowInventory = false;
        @Parameter(name = "WaterBucketFall")
    public boolean allowWaterBucketFall = true;
        @Parameter(name = "SprintAscends")
    public boolean sprintAscends = true;
        @Parameter(name = "AssumeStep")
    public boolean assumeStep = false;
        @Parameter(name = "WalkWhileBreaking")
    public boolean walkWhileBreaking = true;
        @Parameter(name = "AntiCheat")
    public boolean antiCheatCompatibility = false;
        @Parameter(name = "CancelOnInvalidate")
    public boolean cancelOnGoalInvalidation = true;
        @Parameter(name = "BlockFreeLook")
    public boolean blockFreeLook = true;
        @Parameter(name = "ElytraFreeLook")
    public boolean elytraFreeLook = true;
        @Parameter(name = "ElytraSmoothLook")
    public boolean elytraSmoothLook = true;
        @Parameter(name = "ConserveFireworks")
    public boolean elytraConserveFireworks = false;
        @Parameter(name = "ElytraAutoJump")
    public boolean elytraAutoJump = true;
        @Parameter(name = "ElytraAutoSwap")
    public boolean elytraAutoSwap = true;
        @Parameter(name = "EmergencyLand")
    public boolean elytraAllowEmergencyLand = true;
        @Parameter(name = "LandOnFortress")
    public boolean elytraAllowLandOnNetherFortress = true;
        @Parameter(name = "RenderPath")
    public boolean renderPath = true;
        @Parameter(name = "RenderGoal")
    public boolean renderGoal = true;
    private int cc, np, cbb, cbp, cbw, bp, mr, gb, ig, sel, s1, s2;
    private boolean ab, ap, asp, apk, app, apa, ada, add_, av, ai, awb, sa, as_, wwb, acc, cog, bfl;
    private boolean elf, esl, ecf, eaj, eas, eael, ealnf, rp, rg;
    protected void onEnable() {
        if (baritone.init()) {
            syncAll();
        }
    }
    protected void onDisable() {
    }
    public void onTick() {
        if (!baritone.isAvailable()) {
            if (Modules.enabled(BaritoneModule.class) && baritone.init()) {
                syncAll();
            }
            return;
        }
        syncChanged();
    }
    private void syncAll() {
        cc = colorCurrentPath;
        np = colorNextPath;
        cbb = colorBlocksToBreak;
        cbp = colorBlocksToPlace;
        cbw = colorBlocksToWalkInto;
        bp = colorBestPathSoFar;
        mr = colorMostRecentConsidered;
        gb = colorGoalBox;
        ig = colorInvertedGoalBox;
        sel = colorSelection;
        s1 = colorSelectionPos1;
        s2 = colorSelectionPos2;
        ab = allowBreak;
        ap = allowPlace;
        asp = allowSprint;
        apk = allowParkour;
        app = allowParkourPlace;
        apa = allowParkourAscend;
        ada = allowDiagonalAscend;
        add_ = allowDiagonalDescend;
        av = allowVines;
        ai = allowInventory;
        awb = allowWaterBucketFall;
        sa = sprintAscends;
        as_ = assumeStep;
        wwb = walkWhileBreaking;
        acc = antiCheatCompatibility;
        cog = cancelOnGoalInvalidation;
        bfl = blockFreeLook;
        elf = elytraFreeLook;
        esl = elytraSmoothLook;
        ecf = elytraConserveFireworks;
        eaj = elytraAutoJump;
        eas = elytraAutoSwap;
        eael = elytraAllowEmergencyLand;
        ealnf = elytraAllowLandOnNetherFortress;
        rp = renderPath;
        rg = renderGoal;
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
        v = colorCurrentPath;
        if (v != cc) { cc = v; baritone.applyColor("colorCurrentPath", v); }
        v = colorNextPath;
        if (v != np) { np = v; baritone.applyColor("colorNextPath", v); }
        v = colorBlocksToBreak;
        if (v != cbb) { cbb = v; baritone.applyColor("colorBlocksToBreak", v); }
        v = colorBlocksToPlace;
        if (v != cbp) { cbp = v; baritone.applyColor("colorBlocksToPlace", v); }
        v = colorBlocksToWalkInto;
        if (v != cbw) { cbw = v; baritone.applyColor("colorBlocksToWalkInto", v); }
        v = colorBestPathSoFar;
        if (v != bp) { bp = v; baritone.applyColor("colorBestPathSoFar", v); }
        v = colorMostRecentConsidered;
        if (v != mr) { mr = v; baritone.applyColor("colorMostRecentConsidered", v); }
        v = colorGoalBox;
        if (v != gb) { gb = v; baritone.applyColor("colorGoalBox", v); }
        v = colorInvertedGoalBox;
        if (v != ig) { ig = v; baritone.applyColor("colorInvertedGoalBox", v); }
        v = colorSelection;
        if (v != sel) { sel = v; baritone.applyColor("colorSelection", v); }
        v = colorSelectionPos1;
        if (v != s1) { s1 = v; baritone.applyColor("colorSelectionPos1", v); }
        v = colorSelectionPos2;
        if (v != s2) { s2 = v; baritone.applyColor("colorSelectionPos2", v); }
        boolean b;
        b = allowBreak; if (b != ab) { ab = b; baritone.applyBoolean("allowBreak", b); }
        b = allowPlace; if (b != ap) { ap = b; baritone.applyBoolean("allowPlace", b); }
        b = allowSprint; if (b != asp) { asp = b; baritone.applyBoolean("allowSprint", b); }
        b = allowParkour; if (b != apk) { apk = b; baritone.applyBoolean("allowParkour", b); }
        b = allowParkourPlace; if (b != app) { app = b; baritone.applyBoolean("allowParkourPlace", b); }
        b = allowParkourAscend; if (b != apa) { apa = b; baritone.applyBoolean("allowParkourAscend", b); }
        b = allowDiagonalAscend; if (b != ada) { ada = b; baritone.applyBoolean("allowDiagonalAscend", b); }
        b = allowDiagonalDescend; if (b != add_) { add_ = b; baritone.applyBoolean("allowDiagonalDescend", b); }
        b = allowVines; if (b != av) { av = b; baritone.applyBoolean("allowVines", b); }
        b = allowInventory; if (b != ai) { ai = b; baritone.applyBoolean("allowInventory", b); }
        b = allowWaterBucketFall; if (b != awb) { awb = b; baritone.applyBoolean("allowWaterBucketFall", b); }
        b = sprintAscends; if (b != sa) { sa = b; baritone.applyBoolean("sprintAscends", b); }
        b = assumeStep; if (b != as_) { as_ = b; baritone.applyBoolean("assumeStep", b); }
        b = walkWhileBreaking; if (b != wwb) { wwb = b; baritone.applyBoolean("walkWhileBreaking", b); }
        b = antiCheatCompatibility; if (b != acc) { acc = b; baritone.applyBoolean("antiCheatCompatibility", b); }
        b = cancelOnGoalInvalidation; if (b != cog) { cog = b; baritone.applyBoolean("cancelOnGoalInvalidation", b); }
        b = blockFreeLook; if (b != bfl) { bfl = b; baritone.applyBoolean("blockFreeLook", b); }
        b = elytraFreeLook; if (b != elf) { elf = b; baritone.applyBoolean("elytraFreeLook", b); }
        b = elytraSmoothLook; if (b != esl) { esl = b; baritone.applyBoolean("elytraSmoothLook", b); }
        b = elytraConserveFireworks; if (b != ecf) { ecf = b; baritone.applyBoolean("elytraConserveFireworks", b); }
        b = elytraAutoJump; if (b != eaj) { eaj = b; baritone.applyBoolean("elytraAutoJump", b); }
        b = elytraAutoSwap; if (b != eas) { eas = b; baritone.applyBoolean("elytraAutoSwap", b); }
        b = elytraAllowEmergencyLand; if (b != eael) { eael = b; baritone.applyBoolean("elytraAllowEmergencyLand", b); }
        b = elytraAllowLandOnNetherFortress; if (b != ealnf) { ealnf = b; baritone.applyBoolean("elytraAllowLandOnNetherFortress", b); }
        b = renderPath; if (b != rp) { rp = b; baritone.applyBoolean("renderPath", b); }
        b = renderGoal; if (b != rg) { rg = b; baritone.applyBoolean("renderGoal", b); }
    }
    private void syncBoolean(String name, boolean value) {
        baritone.applyBoolean(name, value);
    }



}