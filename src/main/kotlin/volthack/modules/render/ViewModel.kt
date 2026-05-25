// [ignoring loop detection]
package volthack.modules.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.world.InteractionHand
import volthack.setting.Category
import volthack.setting.Module

object ViewModel : Module("ViewModel", "Allows highly customizing item rendering positions in first person", Category.RENDER) {
    private val independentObj = boolean("Independent Hands", false, "Customize main and off hand separately")
    val independent by independentObj

    // Linked Settings (visible if Independent Hands is false)
    private val posXObj = float("Pos X", 0.0f, -2.0f, 2.0f, 0.05f, "Horizontal position offset")
    private val posYObj = float("Pos Y", 0.0f, -2.0f, 2.0f, 0.05f, "Vertical position offset")
    private val posZObj = float("Pos Z", 0.0f, -2.0f, 2.0f, 0.05f, "Depth position offset")
    val posX by posXObj
    val posY by posYObj
    val posZ by posZObj

    private val scaleXObj = float("Scale X", 1.0f, 0.1f, 3.0f, 0.05f, "Horizontal scale multiplier")
    private val scaleYObj = float("Scale Y", 1.0f, 0.1f, 3.0f, 0.05f, "Vertical scale multiplier")
    private val scaleZObj = float("Scale Z", 1.0f, 0.1f, 3.0f, 0.05f, "Depth scale multiplier")
    val scaleX by scaleXObj
    val scaleY by scaleYObj
    val scaleZ by scaleZObj

    private val rotXObj = float("Rot X", 0.0f, -180.0f, 180.0f, 1.0f, "Rotation around X axis")
    private val rotYObj = float("Rot Y", 0.0f, -180.0f, 180.0f, 1.0f, "Rotation around Y axis")
    private val rotZObj = float("Rot Z", 0.0f, -180.0f, 180.0f, 1.0f, "Rotation around Z axis")
    val rotX by rotXObj
    val rotY by rotYObj
    val rotZ by rotZObj

    // Main Hand Settings (visible if Independent Hands is true)
    private val mPosXObj = float("Main X", 0.0f, -2.0f, 2.0f, 0.05f, "Main hand Pos X")
    private val mPosYObj = float("Main Y", 0.0f, -2.0f, 2.0f, 0.05f, "Main hand Pos Y")
    private val mPosZObj = float("Main Z", 0.0f, -2.0f, 2.0f, 0.05f, "Main hand Pos Z")
    val mPosX by mPosXObj
    val mPosY by mPosYObj
    val mPosZ by mPosZObj

    private val mScaleXObj = float("Main S-X", 1.0f, 0.1f, 3.0f, 0.05f, "Main hand Scale X")
    private val mScaleYObj = float("Main S-Y", 1.0f, 0.1f, 3.0f, 0.05f, "Main hand Scale Y")
    private val mScaleZObj = float("Main S-Z", 1.0f, 0.1f, 3.0f, 0.05f, "Main hand Scale Z")
    val mScaleX by mScaleXObj
    val mScaleY by mScaleYObj
    val mScaleZ by mScaleZObj

    private val mRotXObj = float("Main R-X", 0.0f, -180.0f, 180.0f, 1.0f, "Main hand Rot X")
    private val mRotYObj = float("Main R-Y", 0.0f, -180.0f, 180.0f, 1.0f, "Main hand Rot Y")
    private val mRotZObj = float("Main R-Z", 0.0f, -180.0f, 180.0f, 1.0f, "Main hand Rot Z")
    val mRotX by mRotXObj
    val mRotY by mRotYObj
    val mRotZ by mRotZObj

    // Off Hand Settings (visible if Independent Hands is true)
    private val oPosXObj = float("Off X", 0.0f, -2.0f, 2.0f, 0.05f, "Off hand Pos X")
    private val oPosYObj = float("Off Y", 0.0f, -2.0f, 2.0f, 0.05f, "Off hand Pos Y")
    private val oPosZObj = float("Off Z", 0.0f, -2.0f, 2.0f, 0.05f, "Off hand Pos Z")
    val oPosX by oPosXObj
    val oPosY by oPosYObj
    val oPosZ by oPosZObj

