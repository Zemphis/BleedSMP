package net.zemphis.bleed.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.zemphis.bleedsmp.BleedSMP;

public class ContractScreen extends HandledScreen<ContractScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(BleedSMP.MOD_ID, "textures/gui/contract_table_gui.png");

    public ContractScreen(ContractScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x, y,
                0.0F, 0.0F,
                backgroundWidth, backgroundHeight,
                256, 256
        );
    }
}