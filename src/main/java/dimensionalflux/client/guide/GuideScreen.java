package dimensionalflux.client.guide;

import dimensionalflux.client.gui.SteelframePaint;
import com.mojang.blaze3d.platform.InputConstants;
import dimensionalflux.client.gui.DfStyle;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * The Dimensional Flux Guide as an open book in the colors of its model: dark steel covers with copper corners, cream
 * pages, a cyan ribbon back to the contents. The first spread holds the presentation and the clickable contents; then
 * the chapters run page after page. Turning a page (arrows, keys, mouse wheel or a contents line) animates the sheet
 * around the spine.
 */
public class GuideScreen extends Screen {
    /** The book grows with the window between these sizes, keeping its proportions. */
    private static final int MIN_BOOK_W = 312;
    private static final int MAX_BOOK_W = 470;
    private static final float BOOK_RATIO = 1.5F;
    private static final int COVER = 6;
    private static final int SPINE = 4;
    static final int PAD_X = 12;
    static final int PAD_TOP = 10;
    private static final int FOOTER = 18;

    // colors of the book model's texture
    private static final int PAPER = 0xFFE0DBC7;
    private static final int PAPER_LIGHT = 0xFFEFEADA;
    private static final int PAPER_EDGE = 0xFFC9C4B2;
    private static final int PAPER_EDGE_DARK = 0xFFA09D91;
    private static final int STEEL = 0xFF20212C;
    private static final int STEEL_LIGHT = 0xFF333444;
    private static final int STEEL_DARK = 0xFF171820;
    private static final int COPPER = 0xFFD58C50;
    private static final int COPPER_DARK = 0xFF9C5834;
    private static final int RIVET = 0xFF414255;
    private static final int CYAN = 0xFF11C4CF;
    private static final int CYAN_LIGHT = 0xFF7FF4EB;

    private static final long TURN_MS = 380;
    private static final int ARROW_W = 23;
    private static final int ARROW_H = 13;

    /** Spread shown when the book was last closed, kept while the game runs. */
    private static int sLastSpread;

    private int bookW;
    private int bookH;
    private int pageW;
    private int pageH;
    private int contentW;
    private int contentH;
    private int bookX;
    private int bookY;

    private GuideBook book;
    private int spread;

    /** Page turn in progress: from one spread to another, -1 when idle. */
    private int turnFrom = -1;
    private int turnTo;
    private long turnStart;

    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Component> hoveredText = List.of();
    private Identifier hoveredReference;

    public GuideScreen() {
        super(Component.translatable("item.dimensionalflux.guide_book"));
    }

    Font font() {
        return font;
    }

    int pageWidth() {
        return pageW;
    }

    int pageHeight() {
        return pageH;
    }

    /** True while a sheet is turning: the pages are drawn without interaction. */
    boolean turning() {
        return turnFrom >= 0;
    }