    private val oScaleXObj = float("Off S-X", 1.0f, 0.1f, 3.0f, 0.05f, "Off hand Scale X")
    private val oScaleYObj = float("Off S-Y", 1.0f, 0.1f, 3.0f, 0.05f, "Off hand Scale Y")
    private val oScaleZObj = float("Off S-Z", 1.0f, 0.1f, 3.0f, 0.05f, "Off hand Scale Z")
    val oScaleX by oScaleXObj
    val oScaleY by oScaleYObj
    val oScaleZ by oScaleZObj

    private val oRotXObj = float("Off R-X", 0.0f, -180.0f, 180.0f, 1.0f, "Off hand Rot X")
    private val oRotYObj = float("Off R-Y", 0.0f, -180.0f, 180.0f, 1.0f, "Off hand Rot Y")
    private val oRotZObj = float("Off R-Z", 0.0f, -180.0f, 180.0f, 1.0f, "Off hand Rot Z")
    val oRotX by oRotXObj
    val oRotY by oRotYObj
    val oRotZ by oRotZObj

    init {
        // Linked conditions (show if NOT independent)
        val linked = { !independent }
        posXObj.showIf(linked)
        posYObj.showIf(linked)
        posZObj.showIf(linked)
        scaleXObj.showIf(linked)
        scaleYObj.showIf(linked)
        scaleZObj.showIf(linked)
        rotXObj.showIf(linked)
        rotYObj.showIf(linked)
        rotZObj.showIf(linked)

        // Independent conditions (show if independent)
        val indep = { independent }
        mPosXObj.showIf(indep)
        mPosYObj.showIf(indep)
        mPosZObj.showIf(indep)
        mScaleXObj.showIf(indep)
        mScaleYObj.showIf(indep)
        mScaleZObj.showIf(indep)
        mRotXObj.showIf(indep)
        mRotYObj.showIf(indep)
        mRotZObj.showIf(indep)

        oPosXObj.showIf(indep)
        oPosYObj.showIf(indep)
        oPosZObj.showIf(indep)
        oScaleXObj.showIf(indep)
        oScaleYObj.showIf(indep)
        oScaleZObj.showIf(indep)
        oRotXObj.showIf(indep)
        oRotYObj.showIf(indep)
        oRotZObj.showIf(indep)
    }

    fun transform(poseStack: PoseStack, hand: InteractionHand) {
        val isMainHand = hand == InteractionHand.MAIN_HAND

        val tx: Float
        val ty: Float
        val tz: Float
        val sx: Float
        val sy: Float
        val sz: Float
        val rx: Float
        val ry: Float
        val rz: Float

        if (independent) {
            if (isMainHand) {
                tx = mPosX
                ty = mPosY
                tz = mPosZ
                sx = mScaleX
                sy = mScaleY
                sz = mScaleZ
                rx = mRotX
                ry = mRotY
                rz = mRotZ
            } else {
                tx = oPosX
                ty = oPosY
                tz = oPosZ
                sx = oScaleX
                sy = oScaleY
                sz = oScaleZ
                rx = oRotX
                ry = oRotY
                rz = oRotZ
            }
        } else {
            // Mirror X translation for off hand to keep them symmetric
            tx = if (isMainHand) posX else -posX
            ty = posY
            tz = posZ
            sx = scaleX
            sy = scaleY
            sz = scaleZ
            rx = rotX
            ry = rotY
            rz = rotZ
        }

        poseStack.translate(tx.toDouble(), ty.toDouble(), tz.toDouble())
        poseStack.scale(sx, sy, sz)

        poseStack.mulPose(Axis.XP.rotationDegrees(rx))
        poseStack.mulPose(Axis.YP.rotationDegrees(ry))
        poseStack.mulPose(Axis.ZP.rotationDegrees(rz))
    }
}
