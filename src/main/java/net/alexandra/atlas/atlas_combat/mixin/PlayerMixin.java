package net.alexandra.atlas.atlas_combat.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import net.alexandra.atlas.atlas_combat.AtlasCombat;
import net.alexandra.atlas.atlas_combat.enchantment.CleavingEnchantment;
import net.alexandra.atlas.atlas_combat.extensions.*;
import net.alexandra.atlas.atlas_combat.item.NewAttributes;
import net.alexandra.atlas.atlas_combat.item.WeaponType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerExtensions, LivingEntityExtensions {
	public PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	@Nullable
	public abstract ItemEntity drop(ItemStack itemStack, boolean b);

	@Shadow
	protected abstract void doAutoAttackOnTouch(@NotNull LivingEntity target);

	@Shadow
	public abstract Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pos);

	@Shadow
	private ItemStack lastItemInMainHand;
	@Shadow
	@Final
	private static Logger LOGGER;
	@Unique
	protected int attackStrengthStartValue;

	@Unique
	public boolean missedAttackRecovery;
	@Unique
	@Final
	public float baseValue = 1.0F;
	@Unique
	public Multimap additionalModifiers;

	@Unique
	public final Player player = ((Player) (Object)this);
	@Unique
	private static final UUID ADDITIONAL_ATTACK_REACH_UUID = UUID.fromString("c85331f2-9983-470d-8453-cd888c434dea");
	@Unique
	private static final UUID ADDITIONAL_BLOCK_REACH_UUID = UUID.fromString("e427a2fe-8145-483d-843b-d42a69df7742");

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
		player.getAttribute(NewAttributes.BLOCK_REACH).setBaseValue(0);
		player.getAttribute(NewAttributes.ATTACK_REACH).setBaseValue(0);
		player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
	}

	/**
	 * @author zOnlyKroks
	 */
	@Overwrite()
	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1F)
				.add(NewAttributes.ATTACK_SPEED)
				.add(Attributes.LUCK)
				.add(NewAttributes.BLOCK_REACH)
				.add(NewAttributes.ATTACK_REACH);
	}
	@Redirect(method = "tick", at = @At(value = "FIELD",target = "Lnet/minecraft/world/entity/player/Player;attackStrengthTicker:I",opcode = Opcodes.PUTFIELD))
	public void redirectAttackStrengthTicker(Player instance, int value) {
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		--instance.attackStrengthTicker;
		if(getAttackStrengthScale(baseValue) > 1.0F) {
			--instance.attackStrengthTicker;
			--instance.attackStrengthTicker;
		}
	}

	@Inject(method = "die", at = @At(value = "HEAD"))
	public void dieInject(CallbackInfo ci) {
		UUID dead = player.getUUID();
		if(dead == UUID.fromString("b30c7223-3b1d-4099-ba1c-f4a45ba6e303")){
			ItemStack specialHoe = new ItemStack(Items.IRON_HOE);
			specialHoe.enchant(Enchantments.UNBREAKING, 5);
			specialHoe.setHoverName(Component.literal("Alexandra's Hoe"));
			drop(specialHoe, false);
		}else if(dead == UUID.fromString("1623d4b1-b21c-41d3-93c2-eee2845b8497")){
			ItemStack specialBread = new ItemStack(Items.BREAD, 5);
			specialBread.setHoverName(Component.literal("Finn's Bread"));
			drop(specialBread, false);
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameIgnoreDurability(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
	public boolean redirectDurability(ItemStack left, ItemStack right) {
		return true;
	}

	/**
	 * @author zOnlyKroks
	 */
	@Overwrite()
	public void blockUsingShield(@NotNull LivingEntity attacker) {
		super.blockUsingShield(attacker);
	}

	@Override
	public boolean customShieldInteractions(float damage) {
		player.getCooldowns().addCooldown(Items.SHIELD, (int)(damage * 20.0F));
		player.stopUsingItem();
		player.level.broadcastEntityEvent(player, (byte)30);
		return true;
	}

	@Override
	public boolean hasEnabledShieldOnCrouch() {
		return true;
	}

	/**
	 * @author zOnlyKroks
	 * @reason change attacks
	 */
	@Inject(method = "attack", at = @At(value = "HEAD"), cancellable = true)
	public void attack(Entity target, CallbackInfo ci) {
		if (target.isAttackable()) {
			if (!target.skipAttackInteraction(player)) {
				if(isAttackAvailable(baseValue)) {
					float attackDamage = (float) ((ItemExtensions) player.getItemInHand(InteractionHand.MAIN_HAND).getItem()).getAttackDamage(player);
					float attackDamageBonus;
					if (target instanceof LivingEntity livingEntity) {
						attackDamageBonus = EnchantmentHelper.getDamageBonus(player.getMainHandItem(), livingEntity.getMobType());
					} else {
						attackDamageBonus = EnchantmentHelper.getDamageBonus(player.getMainHandItem(), MobType.UNDEFINED);
					}
					float currentAttackReach = (float) this.getAttackRange(player, 2.5);

					float attackStrengthScale = this.getAttackStrengthScale(baseValue);
					attackDamage *= 1;
					attackDamageBonus *= 1;
					if (attackDamage > 0.0F || attackDamageBonus > 0.0F) {
						attackDamage += attackDamageBonus;
						boolean bl = true;
						boolean bl2 = false;
						int knockbackBonus = 0;
						knockbackBonus += EnchantmentHelper.getKnockbackBonus(player);
						if (player.isSprinting()) {
							player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, player.getSoundSource(), 1.0F, 1.0F);
							++knockbackBonus;
							bl2 = true;
						}

						boolean isCrit = bl
								&& player.fallDistance > 0.0F
								&& !player.isOnGround()
								&& !player.onClimbable()
								&& !player.isInWater()
								&& !player.hasEffect(MobEffects.BLINDNESS)
								&& !player.isPassenger()
								&& target instanceof LivingEntity;
						if (isCrit) {
							attackDamage *= 1.5F;
						}

						boolean bl4 = false;
						double d = (player.walkDist - player.walkDistO);
						if (bl && !isCrit && !bl2 && player.isOnGround() && d < player.getSpeed()) {
							bl4 = checkSweepAttack();
						}

						float j = 0.0F;
						boolean bl5 = false;
						int getFireAspectLvL = EnchantmentHelper.getFireAspect(player);
						if (target instanceof LivingEntity livingEntity) {
							j = livingEntity.getHealth();
							if (getFireAspectLvL > 0 && !target.isOnFire()) {
								bl5 = true;
								target.setSecondsOnFire(1);
							}
						}

						Vec3 vec3 = target.getDeltaMovement();
						boolean bl6 = target.hurt(DamageSource.playerAttack(player), attackDamage);
						if (bl6) {
							if (knockbackBonus > 0) {
								if (target instanceof LivingEntity livingEntity) {
									((LivingEntityExtensions)livingEntity)
											.newKnockback((knockbackBonus * 0.5F),
													Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)),
													-Mth.cos(player.getYRot() * (float) (Math.PI / 180.0))
											);
								} else {
									target.push(
											(-Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)) * knockbackBonus * 0.5F),
											0.1,
											(Mth.cos(player.getYRot() * (float) (Math.PI / 180.0)) * knockbackBonus * 0.5F)
									);
								}

								player.setDeltaMovement(player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
								player.setSprinting(false);
							}

							if (bl4) {
								float l = 1.0F + EnchantmentHelper.getSweepingDamageRatio(player) * attackDamage;
								AABB box = target.getBoundingBox().inflate(1.0, 0.25, 1.0);

								for (LivingEntity livingEntity : player.level.getEntitiesOfClass(LivingEntity.class, box)) {
									if (livingEntity != player
											&& livingEntity != target
											&& !player.isAlliedTo(livingEntity)
											&& (!(livingEntity instanceof ArmorStand armorStand) || !armorStand.isMarker())
											&& player.distanceToSqr(livingEntity) < getSquaredAttackRange(player, 6.25)) {
										((LivingEntityExtensions)livingEntity).newKnockback(
												0.5F, Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)), (-Mth.cos(player.getYRot() * (float) (Math.PI / 180.0)))
										);
										livingEntity.hurt(DamageSource.playerAttack(player), l);
									}
								}

								player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
								this.betterSweepAttack(box, currentAttackReach, attackDamage, target);
							}

							if (target instanceof ServerPlayer serverPlayer && target.hurtMarked) {
								serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
								target.hurtMarked = false;
								target.setDeltaMovement(vec3);
							}

							if (isCrit) {
								player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, player.getSoundSource(), 1.0F, 1.0F);
								player.crit(target);
							}

							if (!isCrit && !bl4) {
								if (bl) {
									player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, player.getSoundSource(), 1.0F, 1.0F);
								} else {
									player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, player.getSoundSource(), 1.0F, 1.0F);
								}
							}

							if (attackDamageBonus > 0.0F) {
								player.magicCrit(target);
							}

							player.setLastHurtMob(target);
							if (target instanceof LivingEntity livingEntity) {
								EnchantmentHelper.doPostHurtEffects(livingEntity, player);
							}

							EnchantmentHelper.doPostDamageEffects(player, target);
							ItemStack itemStack2 = player.getMainHandItem();
							Entity entity = target;
							if (target instanceof EnderDragonPart enderDragonPart) {
								entity = enderDragonPart.parentMob;
							}

							if (!player.level.isClientSide && !itemStack2.isEmpty() && entity instanceof LivingEntity livingEntity) {
								itemStack2.hurtEnemy(livingEntity, player);
								if (itemStack2.isEmpty()) {
									player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
								}
							}

							if (target instanceof LivingEntity livingEntity) {
								float m = j - livingEntity.getHealth();
								player.awardStat(Stats.DAMAGE_DEALT, Math.round(m * 10.0F));
								if (getFireAspectLvL > 0) {
									target.setSecondsOnFire(getFireAspectLvL * 4);
								}

								if (player.level instanceof ServerLevel serverLevel && m > 2.0F) {
									int n = (int) (m * 0.5);
									serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), n, 0.1, 0.0, 0.1, 0.2);
								}
							}

							player.causeFoodExhaustion(0.1F);
						} else {
							player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, player.getSoundSource(), 1.0F, 1.0F);
							if (bl5) {
								target.clearFire();
							}
						}
					}

					this.resetAttackStrengthTicker(true);
				}

			}
		}
		ci.cancel();
	}
	@Override
	public void attackAir() {
		LOGGER.info("attacked air");
		if (this.isAttackAvailable(baseValue)) {
			player.swing(InteractionHand.MAIN_HAND);
			float var1 = (float)((ItemExtensions)player.getItemInHand(InteractionHand.MAIN_HAND).getItem()).getAttackDamage(player);
			if (var1 > 0.0F && this.checkSweepAttack()) {
				float var2 = (float) this.getAttackRange(player, 2.5);
				double var5 = (-Mth.sin(player.yBodyRot * 0.017453292F)) * 2.0;
				double var7 = Mth.cos(player.yBodyRot * 0.017453292F) * 2.0;
				AABB var9 = player.getBoundingBox().inflate(1.0, 0.25, 1.0).move(var5, 0.0, var7);
				betterSweepAttack(var9, var2, var1, null);
			}

			this.resetAttackStrengthTicker(false);
		}
	}
	@Unique
	public void resetAttackStrengthTicker(boolean var1) {
		this.missedAttackRecovery = !var1;
		int var2 = (int) this.getCurrentItemAttackStrengthDelay() * 2;
		if (var2 > player.attackStrengthTicker) {
			this.attackStrengthStartValue = var2;
			player.attackStrengthTicker = this.attackStrengthStartValue;
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public float getCurrentItemAttackStrengthDelay() {
		float var1 = (float)this.getAttribute(Attributes.ATTACK_SPEED).getValue() - 1.5F;
		var1 = Mth.clamp(var1, 0.1F, 1024.0F);
		return (1.0F / var1 * 20.0F + 0.5F);
	}
	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public float getAttackStrengthScale(float baseTime) {
		return this.attackStrengthStartValue == 0 ? 2.0F : Mth.clamp((1.0F - ((float)this.attackStrengthTicker - baseTime) / (float)this.attackStrengthStartValue)/6, 0.0F, 2.0F);
	}

	public float getCurrentAttackReach(float baseValue) {
		return (float)((ItemExtensions) player.getItemInHand(InteractionHand.MAIN_HAND).getItem()).getAttackReach(player);
	}

	@Override
	public boolean isAttackAvailable(float baseTime) {
		if (!(getAttackStrengthScale(baseTime) < 1.0F)) {
			return true;
		} else {
			return this.missedAttackRecovery && (float)this.attackStrengthStartValue - ((float)this.attackStrengthTicker - baseTime) > 40.0F;
		}
	}
	@Override
	public boolean isAttackAvailable(float baseTime, float minValue) {
		if (!(getAttackStrengthScale(baseTime) < minValue)) {
			return true;
		} else {
			return this.missedAttackRecovery && (float)this.attackStrengthStartValue - ((float)this.attackStrengthTicker - baseTime) > 40.0F;
		}
	}
	@Override
	public boolean isAttackAvailable(float baseTime, float minValue, boolean isAutoAttack) {
		if (!(getAttackStrengthScale(baseTime) < minValue) && !isAutoAttack) {
			return true;
		} else if (!(getAttackStrengthScale(baseTime) < minValue) && isAutoAttack) {
			return getAttackStrengthScale(baseTime) > minValue + 12.0F;
		} else if (isAutoAttack){
			return this.missedAttackRecovery && (float)this.attackStrengthStartValue - ((float)this.attackStrengthTicker - baseTime) > 50.0F;
		} else {
			return this.missedAttackRecovery && (float)this.attackStrengthStartValue - ((float)this.attackStrengthTicker - baseTime) > 40.0F;
		}
	}

	protected boolean checkSweepAttack() {
		return getAttackStrengthScale(baseValue) > 1.95F && EnchantmentHelper.getSweepingDamageRatio(player) > 0.0F;
	}

	public void betterSweepAttack(AABB var1, float var2, float var3, Entity var4) {
		float sweepingDamageRatio = 1.0F + EnchantmentHelper.getSweepingDamageRatio(player) * var3;
		List<LivingEntity> livingEntities = player.level.getEntitiesOfClass(LivingEntity.class, var1);
		Iterator<LivingEntity> livingEntityIterator = livingEntities.iterator();

		while (true) {
			LivingEntity var8;
			do {
				do {
					do {
						do {
							if (!livingEntityIterator.hasNext()) {
								player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
								if (player.level instanceof ServerLevel serverLevel) {
									double var11 = -Mth.sin(player.getYRot() * 0.017453292F);
									double var12 = Mth.cos(player.getYRot() * 0.017453292F);
									serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + var11, player.getY() + player.getBbHeight() * 0.5, player.getZ() + var12, 0, var11, 0.0, var12, 0.0);
								}

								return;
							}

							var8 = livingEntityIterator.next();
						} while (var8 == player);
					} while (var8 == var4);
				} while (player.isAlliedTo(var8));
			} while (var8 instanceof ArmorStand armorStand && armorStand.isMarker());

			float var9 = var2 + var8.getBbWidth() * 0.5F;
			if (player.distanceToSqr(var8) < (var9 * var9)) {
				((LivingEntityExtensions)var8).newKnockback(0.5F, Mth.sin(player.getYRot() * 0.017453292F), (-Mth.cos(player.getYRot() * 0.017453292F)));
				var8.hurt(DamageSource.playerAttack(player), sweepingDamageRatio);
			}
		}
	}

	@Override
	public boolean isItemOnCooldown(ItemStack var1) {
		return player.getCooldowns().isOnCooldown(var1.getItem());
	}
	@Override
	public Multimap getAdditionalModifiers() {
		return additionalModifiers;
	}

	@Override
	public double getAttackRange(LivingEntity entity, double baseAttackRange) {
		float var3 = getAttackStrengthScale(baseValue);
		if (var3 > 1.95F && !player.isCrouching()) {
			@org.jetbrains.annotations.Nullable final var attackRange = entity.getAttribute(NewAttributes.ATTACK_REACH);
			return (attackRange != null) ? (baseAttackRange + attackRange.getValue()) + 1 : baseAttackRange + 1;
		}
		@org.jetbrains.annotations.Nullable final var attackRange = entity.getAttribute(NewAttributes.ATTACK_REACH);
		return (attackRange != null) ? (baseAttackRange + attackRange.getValue()) : baseAttackRange;
	}

	@Override
	public double getSquaredAttackRange(LivingEntity entity, double sqBaseAttackRange) {
		final var attackRange = getReach(entity, Math.sqrt(sqBaseAttackRange));
		return attackRange * attackRange;
	}

	@Override
	public double getReach(LivingEntity entity, double baseAttackRange) {
		float var3 = getAttackStrengthScale(baseValue);
		if (var3 > 1.95F && !player.isCrouching()) {
			@org.jetbrains.annotations.Nullable final var attackRange = entity.getAttribute(NewAttributes.BLOCK_REACH);
			return (attackRange != null) ? (baseAttackRange + attackRange.getValue()) + 1 : baseAttackRange + 1;
		}
		@org.jetbrains.annotations.Nullable final var attackRange = entity.getAttribute(NewAttributes.BLOCK_REACH);
		return (attackRange != null) ? (baseAttackRange + attackRange.getValue()) : baseAttackRange;
	}

	@Override
	public double getSquaredReach(LivingEntity entity, double sqBaseAttackRange) {
		final var attackRange = getAttackRange(entity, Math.sqrt(sqBaseAttackRange));
		return attackRange * attackRange;
	}

	@Override
	public boolean getMissedAttackRecovery() {
		return missedAttackRecovery;
	}

	@Override
	public int getAttackStrengthStartValue() {
		return attackStrengthStartValue;
	}
}
