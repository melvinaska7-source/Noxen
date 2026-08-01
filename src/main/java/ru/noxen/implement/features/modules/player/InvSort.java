package ru.noxen.implement.features.modules.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.BindSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.implement.events.keyboard.KeyEvent;
import ru.noxen.implement.events.player.TickEvent;
import ru.noxen.implement.features.draggables.Notifications;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class InvSort extends Module implements QuickImports {

    public static InvSort getInstance() {
        return ru.noxen.common.util.other.Instance.get(InvSort.class);
    }

    public final BindSetting saveBind = new BindSetting("Save", "Remembers the current layout of your inventory");
    public final BindSetting loadBind = new BindSetting("Load", "Restores the saved layout, drops anything that wasn't part of it");
    public final ValueSetting delaySetting = new ValueSetting("Delay", "Delay between actions (ms)").range(50, 500).setValue(120);

    // 36 slots: 0-8 hotbar, 9-35 main inventory storage (matches PlayerInventory.main indexing)
    private final Item[] savedLayout = new Item[36];
    private boolean hasSavedLayout = false;

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private long lastActionTime = 0;

    public InvSort() {
        super("InvSort", "Inv-Sort", ModuleCategory.PLAYER);
        setup(saveBind, loadBind, delaySetting);
        loadFromDisk();
    }

    @Override
    public void deactivate() {
        queue.clear();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null) return;
        if (e.isKeyDown(saveBind.getKey())) saveInventory();
        if (e.isKeyDown(loadBind.getKey())) loadInventory();
    }

    @EventHandler
    public void onTick(TickEvent tick) {
        if (queue.isEmpty()) return;
        long delay = (long) delaySetting.getValue();
        if (System.currentTimeMillis() - lastActionTime < delay) return;
        queue.poll().run();
        lastActionTime = System.currentTimeMillis();
    }

    private void saveInventory() {
        if (mc.player == null) return;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            savedLayout[i] = stack.isEmpty() ? null : stack.getItem();
        }
        hasSavedLayout = true;
        saveToDisk();
        Notifications.getInstance().addList(net.minecraft.text.Text.literal("Inv-Sort: layout saved"), 3000);
    }

    private void loadInventory() {
        if (mc.player == null) return;
        if (!hasSavedLayout) {
            Notifications.getInstance().addList(net.minecraft.text.Text.literal("Inv-Sort: no saved layout yet"), 3000);
            return;
        }

        queue.clear();

        // Working copy of current items so we can plan the whole sequence
        // against a predicted state, same idea the original chest-sorter used.
        Item[] current = new Item[36];
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            current[i] = stack.isEmpty() ? null : stack.getItem();
        }

        // Pass 1: put every tracked item back where it was saved.
        for (int target = 0; target < 36; target++) {
            Item wanted = savedLayout[target];
            if (wanted == null) continue; // slot was empty at save time -> don't touch it
            if (current[target] == wanted) continue; // already correct

            int source = -1;
            for (int j = 0; j < 36; j++) {
                if (j == target) continue;
                if (current[j] == wanted) {
                    source = j;
                    break;
                }
            }
            if (source == -1) continue; // item is missing entirely -> nothing we can do

            int a = target, b = source;
            queue.add(() -> swapSlots(toScreenSlot(a), toScreenSlot(b)));

            Item tmp = current[target];
            current[target] = current[source];
            current[source] = tmp;
        }

        // Pass 2: anything left that isn't part of the saved layout at all -> junk, drop it.
        for (int i = 0; i < 36; i++) {
            Item item = current[i];
            if (item == null) continue;
            if (isPartOfSavedLayout(item)) continue;

            int slot = i;
            queue.add(() -> dropSlot(toScreenSlot(slot)));
            current[i] = null;
        }

        Notifications.getInstance().addList(net.minecraft.text.Text.literal("Inv-Sort: sorting..."), 3000);
    }

    private boolean isPartOfSavedLayout(Item item) {
        for (Item saved : savedLayout) {
            if (saved == item) return true;
        }
        return false;
    }

    // PlayerInventory.main uses 0-8 for the hotbar and 9-35 for storage.
    // The player's own screen handler numbers them differently: hotbar is 36-44,
    // storage stays 9-35. This converts our simple index into that slot id.
    private int toScreenSlot(int invIndex) {
        return invIndex < 9 ? 36 + invIndex : invIndex;
    }

    private void swapSlots(int slotA, int slotB) {
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotB, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.SWAP, mc.player);
    }

    private void dropSlot(int slot) {
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 1, SlotActionType.THROW, mc.player);
    }

    // ===== Persistence: saved to <run dir>/noxen/inv/layout.json =====

    private File getSaveDirectory() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, "noxen" + File.separator + "inv");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void saveToDisk() {
        try {
            String[] ids = new String[36];
            for (int i = 0; i < 36; i++) {
                Item item = savedLayout[i];
                ids[i] = item == null ? null : Registries.ITEM.getId(item).toString();
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File file = new File(getSaveDirectory(), "layout.json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(ids, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadFromDisk() {
        try {
            File file = new File(getSaveDirectory(), "layout.json");
            if (!file.exists()) return;

            Gson gson = new Gson();
            try (FileReader reader = new FileReader(file)) {
                String[] ids = gson.fromJson(reader, String[].class);
                if (ids == null || ids.length != 36) return;
                for (int i = 0; i < 36; i++) {
                    savedLayout[i] = ids[i] == null ? null : Registries.ITEM.get(Identifier.of(ids[i]));
                }
                hasSavedLayout = true;
            }
        } catch (Exception ignored) {
        }
    }
}
