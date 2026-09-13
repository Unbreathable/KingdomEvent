package com.liphium.kingdom.screens;

import com.liphium.core.inventory.CClickEvent;
import com.liphium.core.inventory.CItem;
import com.liphium.core.inventory.CScreen;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.state.IngameState;
import com.liphium.kingdom.game.team.Team;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class ItemShopScreen extends CScreen {

    public static final Material CURRENCY = Material.GOLD_NUGGET;

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
            } else if (category == ShopCategory.UPGRADES && i == 0) {
                // Build upgrade items dynamically so the price reflects the team's current level
                setItemNotCached(event.player(), 18 + i, coinDropperUpgradeItem(event.player()), inventory);
            } else {
                setItemNotCached(event.player(), 18 + i, category.getItems().get(i), inventory);
            }
        }
    }

    /**
     * Coin dropper upgrade item with the current level and the price for the next level.
     */
    private CItem coinDropperUpgradeItem(Player player) {
        Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
        int level = team == null ? 1 : team.getCoinDropperLevel();

        var name = Component.text("Coin dropper upgrade", NamedTextColor.GOLD);
        var levelLine = Component.text("Current level: ", NamedTextColor.GRAY)
                .append(Component.text(level, NamedTextColor.GOLD, TextDecoration.BOLD));
        CItem item;
        if (level >= 5) {
            item = new CItem(new ItemStackBuilder(Material.GOLD_NUGGET)
                    .withName(name)
                    .withLore(levelLine, Component.text("Fully upgraded!", NamedTextColor.RED))
                    .buildStack());
        } else {
            item = new CItem(new ItemStackBuilder(Material.GOLD_NUGGET)
                    .withName(name)
                    .withLore(levelLine,
                            Component.text("Makes your coin dropper faster.", NamedTextColor.GRAY),
                            Component.text("Price: ", NamedTextColor.GRAY)
                                    .append(Component.text(ShopCategory.upgradeCost(level), NamedTextColor.GOLD)))
                    .buildStack()).onClick(ShopCategory::buyCoinDropperUpgrade);
        }
        return item;
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

    public static ItemStack tntBow() {
        ItemStack stack = new ItemStackBuilder(Material.BOW)
                .withName(Component.text("TNT Bow", NamedTextColor.GOLD))
                .withLore(Component.text("Shoots exploding arrows.", NamedTextColor.GRAY),
                        Component.text("5 uses only!", NamedTextColor.RED))
                .buildStack();
        final var meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(Kingdom.TNT_BOW_KEY, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);

        // Add cooldown meta to make it detected
        var cooldown = UseCooldown.useCooldown(5).cooldownGroup(IngameState.TNT_BOW_COOLDOWN_KEY).build();
        stack.setData(DataComponentTypes.USE_COOLDOWN, cooldown);

        stack.setData(DataComponentTypes.MAX_DAMAGE, 5);
        stack.setData(DataComponentTypes.DAMAGE, 0);
        return stack;
    }

    public enum ShopCategory {
        TOOLS(
                new ItemStackBuilder(Material.IRON_SWORD)
                        .withName(Component.text("Tools", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .withLore(Component.text("Weapons & different tools.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.IRON_SWORD, "Iron sword", NamedTextColor.LIGHT_PURPLE, 10, 1),
                        itemWithPrice(Material.IRON_AXE, "Iron axe", NamedTextColor.LIGHT_PURPLE, 15, 1),
                        itemWithPrice(Material.IRON_PICKAXE, "Iron pickaxe", NamedTextColor.LIGHT_PURPLE, 10, 1),
                        itemWithPrice(Material.IRON_SHOVEL, "Iron axe", NamedTextColor.LIGHT_PURPLE, 10, 1)
                )
        ),
        RANGED(
                new ItemStackBuilder(Material.BOW)
                        .withName(Component.text("Ranged", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .withLore(Component.text("Bows & explosives.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.CROSSBOW, "Crossbow", NamedTextColor.GOLD, 15, 1),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Infinity bow", NamedTextColor.GOLD))
                                        .withEnchantments(Map.of(Enchantment.INFINITY, 1))
                                        .buildStack(),
                                20
                        ),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.BOW)
                                        .withName(Component.text("Power bow", NamedTextColor.GOLD))
                                        .withEnchantments(Map.of(Enchantment.POWER, 2))
                                        .buildStack(),
                                25
                        ),
                        itemWithPriceCustom(tntBow(), 30),
                        spacer(),
                        itemWithPrice(Material.ARROW, "Arrow", NamedTextColor.GOLD, 1, 4),
                        itemWithPrice(Material.WIND_CHARGE, "Wind charge", NamedTextColor.GOLD, 2, 5)
                )
        ),
        UTILITY(
                new ItemStackBuilder(Material.TNT)
                        .withName(Component.text("Utility", NamedTextColor.RED, TextDecoration.BOLD))
                        .withLore(
                                Component.text("Different utility items & traps.", NamedTextColor.GRAY)
                        )
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.TNT, "TNT", NamedTextColor.RED, 8, 1),
                        itemWithPrice(Material.FIRE_CHARGE, "Fire charge", NamedTextColor.RED, 16, 1),
                        spacer(),
                        itemWithPrice(Material.GRAY_DYE, "Slowness trap", NamedTextColor.RED, 20, 1),
                        itemWithPrice(Material.LIME_DYE, "Poison trap", NamedTextColor.RED, 20, 1),
                        itemWithPrice(Material.WHITE_DYE, "Web trap", NamedTextColor.RED, 30, 1),
                        itemWithPrice(Material.GUNPOWDER, "Explosion trap", NamedTextColor.RED, 40, 1),
                        spacer(),
                        itemWithPrice(Material.GOLDEN_APPLE, "Golden apple", NamedTextColor.RED, 10, 3)
                )
        ),
        UPGRADES(
                new ItemStackBuilder(Material.GOLD_BLOCK)
                        .withName(Component.text("Upgrades", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .withLore(Component.text("Team upgrades.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        new CItem(new ItemStackBuilder(Material.GOLD_NUGGET)
                                .withName(Component.text("Coin dropper upgrade", NamedTextColor.GOLD))
                                .withLore(Component.text("Makes your coin dropper faster.", NamedTextColor.GRAY),
                                        Component.text("Click to see the price.", NamedTextColor.GRAY))
                                .buildStack()).onClick(ShopCategory::buyCoinDropperUpgrade),
                        spacer(),
                        new CItem(new ItemStackBuilder(Material.IRON_ORE)
                                .withName(Component.text("Iron ore", NamedTextColor.AQUA))
                                .withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(10, NamedTextColor.GOLD)),
                                        Component.text("Gets placed at your base.", NamedTextColor.GRAY))
                                .buildStack()).onClick(event -> buyOre(event, Material.IRON_ORE, 10)),
                        new CItem(new ItemStackBuilder(Material.DIAMOND_ORE)
                                .withName(Component.text("Diamond ore", NamedTextColor.AQUA))
                                .withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(30, NamedTextColor.GOLD)),
                                        Component.text("Gets placed at your base.", NamedTextColor.GRAY))
                                .buildStack()).onClick(event -> buyOre(event, Material.DIAMOND_ORE, 30))
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
            return new CItem(new ItemStackBuilder(sold.getType())
                    .withName(sold.getItemMeta().hasItemName() ? sold.getItemMeta().itemName() : sold.getItemMeta().displayName())
                    .withLore(Component.text("Price: ", NamedTextColor.GRAY)
                            .append(Component.text(price, NamedTextColor.GOLD)))
                    .withEnchantments(sold.getEnchantments()).buildStack())
                    .onClick(event -> buyFunction(event, sold, price));
        }

        private static void buyOre(CClickEvent event, Material ore, int price) {
            Player player = event.player();
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
            if (team == null) {
                return;
            }

            if (!(Kingdom.getInstance().getGameManager().getCurrentState() instanceof IngameState ingame)) {
                player.sendMessage(Kingdom.PREFIX.append(Component.text("You can only buy ores during the game.", NamedTextColor.RED)));
                return;
            }

            // Buy (the coin check happens inside)
            ingame.buyOre(player, team, ore);
        }

        private static void buyCoinDropperUpgrade(CClickEvent event) {
            Player player = event.player();
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
            if (team == null) {
                return;
            }

            int level = team.getCoinDropperLevel();
            if (level >= 5) {
                player.sendMessage(Kingdom.PREFIX.append(Component.text("Your coin dropper is already at max level!", NamedTextColor.RED)));
                player.closeInventory();
                return;
            }

            int price = upgradeCost(level);
            if (countMaterial(player, CURRENCY) < price) {
                player.sendMessage(Kingdom.PREFIX.append(Component.text("You don't have enough coins to purchase this upgrade.", NamedTextColor.RED)));
                player.closeInventory();
                return;
            }

            removeAmountFromInventory(player, CURRENCY, price);
            team.setCoinDropperLevel(level + 1);
            player.sendMessage(Kingdom.PREFIX.append(Component.text("Your coin dropper is now level ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(level + 1), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY))));
            player.closeInventory();
        }

        public static int upgradeCost(int currentLevel) {
            // Costs to get from level 1 to 5
            return switch (currentLevel) {
                case 1 -> 5;
                case 2 -> 10;
                case 3 -> 20;
                case 4 -> 40;
                default -> 80;
            };
        }

        public static void buyFunction(CClickEvent event, ItemStack stack, int price) {
            // Get the amount of coins in the inventory
            int count = countMaterial(event.player(), CURRENCY);

            if (count < price) {
                event.player().sendMessage(Kingdom.PREFIX.append(Component.text("You don't have enough coins to purchase this item.", NamedTextColor.RED)));
                event.player().closeInventory();
                return;
            }

            // Remove the coins from the players inventory
            removeAmountFromInventory(event.player(), CURRENCY, price);

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
