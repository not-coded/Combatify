package net.atlas.combatify.util;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public interface CustomEnchantmentHelper {
	static float getDamageBonus(ItemStack level, LivingEntity entity) {
		if(entity.getType().is(EntityTypeTags.AQUATIC) || entity.isInWaterOrRain()) {
			return EnchantmentHelper.getDamageBonus(level, EntityType.DROWNED);
		}
		return EnchantmentHelper.getDamageBonus(level, entity.getType());
	}
}
