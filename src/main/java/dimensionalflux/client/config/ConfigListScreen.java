package dimensionalflux.client.config;

import dimensionalflux.client.ClientCompat;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Registry ID and tag editor. Changes stay in the parent draft until Done is pressed there. */
final class ConfigListScreen extends Screen {
    private final Screen parent;
    private final String key;
    private final List<String> entries = new ArrayList<>();
    private final Consumer<List<String>> apply;
    private int scroll, pageSize;
    private boolean rebuild;
    private Component error = Component.empty();

    ConfigListScreen(Screen parent, Component title, String key, List<?> values, Consumer<List<String>> apply) {
        super(title);
        this.parent = parent;
        this.key = key;
        this.apply = apply;
        values.forEach(value -> entries.add(value.toString()));
    }

    @Override
    protected void init() {
        int rowWidth = Math.min(380, width - 24), left = (width - rowWidth) / 2;
        pageSize = Math.max(1, (height - 100) / 24);
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - pageSize));
        for (int i = scroll; i < Math.min(entries.size(), scroll + pageSize); i++) {
            int index = i, y = 42 + (i - scroll) * 24;
            var input = new EditBox(font, left, y, rowWidth - 26, 20, title);
            input.setMaxLength(256);
            input.setValue(entries.get(i));
            input.setResponder(value -> { entries.set(index, value); error = Component.empty(); });
            addRenderableWidget(input);
            addRenderableWidget(ConfigStyle.button(Component.literal("×"), left + rowWidth - 20, y, 20, 20,
                    button -> { entries.remove(index); rebuild = true; }));
        }
        var add = addRenderableWidget(ConfigStyle.button(DimensionalConfigScreen.text("add_entry"), left, height - 50,
                rowWidth, 20, button -> { entries.add(""); scroll = Math.max(0, entries.size() - pageSize); rebuild = true; }));
        add.active = entries.size() < 1024;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left, height - 26, (rowWidth - 4) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            try {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) DimensionalConfig.checkedValue(key,
                        entries.stream().map(String::trim).filter(value -> !value.isEmpty()).toList(), false);
                apply.accept(values);
                onClose();
            } catch (IllegalArgumentException invalid) { error = DimensionalConfigScreen.text("invalid_entry"); }
        }).bounds(left + (rowWidth + 4) / 2, height - 26, rowWidth / 2 - 2, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (rebuild) { rebuild = false; rebuildWidgets(); }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        SteelframePaint.text(g, font, title, (width - font.width(title)) / 2, 10, ConfigStyle.ACCENT, false);
        Component hint = error.getString().isEmpty() ? DimensionalConfigScreen.text("list_hint") : error;
        SteelframePaint.text(g, font, hint, 12, 27, error.getString().isEmpty() ? ConfigStyle.TEXT : 0xFFFF7777, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical != 0) { scroll -= (int) Math.signum(vertical); rebuild = true; return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void onClose() { ClientCompat.setScreen(parent); }
}
