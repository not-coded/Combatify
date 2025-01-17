package net.atlas.combatify.mixin.cookey.compatibility.optifabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.atlas.combatify.CombatifyClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.LivingEntity;
import net.atlas.combatify.annotation.mixin.ModSpecific;
import net.atlas.combatify.config.cookey.option.BooleanOption;
import net.atlas.combatify.extensions.OverlayRendered;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
@ModSpecific("optifabric")
public abstract class OFHumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> implements OverlayRendered<T> {
    @Shadow
    public abstract void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T livingEntity, float f, float g, float h, float j, float k, float l);


    @Unique
	private int overlayCoords;

	@Unique
	private BooleanOption showDamageTintOnArmor;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void injectOptions(RenderLayerParent<T, M> renderLayerParent, A humanoidModel, A humanoidModel2, ModelManager modelManager, CallbackInfo ci) {
		showDamageTintOnArmor = CombatifyClient.getInstance().getConfig().hudRendering().showDamageTintOnArmor();
	}

    @ModifyExpressionValue(method = "*", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;NO_OVERLAY:I"))
    public int modifyOverlayCoords(int original) {
        boolean show = this.showDamageTintOnArmor.get();
        return show ? this.overlayCoords : original;
    }

    @Override
    public void combatify$renderWithOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T entity, float f, float g, float h, float j, float k, float l, int overlayCoords) {
        this.overlayCoords = overlayCoords;
        this.render(poseStack, multiBufferSource, i, entity, f, g, h, j, k, l);
    }
}
