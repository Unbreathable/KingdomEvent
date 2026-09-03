package com.liphium.snowsplash.screens;

import com.liphium.core.inventory.CClickEvent;
import com.liphium.core.inventory.CItem;
import com.liphium.core.inventory.CScreen;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.snowsplash.Snowsplash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ItemShopScreen extends CScreen {

    public ItemShopScreen() {
        super(3, Component.text("Item shop", NamedTextColor.DARK_AQUA, TextDecoration.BOLD), 4, false);
    }

    @Override
    public void init(Player player, Inventory inventory) {
        background(player);

        // Add all the categories
        for (ShopCategory category : ShopCategory.values()) {
            setItemNotCached(player, 10 + category.ordinal(), new CItem(category.getStack()).onClick(event -> openCategory(event, category, inventory)));
        }
    }

    public void openCategory(CClickEvent event, ShopCategory category, Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (category.getItems().size() <= i) {
                setItemNotCached(event.player(), 18 + i, ShopCategory.spacer(), inventory);
            } else {
                setItemNotCached(event.player(), 18 + i, category.getItems().get(i), inventory);
            }
        }
    }

    public static void removeAmountFromInventory(Player player, Material material, int amount) {
        int count = amount;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                int sub = Math.min(item.getAmount(), count);
                int newAmount = item.getAmount() - sub;
                item.setAmount(newAmount);
                count -= sub;
                if (count <= 0) {
                    break;
                }
            }
        }
    }

    public enum ShopCategory {
        WEAPONS(
                new ItemStackBuilder(Material.IRON_SWORD)
                        .withName(Component.text("Weapons", NamedTextColor.RED, TextDecoration.BOLD))
                        .withLore(Component.text("Useful weapons.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.STONE_SWORD, "Stone sword", NamedTextColor.RED, 4, 1),
                        itemWithPrice(Material.IRON_SWORD, "Iron sword", NamedTextColor.RED, 10, 1),
                        itemWithPrice(Material.DIAMOND_SWORD, "Diamond sword", NamedTextColor.RED, 20, 1),
                        spacer(),
                        itemWithPrice(Material.IRON_AXE, "Iron axe", NamedTextColor.RED, 15, 1),
                        itemWithPrice(Material.DIAMOND_AXE, "Diamond axe", NamedTextColor.RED, 35, 1)
                )
        ),
        BOWS(
                new ItemStackBuilder(Material.BOW)
                        .withName(Component.text("Bows", NamedTextColor.RED, TextDecoration.BOLD))
                        .withLore(Component.text("Bows & utilities.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.BOW, "Bow", NamedTextColor.RED, 10, 1),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Punch", NamedTextColor.RED))
                                        .withEnchantments(Map.of(Enchantment.PUNCH, 1))
                                        .buildStack(),
                                15
                        ),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Power", NamedTextColor.RED))
                                        .withEnchantments(Map.of(Enchantment.POWER, 1))
                                        .buildStack(),
                                30
                        ),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Absolute power", NamedTextColor.RED))
                                        .withEnchantments(Map.of(Enchantment.POWER, 2))
                                        .buildStack(),
                                70
                        ),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Power & Punch", NamedTextColor.RED))
                                        .withEnchantments(Map.of(Enchantment.PUNCH, 1, Enchantment.POWER, 1))
                                        .buildStack(),
                                40
                        ),
                        itemWithPrice(Material.CROSSBOW, "Crossbow", NamedTextColor.RED, 40, 1),
                        spacer(),
                        itemWithPrice(Material.WIND_CHARGE, "Wind charge", NamedTextColor.RED, 2, 5),
                        itemWithPrice(Material.ARROW, "Arrow", NamedTextColor.RED, 1, 4)
                )
        ),
        ARROWS(
                new ItemStackBuilder(Material.RED_DYE)
                        .withName(Component.text("Effects", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .withLore(
                                Component.text("Traps & arrow effects.", NamedTextColor.GRAY),
                                Component.text("", NamedTextColor.GRAY),
                                Component.text("Right click on block - Use as trap", NamedTextColor.GRAY),
                                Component.text("Right click in the air - Use as arrow effect", NamedTextColor.GRAY)
                        )
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.GRAY_DYE, "Slowness", NamedTextColor.GOLD, 3, 1),
                        itemWithPrice(Material.LIME_DYE, "Poison", NamedTextColor.GOLD, 4, 1),
                        itemWithPrice(Material.GUNPOWDER, "Explosion", NamedTextColor.GOLD, 10, 1)
                )
        ),
        TOOLS(
                new ItemStackBuilder(Material.IRON_PICKAXE)
                        .withName(Component.text("Tools", NamedTextColor.AQUA, TextDecoration.BOLD))
                        .withLore(Component.text("Useful tools.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.IRON_PICKAXE, "Iron pickaxe", NamedTextColor.AQUA, 4, 1),
                        itemWithPrice(Material.IRON_SHOVEL, "Iron shovel", NamedTextColor.AQUA, 4, 1),
                        spacer(),
                        itemWithPrice(Material.DIAMOND_PICKAXE, "Diamond pickaxe", NamedTextColor.AQUA, 8, 1),
                        itemWithPrice(Material.DIAMOND_SHOVEL, "Diamond shovel", NamedTextColor.AQUA, 8, 1)
                )
        ),
        EXTRA(
                new ItemStackBuilder(Material.TNT)
                        .withName(Component.text("Extra", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .withLore(Component.text("Blocks, special items, etc.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.PACKED_ICE, "Ice", NamedTextColor.WHITE, 2, 16),
                        itemWithPrice(Material.SNOW_BLOCK, "Snow", NamedTextColor.WHITE, 4, 16),
                        itemWithPrice(Material.SPRUCE_LOG, "Spruce wood", NamedTextColor.WHITE, 8, 4),
                        itemWithPrice(Material.COBBLESTONE, "Cobblestone", NamedTextColor.WHITE, 8, 16),
                        spacer(),
                        itemWithPrice(Material.SPRUCE_BOAT, "Boat", NamedTextColor.WHITE, 4, 1),
                        itemWithPrice(Material.TNT, "TNT", NamedTextColor.WHITE, 8, 1),
                        itemWithPrice(Material.GOLDEN_APPLE, "Golden apple", NamedTextColor.WHITE, 3, 1)
                )
        );

        final ItemStack stack;
        final List<CItem> items;

        ShopCategory(ItemStack stack, List<CItem> items) {
            this.stack = stack;
            this.items = items;
        }

        public ItemStack getStack() {
            return stack;
        }

        public List<CItem> getItems() {
            return items;
        }

        private static final ItemStack item = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(Component.empty()).buildStack();

        public static CItem spacer() {
            return new CItem(item).notClickable();
        }

        public static CItem itemWithPrice(Material material, String name, NamedTextColor color, int price, int amount) {
            return new CItem(new ItemStackBuilder(material).withName(Component.text(name, color)).withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD))).withAmount(amount).buildStack()).onClick(event -> buyFunction(event, new ItemStackBuilder(material).withName(Component.text(name, color)).withAmount(amount).buildStack(), price));
        }

        public static CItem itemWithPriceCustom(ItemStack sold, int price) {
            return new CItem(new ItemStackBuilder(sold.getType()).withName(sold.getItemMeta().displayName()).withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD))).withEnchantments(sold.getEnchantments()).buildStack()).onClick(event -> buyFunction(event, sold, price));
        }

        public static void buyFunction(CClickEvent event, ItemStack stack, int price) {
            // Get the amount of pumpkins in the inventory
            int count = countMaterial(event.player(), Material.BLUE_ICE);

            if (count < price) {
                event.player().sendMessage(Snowsplash.PREFIX.append(Component.text("You don't have enough Blue Ice to purchase this item.", NamedTextColor.RED)));
                event.player().closeInventory();
                return;
            }

            // Remove the pumpkins from the players inventory
            removeAmountFromInventory(event.player(), Material.BLUE_ICE, price);

            event.player().getInventory().addItem(stack);
        }
    }

    public static int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
