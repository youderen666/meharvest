package com.meharvest.client;

import appeng.util.Platform;
import com.meharvest.blockentity.HarvestFlagBlockEntity;
import com.meharvest.menu.HarvestFlagMenu;
import com.meharvest.network.C2SConfig;
import com.meharvest.network.C2SToggle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class HarvestFlagScreen extends AbstractContainerScreen<HarvestFlagMenu> {
    private static final int BG = 0xFF1B2227;
    private static final int EDGE = 0xFF3A4A52;
    private static final int TRACK = 0xFF101619;
    private static final int ACCENT = 0xFF35C8E8;
    private static final int TEXT = 0xFFE8F6FA;
    private static final int MUTED = 0xFF93A7B0;
    private static final int OK = 0xFF6FDD8B;
    private static final int WARN = 0xFFE8A33D;

    private static final int BAR_X = 10;
    private static final int BAR_Y = 136;
    private static final int BAR_H = 10;

    private Button toggleButton;

    public HarvestFlagScreen(HarvestFlagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRow(0, 28);
        addRow(1, 52);
        addRow(2, 76);

        toggleButton = addRenderableWidget(Button.builder(toggleText(), button -> toggle())
                .bounds(leftPos + 104, topPos + 96, 62, 16).build());
    }

    private void addRow(int index, int y) {
        int x = leftPos + 104;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> adjust(index, -1))
                .bounds(x, topPos + y, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjust(index, 1))
                .bounds(x + 24, topPos + y, 18, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (toggleButton != null) {
            toggleButton.setMessage(toggleText());
        }
    }

    private Component toggleText() {
        return Component.translatable(menu.isEnabled()
                ? "gui.meharvest.toggle.on"
                : "gui.meharvest.toggle.off");
    }

    private void toggle() {
        PacketDistributor.sendToServer(new C2SToggle(menu.getPos(), !menu.isEnabled()));
    }

    private void adjust(int index, int direction) {
        int radius = menu.get(0);
        int interval = menu.get(1);
        int batch = menu.get(2);

        switch (index) {
            case 0 -> radius = Mth.clamp(radius + direction,
                    HarvestFlagBlockEntity.MIN_RADIUS, HarvestFlagBlockEntity.MAX_RADIUS);
            case 1 -> interval = Mth.clamp(interval + direction * 20,
                    HarvestFlagBlockEntity.MIN_INTERVAL, HarvestFlagBlockEntity.MAX_INTERVAL);
            default -> batch = Mth.clamp(batch + direction, 1, HarvestFlagBlockEntity.MAX_BATCH);
        }

        PacketDistributor.sendToServer(new C2SConfig(menu.getPos(), radius, interval, batch));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, EDGE);
        graphics.fill(leftPos, topPos + imageHeight - 2, leftPos + imageWidth, topPos + imageHeight, EDGE);
        graphics.fill(leftPos, topPos, leftPos + 2, topPos + imageHeight, EDGE);
        graphics.fill(leftPos + imageWidth - 2, topPos, leftPos + imageWidth, topPos + imageHeight, EDGE);
        graphics.fill(leftPos + 8, topPos + 20, leftPos + imageWidth - 8, topPos + 21, EDGE);
        graphics.fill(leftPos + 8, topPos + 118, leftPos + imageWidth - 8, topPos + 119, EDGE);

        // 电量条
        int barWidth = imageWidth - 2 * BAR_X;
        graphics.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + barWidth, topPos + BAR_Y + BAR_H, TRACK);

        int stored = menu.getStoredPower();
        int max = menu.getMaxStoredPower();
        int filled = Mth.clamp((int) ((long) barWidth * stored / max), 0, barWidth);
        if (filled > 0) {
            graphics.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H,
                    menu.isEnabled() ? ACCENT : MUTED);
            graphics.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + filled, topPos + BAR_Y + 2, 0x60FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, ACCENT, false);

        row(graphics, "gui.meharvest.radius", menu.get(0), 32);
        row(graphics, "gui.meharvest.interval", menu.get(1), 56);
        row(graphics, "gui.meharvest.batch", menu.get(2), 80);

        graphics.drawString(font, Component.translatable("gui.meharvest.toggle"), 10, 100,
                menu.isEnabled() ? TEXT : MUTED, false);

        graphics.drawString(font, powerText(), BAR_X, BAR_Y - 12, TEXT, false);

        Component status = statusText();
        graphics.drawString(font, Component.translatable("gui.meharvest.status", status), 10, 152, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.meharvest.index", menu.get(4)), 10, 164, MUTED, false);
        graphics.drawString(font, Component.translatable("gui.meharvest.buffer", menu.get(5)), 10, 176, MUTED, false);
    }

    private Component powerText() {
        return Component.translatable("gui.meharvest.power",
                Platform.formatPower(menu.getStoredPower(), false),
                Platform.formatPower(menu.getMaxStoredPower(), false));
    }

    private Component statusText() {
        return switch (menu.get(3)) {
            case 0 -> Component.translatable("gui.meharvest.status.nonet").withStyle(style -> style.withColor(MUTED));
            case 1 -> Component.translatable("gui.meharvest.status.offline").withStyle(style -> style.withColor(WARN));
            case 3 -> Component.translatable("gui.meharvest.status.nopower").withStyle(style -> style.withColor(WARN));
            case 4 -> Component.translatable("gui.meharvest.status.disabled").withStyle(style -> style.withColor(MUTED));
            default -> Component.translatable("gui.meharvest.status.online").withStyle(style -> style.withColor(OK));
        };
    }

    private void row(GuiGraphics graphics, String key, int value, int y) {
        graphics.drawString(font, Component.translatable(key), 10, y, MUTED, false);
        graphics.drawString(font, Integer.toString(value), 80, y, TEXT, false);
    }
}
