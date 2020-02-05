/*
 * MIT License
 *
 * Copyright (c) 2019 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.glaremasters.guilds.guis;

import ch.jalu.configme.SettingsManager;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.configuration.sections.GuildInfoSettings;
import me.glaremasters.guilds.configuration.sections.GuildListSettings;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildHandler;
import me.glaremasters.guilds.utils.GuiBuilder;
import me.glaremasters.guilds.utils.GuiUtils;
import me.glaremasters.guilds.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by Glare
 * Date: 4/13/2019
 * Time: 3:51 PM
 */
public class ListGUI {

    private Guilds guilds;
    private SettingsManager settingsManager;
    private GuildHandler guildHandler;
    private List<GuiItem> items;
    private Map<String, Function<Guild, Object>> replacements = new HashMap<>();

    public ListGUI(Guilds guilds, SettingsManager settingsManager, GuildHandler guildHandler) {
        this.guilds = guilds;
        this.settingsManager = settingsManager;
        this.guildHandler = guildHandler;
        this.items = new ArrayList<>();
        loadReplacements();
    }

    private void loadReplacements() {
        SimpleDateFormat sdf = new SimpleDateFormat(settingsManager.getProperty(GuildListSettings.GUI_TIME_FORMAT));

        replacements.put("{guild}", Guild::getName);
        replacements.put("{player}", (guild) -> Bukkit.getOfflinePlayer(guild.getGuildMaster().getUuid()).getName());
        replacements.put("{guild-name}", Guild::getName);
        replacements.put("{guild-prefix}", Guild::getPrefix);
        replacements.put("{guild-master}", (guild) -> Bukkit.getOfflinePlayer(guild.getGuildMaster().getUuid()).getName());
        replacements.put("{guild-tier}", (guild) -> guild.getTier().getLevel());
        replacements.put("{guild-balance}", Guild::getBalance);
        replacements.put("{guild-member-count}", Guild::getSize);
        replacements.put("{guild-challenge-win}", (guild) -> guild.getGuildScore().getWins());
        replacements.put("{guild-challenge-loses", (guild) -> guild.getGuildScore().getLoses());
        replacements.put("{guild-tier-name}", (guild) -> guild.getTier().getName());
        replacements.put("{guild-status}", (guild) -> settingsManager.getProperty(guild.isPrivate() ? GuildInfoSettings.STATUS_PRIVATE  : GuildInfoSettings.STATUS_PUBLIC));
        replacements.put("{creation}", (guild) -> sdf.format(guild.getCreationDate()));
    }

    public Gui getListGUI() {
        String name = settingsManager.getProperty(GuildListSettings.GUILD_LIST_NAME);

        Gui gui = new GuiBuilder(guilds).setName(name).setRows(6).blockGlobalClick().build();

        // Prepare a paginated pane
        PaginatedPane paginatedPane = new PaginatedPane(0, 0, 9, 5);

        // Prepare the buttons
        StaticPane pane = new StaticPane(0, 5, 9, 1);

        createButtons(pane, paginatedPane, gui);

        // Add the items to the pane
        createListItems(paginatedPane);

        // Add the pane to the GUI
        gui.addPane(paginatedPane);

        gui.addPane(pane);

        // Return the GUI
        return gui;
    }

    private void createButtons(StaticPane pane, PaginatedPane paginatedPane, Gui gui) {
        // Next Button
        pane.addItem(new GuiItem(GuiUtils.createItem(settingsManager.getProperty(GuildListSettings.GUILD_LIST_NEXT_PAGE_ITEM), settingsManager.getProperty(GuildListSettings.GUILD_LIST_NEXT_PAGE_ITEM_NAME), new ArrayList<>()), event -> {
            if (!((paginatedPane.getPage() + 1) + 1 > paginatedPane.getPages())) {
                paginatedPane.setPage(paginatedPane.getPage() + 1);
                gui.update();
            }
        }), 8, 0);
        // Back Button
        pane.addItem(new GuiItem(GuiUtils.createItem(settingsManager.getProperty(GuildListSettings.GUILD_LIST_PREVIOUS_PAGE_ITEM), settingsManager.getProperty(GuildListSettings.GUILD_LIST_PREVIOUS_PAGE_ITEM_NAME), new ArrayList<>()), event -> {
            if (!((paginatedPane.getPage() - 1) < 0)) {
                paginatedPane.setPage(paginatedPane.getPage() - 1);
                gui.update();
            }
        }), 0, 0);
    }

    /**
     * Create all the items for the GUI
     *
     * @param pane the pane to add the items to
     */
    private void createListItems(PaginatedPane pane) {
        List<Guild> guilds = guildHandler.getGuilds();
        String sortOrder = settingsManager.getProperty(GuildListSettings.GUILD_LIST_SORT).toUpperCase();

        switch (sortOrder) {
            case "TIER":
                guilds.sort(Comparator.<Guild>comparingInt(g -> g.getTier().getLevel()).reversed());
                break;
            case "MEMBERS":
                guilds.sort(Comparator.<Guild>comparingInt(g -> g.getMembers().size()).reversed());
                break;
            case "BALANCE":
                guilds.sort(Comparator.comparingDouble(Guild::getBalance).reversed());
                break;
            case "WINS":
                guilds.sort(Comparator.<Guild>comparingInt(g -> g.getGuildScore().getWins()).reversed());
                break;
            case "NAME":
                guilds.sort(Comparator.comparing(Guild::getName));
                break;
            case "AGE":
                guilds.sort(Comparator.comparingLong(Guild::getCreationDate));
                break;
            default:
            case "LOADED":
                break;
        }

        // Loop through each guild to create the item
        guilds.forEach(this::setListItem);
        pane.populateWithGuiItems(items);
        items.clear();
    }

    /**
     * Set the item to the list
     *
     * @param guild the guild of the pane
     */
    private void setListItem(Guild guild) {

        GuiItem listItem = new GuiItem(guild.getSkull(), event -> {
            guilds.getGuiHandler().getInfoMembersGUI().getInfoMembersGUI(guild).show(event.getWhoClicked());
            event.setCancelled(true);
        });
        ItemMeta meta = listItem.getItem().getItemMeta();
        meta.setDisplayName(StringUtils.color(settingsManager.getProperty(GuildListSettings.GUILD_LIST_ITEM_NAME).replace("{player}", Bukkit.getOfflinePlayer(guild.getGuildMaster().getUuid()).getName()).replace("{guild}", guild.getName())));
        meta.setLore(updatedLore(guild, settingsManager.getProperty(GuildListSettings.GUILD_LIST_HEAD_LORE)));
        listItem.getItem().setItemMeta(meta);
        items.add(listItem);
    }

    /**
     * Update lore with replacements
     *
     * @param guild the guild being edited
     * @param lore  the lore to change
     * @return updated lore
     */
    private List<String> updatedLore(Guild guild, List<String> lore) {
        final List<String> updated = new ArrayList<>(lore);
        replacements.forEach((k, v) -> updated.replaceAll(s -> s.replace("{" + k + "}", String.valueOf(v.apply(guild)))));
        return updated;
    }

}
