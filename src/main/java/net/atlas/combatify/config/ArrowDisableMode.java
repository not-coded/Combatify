package net.atlas.combatify.config;

import net.minecraft.world.entity.projectile.AbstractArrow;

public enum ArrowDisableMode {
	NONE(false, false, false),
	ALL(true, true, true),
	CRIT(false, true, false),
	PIERCE(false, false, true),
	CRIT_OR_PIERCE(false, true, true),
	CRIT_AND_PIERCE(false, false, false, true);
	public final boolean onAny;
	public final boolean onCrit;
	public final boolean onPierce;
	public final boolean onPierceCrit;
	ArrowDisableMode(boolean onAny, boolean onCrit, boolean onPierce) {
		this(onAny, onCrit, onPierce, false);
	}
	ArrowDisableMode(boolean onAny, boolean onCrit, boolean onPierce, boolean onPierceCrit) {
		this.onAny = onAny;
		this.onCrit = onCrit;
		this.onPierce = onPierce;
		this.onPierceCrit = onPierceCrit;
	}
	public boolean satisfiesConditions(AbstractArrow arrow) {
		if (onAny)
			return true;
		boolean bl = false;
		if (onCrit)
			bl = arrow.isCritArrow();
		if (onPierce)
			bl |= arrow.getPierceLevel() > 0;
		if (onPierceCrit)
			bl = arrow.isCritArrow() && arrow.getPierceLevel() > 0;
		return bl;
	}
}