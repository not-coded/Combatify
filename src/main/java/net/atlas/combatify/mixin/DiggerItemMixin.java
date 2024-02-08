package net.atlas.combatify.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.atlas.combatify.Combatify;
import net.atlas.combatify.config.ConfigurableItemData;
import net.atlas.combatify.config.ConfigurableWeaponData;
import net.atlas.combatify.extensions.DefaultedItemExtensions;
import net.atlas.combatify.extensions.ItemExtensions;
import net.atlas.combatify.extensions.WeaponWithType;
import net.atlas.combatify.item.WeaponType;
import net.atlas.combatify.util.BlockingType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(DiggerItem.class)
public abstract class DiggerItemMixin extends TieredItem implements ItemExtensions, DefaultedItemExtensions, WeaponWithType {
	@Shadow
	private Multimap<Holder<Attribute>, AttributeModifier> defaultModifiers;
	public DiggerItemMixin(Tier tier, Properties properties) {
		super(tier, properties);
	}
	@Override
	public void modifyAttributeModifiers() {
		if (getWeaponType().isEmpty())
			return;
		ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> var3 = ImmutableMultimap.builder();
		getWeaponType().addCombatAttributes(getTier(), var3);
		ImmutableMultimap<Holder<Attribute>, AttributeModifier> output = var3.build();
		((DefaultedItemExtensions)this).setDefaultModifiers(output);
	}
	@Redirect(method = "hurtEnemy",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
	public <T extends LivingEntity> void damage(ItemStack instance, int amount, LivingEntity livingEntity, EquipmentSlot equipmentSlot) {
		boolean bl = instance.getItem() instanceof AxeItem || instance.getItem() instanceof HoeItem;
		if (bl) {
			amount -= 1;
		}
		instance.hurtAndBreak(amount, livingEntity, equipmentSlot);
	}

	@Override
	public void setDefaultModifiers(ImmutableMultimap<Holder<Attribute>, AttributeModifier> modifiers) {
		defaultModifiers = modifiers;
	}

	@Override
	public void setStackSize(int stackSize) {
		this.maxStackSize = stackSize;
	}

	@Override
	public WeaponType getWeaponType() {
		if(Combatify.CONFIG != null && Combatify.CONFIG.configuredItems.containsKey(this)) {
			WeaponType type = Combatify.CONFIG.configuredItems.get(this).type;
			if (type != null)
				return type;
		}
		return WeaponType.EMPTY;
	}
	@Override
	public double getChargedAttackBonus() {
		Item item = this;
		double chargedBonus = getWeaponType().getChargedReach();
		if(Combatify.CONFIG.configuredItems.containsKey(item)) {
			ConfigurableItemData configurableItemData = Combatify.CONFIG.configuredItems.get(item);
			if (configurableItemData.chargedReach != null)
				chargedBonus = configurableItemData.chargedReach;
		}
		return chargedBonus;
	}

	@Override
	public BlockingType getBlockingType() {
		if (Combatify.CONFIG != null && Combatify.CONFIG.configuredItems.containsKey(this)) {
			ConfigurableItemData configurableItemData = Combatify.CONFIG.configuredItems.get(this);
			if (configurableItemData.blockingType != null) {
				return configurableItemData.blockingType;
			}
		}
		if (Combatify.CONFIG != null && Combatify.CONFIG.configuredWeapons.containsKey(getWeaponType())) {
			ConfigurableWeaponData configurableWeaponData = Combatify.CONFIG.configuredWeapons.get(getWeaponType());
			if (configurableWeaponData.blockingType != null) {
				return configurableWeaponData.blockingType;
			}
		}
		return Combatify.EMPTY;
	}

	@Override
	public double getPiercingLevel() {
		if(Combatify.CONFIG != null && Combatify.CONFIG.configuredItems.containsKey(this)) {
			ConfigurableItemData configurableItemData = Combatify.CONFIG.configuredItems.get(this);
			if (configurableItemData.piercingLevel != null) {
				return configurableItemData.piercingLevel;
			}
		}
		if (Combatify.CONFIG != null && Combatify.CONFIG.configuredWeapons.containsKey(getWeaponType())) {
			ConfigurableWeaponData configurableWeaponData = Combatify.CONFIG.configuredWeapons.get(getWeaponType());
			if (configurableWeaponData.piercingLevel != null) {
				return configurableWeaponData.piercingLevel;
			}
		}
		return 0;
	}
}
