package net.zemphis.bleedsmp;

import net.fabricmc.api.ModInitializer;

import net.zemphis.bleed.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BleedSMP implements ModInitializer {
	public static final String MOD_ID = "bleedsmp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();

	}
}