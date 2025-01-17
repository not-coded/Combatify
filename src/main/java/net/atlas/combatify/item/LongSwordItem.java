package net.atlas.combatify.item;

import net.atlas.combatify.Combatify;
import net.atlas.combatify.extensions.ExtendedTier;
import net.atlas.combatify.extensions.WeaponWithType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LongSwordItem extends TieredItem implements WeaponWithType {
	public LongSwordItem(Tier tier, Properties properties) {
		super(tier, properties.component(DataComponents.TOOL, createToolProperties()).attributes(baseAttributeModifiers(tier)));
	}

	public static ItemAttributeModifiers baseAttributeModifiers(Tier tier) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		WeaponType.LONGSWORD.addCombatAttributes(tier, builder);
		return builder.build();
	}

	@Override
	public ItemAttributeModifiers modifyAttributeModifiers(ItemAttributeModifiers original) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		getWeaponType().addCombatAttributes(getConfigTier(), builder);
		return builder.build();
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
		return !miner.isCreative();
	}

	private static Tool createToolProperties() {
		return new Tool(List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F), Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)), 1.0F, 2);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
		return true;
	}

	@Override
	public double getPiercingLevel() {
		if(Combatify.ITEMS != null && Combatify.ITEMS.configuredItems.containsKey(this)) {
			Double piercingLevel = Combatify.ITEMS.configuredItems.get(this).piercingLevel;
			if (piercingLevel != null)
				return piercingLevel;
		}
		if (Combatify.ITEMS != null && Combatify.ITEMS.configuredWeapons.containsKey(getWeaponType())) {
			Double piercingLevel = Combatify.ITEMS.configuredWeapons.get(getWeaponType()).piercingLevel;
			if (piercingLevel != null)
				return piercingLevel;
		}
		Tier tier = getConfigTier();
		return tier == Tiers.NETHERITE || ExtendedTier.getLevel(tier) >= 4 ? 0.2
			: tier == Tiers.GOLD || tier == Tiers.WOOD || tier == Tiers.STONE || ExtendedTier.getLevel(tier) <= 1 ? 0.0
			: (0.1 * (ExtendedTier.getLevel(tier) - 1));
	}

	@Override
	public Item self() {
		return this;
	}

	@Override
	public WeaponType getWeaponType() {
		if(Combatify.ITEMS != null && Combatify.ITEMS.configuredItems.containsKey(this)) {
			WeaponType type = Combatify.ITEMS.configuredItems.get(this).type;
			if (type != null)
				return type;
		}
		return WeaponType.LONGSWORD;
	}
}
