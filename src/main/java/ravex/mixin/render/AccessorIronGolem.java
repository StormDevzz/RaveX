package ravex.mixin.render;

import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IronGolemModel.class)
public interface AccessorIronGolem {
    @Accessor("head")
    ModelPart getHead();

    @Accessor("rightArm")
    ModelPart getRightArm();

    @Accessor("leftArm")
    ModelPart getLeftArm();

    @Accessor("rightLeg")
    ModelPart getRightLeg();

    @Accessor("leftLeg")
    ModelPart getLeftLeg();
}