    /** The book opening: the cover laid open on the table, then the first page settling. */
    @Override
    public void added() {
        super.added();
        // called before init(): the screen's own minecraft field is not set yet
        var sounds = net.minecraft.client.Minecraft.getInstance().getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PUT, 1.1F, 0.9F));
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F, 0.7F));
    }

    @Override
    protected void init() {
        bookW = Mth.clamp(Math.min(width - 24, Math.round((height - 24) * BOOK_RATIO)), MIN_BOOK_W, MAX_BOOK_W);
        bookH = Math.round(bookW / BOOK_RATIO);
        pageW = (bookW - 2 * COVER - SPINE) / 2;
        pageH = bookH - 2 * COVER;
        contentW = pageW - 2 * PAD_X;
        contentH = pageH - PAD_TOP - FOOTER;
        bookX = (width - bookW) / 2;
        bookY = (height - bookH) / 2;
        book = GuideBook.build(this, GuideContent.chapters(), contentW, contentH);
        spread = Mth.clamp(sLastSpread, 0, book.spreads() - 1);
        turnFrom = -1;
    }

    private int leftPageX() {
        return bookX + COVER;
    }

    private int rightPageX() {
        return bookX + COVER + pageW + SPINE;
    }

    private int spineX() {
        return bookX + bookW / 2;
    }

    private int pageY() {
        return bookY + COVER;
    }

    // ---- navigation

    void turnTo(int target) {
        target = Mth.clamp(target, 0, book.spreads() - 1);
        if (target == spread || turning()) {
            return;
        }
        turnFrom = spread;
        turnTo = target;
        turnStart = Util.getMillis();
        spread = target;
        sLastSpread = target;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        // the book in the player's hand turns a leaf as well
        GuideBookAnimation.pageTurned(turnTo > turnFrom);
    }

    // ---- rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hoveredStack = ItemStack.EMPTY;
        hoveredText = List.of();
        hoveredReference = null;
        float progress = 1.0F;
        if (turning()) {
            progress = Math.min(1.0F, (Util.getMillis() - turnStart) / (float) TURN_MS);
        }
        renderCover(g);
        if (!turning()) {
            renderPage(g, 2 * spread, leftPageX(), true, mouseX, mouseY);
            renderPage(g, 2 * spread + 1, rightPageX(), false, mouseX, mouseY);
        } else {
            renderTurn(g, progress);
            if (progress >= 1.0F) {
                turnFrom = -1;
            }
        }
        renderRibbon(g, mouseX, mouseY);
        renderArrows(g, mouseX, mouseY);
        GuideReferences.Target reference = hoveredReference == null ? null : GuideReferences.target(hoveredReference);
        if (reference != null) {
            renderReference(g, reference, mouseX, mouseY);
        } else if (!hoveredStack.isEmpty()) {
            g.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY);
        } else if (!hoveredText.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, hoveredText, mouseX, mouseY);
        }
    }

    /** Steel covers with beveled edges, copper corners with rivets, the spine between them, and the page block. */
    private void renderCover(GuiGraphicsExtractor g) {
        int x0 = bookX;
        int y0 = bookY;
        int x1 = bookX + bookW;
        int y1 = bookY + bookH;
        SteelframePaint.fill(g, x0 + 3, y1, x1 + 3, y1 + 3, 0x55000000);
        SteelframePaint.fill(g, x0, y0, x1, y1, STEEL_DARK);
        SteelframePaint.fill(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, STEEL);
        SteelframePaint.fill(g, x0 + 1, y0 + 1, x1 - 1, y0 + 2, STEEL_LIGHT);
        SteelframePaint.fill(g, x0 + 1, y0 + 1, x0 + 2, y1 - 1, STEEL_LIGHT);
        for (int[] c : new int[][] {{x0, y0}, {x1 - 7, y0}, {x0, y1 - 7}, {x1 - 7, y1 - 7}}) {
            SteelframePaint.fill(g, c[0], c[1], c[0] + 7, c[1] + 7, COPPER_DARK);
            SteelframePaint.fill(g, c[0] + 1, c[1] + 1, c[0] + 6, c[1] + 6, COPPER);
            SteelframePaint.fill(g, c[0] + 3, c[1] + 3, c[0] + 4, c[1] + 4, RIVET);
        }
        int sx = spineX();
        SteelframePaint.fill(g, sx - 3, y0, sx + 3, y1, STEEL_DARK);
        SteelframePaint.fill(g, sx - 1, y0, sx + 1, y1, STEEL_LIGHT);
        for (int band : new int[] {y0 + 22, y0 + bookH / 2 - 2, y1 - 26}) {
            SteelframePaint.fill(g, sx - 3, band, sx + 3, band + 4, COPPER_DARK);
            SteelframePaint.fill(g, sx - 2, band + 1, sx + 2, band + 3, CYAN);
        }
        // page block: the stacked edges under the open pages
        int py = pageY();
        SteelframePaint.fill(g, leftPageX() - 2, py + 2, leftPageX() + pageW, py + pageH + 2, PAPER_EDGE_DARK);
        SteelframePaint.fill(g, leftPageX() - 1, py + 1, leftPageX() + pageW, py + pageH + 1, PAPER_EDGE);
        SteelframePaint.fill(g, rightPageX(), py + 2, rightPageX() + pageW + 2, py + pageH + 2, PAPER_EDGE_DARK);
        SteelframePaint.fill(g, rightPageX(), py + 1, rightPageX() + pageW + 1, py + pageH + 1, PAPER_EDGE);
    }

    /** A page: paper with the shade of the spine, its elements and its number. */
    private void renderPage(GuiGraphicsExtractor g, int index, int x, boolean left, int mouseX, int mouseY) {
        int y = pageY();
        SteelframePaint.fill(g, x, y, x + pageW, y + pageH, PAPER);
        SteelframePaint.fill(g, left ? x : x + pageW - 1, y, left ? x + 1 : x + pageW, y + pageH, PAPER_LIGHT);
        for (int i = 0; i < 6; i++) {
            int alpha = (int) (0x30 * (1.0 - i / 6.0));
            int sx = left ? x + pageW - 1 - i : x + i;
            SteelframePaint.fill(g, sx, y, sx + 1, y + pageH, alpha << 24 | 0x171820);
        }
        if (index < 0 || index >= book.pages.size()) {
            return;
        }
        GuideBook.Page page = book.pages.get(index);
        int cx = x + PAD_X;
        int cy = y + PAD_TOP;
        boolean over = !turning() && mouseX >= cx && mouseX < cx + contentW && mouseY >= cy && mouseY < cy + contentH;
        int mx = over ? mouseX : -1;
        int my = over ? mouseY : -1;
        for (GuideBook.Placed p : page.elements()) {
            p.element().render(this, g, cx, cy + p.y(), contentW, mx, my);
        }
        String number = Integer.toString(index + 1);
        int footerY = y + pageH - 11;
        SteelframePaint.text(g, font, number, left ? x + PAD_X : x + pageW - PAD_X - font.width(number), footerY, GuideElements.MUTED, false);
        if (page.chapter() >= 0) {
            Component name = Component.translatable("guide.dimensionalflux.chapter." + book.chapters.get(page.chapter()).id());
            int nameW = contentW - 2 * (font.width(number) + 6);
            fittedText(g, name, x + PAD_X + font.width(number) + 6, footerY, nameW, GuideElements.MUTED, true);
        }
    }

    /**
     * The turning sheet: forward, the right page lifts from the spine and lands on the left showing its back (the
     * target's left page); backward, the opposite. Underneath, the target pages are already in place.
     */
    private void renderTurn(GuiGraphicsExtractor g, float progress) {
        boolean forward = turnTo > turnFrom;
        float eased = (float) (0.5 - 0.5 * Math.cos(Math.PI * progress));
        float angle = eased * 180.0F;
        float cos = Mth.cos(angle * Mth.DEG_TO_RAD);
        int from = turnFrom;
        int to = turnTo;
        if (forward) {
            renderPage(g, 2 * from, leftPageX(), true, -1, -1);
            renderPage(g, 2 * to + 1, rightPageX(), false, -1, -1);
        } else {
            renderPage(g, 2 * to, leftPageX(), true, -1, -1);
            renderPage(g, 2 * from + 1, rightPageX(), false, -1, -1);
        }
        boolean firstHalf = angle < 90.0F;
        // forward: front = current right page (right side), back = target left page (left side); backward mirrors it
        int pageIndex;
        boolean onLeft;
        if (forward) {
            pageIndex = firstHalf ? 2 * from + 1 : 2 * to;
            onLeft = !firstHalf;
        } else {
            pageIndex = firstHalf ? 2 * from : 2 * to + 1;
            onLeft = firstHalf;
        }
        float squash = Math.max(0.02F, Math.abs(cos));
        float lift = 1.0F + 0.04F * Mth.sin(angle * Mth.DEG_TO_RAD);
        int sx = spineX();
        g.pose().pushMatrix();
        g.pose().translate(sx, pageY() + pageH / 2.0F);
        g.pose().scale(squash, lift);
        g.pose().translate(-sx, -(pageY() + pageH / 2.0F));
        renderPage(g, pageIndex, onLeft ? leftPageX() : rightPageX(), onLeft, -1, -1);
        // the sheet darkens as it stands up
        int shade = (int) (0x70 * (1.0F - squash));
        int px = onLeft ? leftPageX() : rightPageX();
        SteelframePaint.fill(g, px, pageY(), px + pageW, pageY() + pageH, shade << 24 | 0x171820);
        g.pose().popMatrix();
    }

    /** The cyan ribbon hanging over the right page, edged in copper like the book's own: back to the contents. */
    private void renderRibbon(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = ribbonX();
        int y = bookY;
        boolean hover = overRibbon(mouseX, mouseY);
        int length = hover ? 22 : 18;
        SteelframePaint.fill(g, x, y, x + 6, y + length, COPPER_DARK);
        SteelframePaint.fill(g, x + 1, y, x + 5, y + length, CYAN);
        SteelframePaint.fill(g, x + 2, y, x + 3, y + length, CYAN_LIGHT);
        SteelframePaint.fill(g, x, y + length, x + 2, y + length + 2, COPPER_DARK);
        SteelframePaint.fill(g, x + 4, y + length, x + 6, y + length + 2, COPPER_DARK);
        if (hover) {
            hoveredText = List.of(Component.translatable("guide.dimensionalflux.index.back"));
        }
    }

    private int ribbonX() {
        return rightPageX() + pageW - 30;
    }

    private boolean overRibbon(double mouseX, double mouseY) {
        return mouseX >= ribbonX() && mouseX < ribbonX() + 6 && mouseY >= bookY && mouseY < bookY + 24;
    }

    private void renderArrows(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (spread > 0) {
            boolean hover = overBack(mouseX, mouseY);
            arrow(g, backX(), arrowY(), false, hover);
        }
        if (spread < book.spreads() - 1) {
            boolean hover = overForward(mouseX, mouseY);
            arrow(g, forwardX(), arrowY(), true, hover);
        }
    }

    /** The discreet parchment page arrows, protected from external GUI recoloring. */
    private static void arrow(GuiGraphicsExtractor g, int x, int y, boolean forward, boolean hover) {
        String name = "page_" + (forward ? "forward" : "backward") + (hover ? "_highlighted" : "");
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                dimensionalflux.DimensionalFlux.location("textures/gui/icons/guide/" + name + ".png"),
                x, y, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
    }

    void hoverText(Component text) {
        hoveredText = List.of(text);
    }

    private int arrowY() {
        return bookY + bookH - ARROW_H - 4;
    }

    private int backX() {
        return leftPageX() + 26;
    }

    private int forwardX() {
        return rightPageX() + pageW - 26 - ARROW_W;
    }

    private boolean overBack(double mouseX, double mouseY) {
        return spread > 0 && mouseX >= backX() && mouseX < backX() + ARROW_W && mouseY >= arrowY() && mouseY < arrowY() + ARROW_H;
    }

    private boolean overForward(double mouseX, double mouseY) {
        return spread < book.spreads() - 1 && mouseX >= forwardX() && mouseX < forwardX() + ARROW_W
                && mouseY >= arrowY() && mouseY < arrowY() + ARROW_H;
    }

    // ---- helpers for the elements

    /** A recessed inventory slot with an item; hovering it shows the item tooltip. */
    void itemSlot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        SteelframePaint.fill(g, x, y, x + 18, y + 18, 0xFF8B8B8B);
        SteelframePaint.fill(g, x, y, x + 17, y + 1, 0xFF373737);
        SteelframePaint.fill(g, x, y, x + 1, y + 17, 0xFF373737);
        SteelframePaint.fill(g, x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        SteelframePaint.fill(g, x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        if (stack.isEmpty()) {
            return;
        }
        g.item(stack, x + 1, y + 1);
        g.itemDecorations(font, stack, x + 1, y + 1);
        if (mouseX >= x + 1 && mouseX < x + 17 && mouseY >= y + 1 && mouseY < y + 17) {
            SteelframePaint.fill(g, x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
            hoveredStack = stack;
        }
    }

    /** An item reference in the text is under the mouse: its icon and name are shown this frame. */
    void hoverReference(Identifier id) {
        hoveredReference = id;
    }

    /** Tooltip of an item reference: the item drawn large, with its name beside it. */
    private void renderReference(GuiGraphicsExtractor g, GuideReferences.Target reference, int mouseX, int mouseY) {
        Component name = reference.name().copy().withStyle(reference.stack().getRarity().getStyleModifier());
        int w = 32 + 6 + font.width(name);
        int h = 32;
        int x = mouseX + 12;
        if (x + w + 4 > width) {
            x = Math.max(4, mouseX - 12 - w);
        }
        int y = Mth.clamp(mouseY - 12, 4, height - h - 4);
        g.nextStratum();
        g.pose().pushMatrix();
        TooltipRenderUtil.extractTooltipBackground(g, x, y, w, h, null);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0F, 2.0F);
        g.item(reference.stack(), 0, 0);
        g.pose().popMatrix();
        SteelframePaint.text(g, font, name, x + 38, y + 12, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    /** A Steelframe button with its label. */
    void textButton(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        DfStyle.button(g, x, y, w, h, hover, false, false);
        fittedText(g, label, x + 3, y + (h - 8) / 2, w - 6, 0xFFFFFFFF, true);
    }

    /** One line of text, shrunk to fit {@code maxWidth}. */
    void fittedText(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color) {
        fittedText(g, text, x, y, maxWidth, color, false);
    }

    /**
     * One line of text at its normal size. When it does not fit, it slides from one end to the other inside its width,
     * like the long names on the device screens; only while a page turns (no clipping then) is it shrunk.
     */
    void fittedText(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color, boolean center) {
        int w = font.width(text);
        if (w > maxWidth && maxWidth > 0 && !turning()) {
            int overflow = w - maxWidth;
            double seconds = Util.getMillis() / 1000.0;
            double period = Math.max(overflow * 0.5, 3.0);
            double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
            int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
            g.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
            SteelframePaint.text(g, font, text, x - offset, y, color, false);
            g.disableScissor();
            return;
        }
        float scale = w > maxWidth && w > 0 ? (float) maxWidth / w : 1.0F;
        g.pose().pushMatrix();
        float drawX = center ? x + (maxWidth - w * scale) / 2.0F : x;
        g.pose().translate(drawX, y + (1 - scale) * 4);
        g.pose().scale(scale, scale);
        SteelframePaint.text(g, font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ---- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button() == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : 0;
        if (turning()) {
            return true;
        }
        if (overRibbon(mouseX, mouseY)) {
            turnTo(0);
            return true;
        }
        if (overBack(mouseX, mouseY)) {
            turnTo(spread - 1);
            return true;
        }
        if (overForward(mouseX, mouseY)) {
            turnTo(spread + 1);
            return true;
        }
        for (int side = 0; side < 2; side++) {
            int index = 2 * spread + side;
            int cx = (side == 0 ? leftPageX() : rightPageX()) + PAD_X;
            int cy = pageY() + PAD_TOP;
            if (index >= book.pages.size() || mouseX < cx || mouseX >= cx + contentW || mouseY < cy || mouseY >= cy + contentH) {
                continue;
            }
            for (GuideBook.Placed p : book.pages.get(index).elements()) {
                int top = cy + p.y();
                if (mouseY >= top && mouseY < top + p.height() && p.element().click(this, mouseX, mouseY, cx, top, contentW, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            turnTo(spread + 1);
        } else if (scrollY > 0) {
            turnTo(spread - 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_RIGHT, InputConstants.KEY_PAGEDOWN -> turnTo(spread + 1);
            case InputConstants.KEY_LEFT, InputConstants.KEY_PAGEUP -> turnTo(spread - 1);
            case InputConstants.KEY_HOME -> turnTo(0);
            default -> {
                if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
                    onClose();
                    return true;
                }
                return super.keyPressed(event);
            }
        }
        return true;
    }

    /** The screen closed (any way): the book in the hand closes too. */
    @Override
    public void removed() {
        super.removed();
        GuideBookAnimation.screenClosed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
