package net.atlas.combatify.mixin;

import net.atlas.combatify.Combatify;
import net.atlas.combatify.extensions.*;
import net.atlas.combatify.util.CombatUtil;
import net.atlas.combatify.util.MethodHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements ServerPlayerExtensions {
	@Unique
	private boolean retainAttack;

	@Shadow
	public abstract void swing(InteractionHand interactionHand);

	@Shadow
	public ServerGamePacketListenerImpl connection;

	@Shadow
	public abstract Entity getCamera();

	@Unique
	public final ServerPlayer player = ServerPlayer.class.cast(this);

	public ServerPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "tick", at = @At(value = "HEAD"))
	public void hitreg(CallbackInfo ci) {
		CombatUtil.setPosition((ServerPlayer)(Object)this);
		if (((PlayerExtensions) this.player).isAttackAvailable(-1.0F) && retainAttack && Combatify.unmoddedPlayers.contains(getUUID())) {
			retainAttack = false;
			customSwing(InteractionHand.MAIN_HAND);
			Entity entity = getCamera();
			if (entity == null)
				entity = this.player;
			Vec3 eyePos = entity.getEyePosition(1.0f);
			Vec3 viewVector = entity.getViewVector(1.0f);
			double reach = entityInteractionRange();
			double sqrReach = reach * reach;
			Vec3 adjPos = eyePos.add(viewVector.x * reach, viewVector.y * reach, viewVector.z * reach);
			AABB rayBB = entity.getBoundingBox().expandTowards(viewVector.scale(reach)).inflate(1.0, 1.0, 1.0);
			HitResult hitResult = entity.pick(reach, 1.0f, false);
			double i = hitResult.getLocation().distanceToSqr(eyePos);
			EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(entity, eyePos, adjPos, rayBB, (entityx) -> !entityx.isSpectator() && entityx.isPickable(), sqrReach);
			if (entityHitResult != null && entityHitResult.getLocation().distanceToSqr(eyePos) < i)
				hitResult = entityHitResult;
			else
				hitResult = MethodHandler.redirectResult(player, hitResult);
			if (hitResult.getType() == HitResult.Type.ENTITY)
				connection.handleInteract(ServerboundInteractPacket.createAttackPacket(((EntityHitResult)hitResult).getEntity(), isShiftKeyDown()));
		}
	}
	@Inject(method = "swing", at = @At(value = "HEAD"), cancellable = true)
	public void removeReset(InteractionHand hand, CallbackInfo ci) {
		super.swing(hand);
		if (Combatify.unmoddedPlayers.contains(getUUID())) {
			if (Combatify.isPlayerAttacking.get(getUUID()))
				handleInteract();
			Combatify.isPlayerAttacking.put(getUUID(), true);
		}
		ci.cancel();
	}
	@Unique
	public void handleInteract() {
		if (retainAttack)
			return;
		if (!isAttackAvailable(0.0F)) {
			float var1 = this.player.getAttackStrengthScale(0.0F);
			if (var1 < 0.8F) {
				resetAttackStrengthTicker(!getMissedAttackRecovery());
				return;
			}

			if (var1 < 1.0F) {
				retainAttack = true;
				return;
			}
		}
		attackAir();
	}
	@Inject(method = "updatePlayerAttributes", at = @At("HEAD"), cancellable = true)
	public void removeCreativeReach(CallbackInfo ci) {
		@Nullable final var attackRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
		double chargedBonus;
		float strengthScale = player.getAttackStrengthScale(1.0F);
		float charge = Combatify.CONFIG.chargedAttacks() ? 1.95F : 0.95F;
		if (attackRange != null) {
			Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
			chargedBonus = ((ItemExtensions) item).getChargedAttackBonus();
			AttributeModifier modifier = new AttributeModifier(UUID.fromString("98491ef6-97b1-4584-ae82-71a8cc85cf74"), "Charged reach bonus", chargedBonus, AttributeModifier.Operation.ADDITION);
			if (strengthScale > charge && !player.isCrouching() && Combatify.CONFIG.chargedReach())
				attackRange.addOrUpdateTransientModifier(modifier);
			else
				attackRange.removeModifier(modifier);
		}
		if (!Combatify.CONFIG.creativeReach())
			ci.cancel();
	}

	public boolean isRetainingAttack() {
		return retainAttack;
	}

	public void setRetainAttack(boolean retain) {
		retainAttack = retain;
	}
}
