package net.atlas.combatify.enchantment;

import net.atlas.combatify.compat.PolymerCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;

public class CleavingEnchantment extends Enchantment {
	public int level;

	public CleavingEnchantment() {
		super(Enchantment.definition(ItemTags.AXES, 2, 3, Enchantment.dynamicCost(5, 20), Enchantment.dynamicCost(25, 20), 3, EquipmentSlot.MAINHAND));
	}

	@Override
	public float getDamageBonus(int level, EntityType<?> group) {
		this.level = level;
		return (float) 1 + level;
	}

	@Override
	protected boolean checkCompatibility(Enchantment enchantment) {
		return super.checkCompatibility(enchantment) && !(enchantment instanceof PiercingEnchantment || enchantment instanceof DamageEnchantment);
	}
	public static CleavingEnchantment create() {
		if (FabricLoader.getInstance().isModLoaded("polymer-core"))
			return PolymerCompat.getPolyCleaving();
		return new CleavingEnchantment();
	}
}
