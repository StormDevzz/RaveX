package ravex.mixin.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ravex.utility.render.CorpseRenderStateUtility;

@Mixin(LivingEntityRenderState.class)
public class MixinLivingEntityRenderState implements CorpseRenderStateUtility {
    @Unique
    private boolean ravexCorpse;
    @Unique
    private boolean ravexLogoutSpot;
    @Unique
    private float ravexLimbSwing;
    @Unique
    private float ravexLimbPhase;

    @Override
    public boolean isRavexCorpse() {
        return ravexCorpse;
    }

    @Override
    public void setRavexCorpse(boolean corpse) {
        this.ravexCorpse = corpse;
    }

    @Override
    public boolean isRavexLogoutSpot() {
        return ravexLogoutSpot;
    }

    @Override
    public void setRavexLogoutSpot(boolean logoutSpot) {
        this.ravexLogoutSpot = logoutSpot;
    }

    @Override
    public float ravexLimbSwing() {
        return ravexLimbSwing;
    }

    @Override
    public void setRavexLimbSwing(float swing) {
        this.ravexLimbSwing = swing;
    }

    @Override
    public float ravexLimbPhase() {
        return ravexLimbPhase;
    }

    @Override
    public void setRavexLimbPhase(float phase) {
        this.ravexLimbPhase = phase;
    }
}
