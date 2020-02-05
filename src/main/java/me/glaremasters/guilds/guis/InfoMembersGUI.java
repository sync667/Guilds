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
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.configuration.sections.GuildInfoMemberSettings;
import me.glaremasters.guilds.configuration.sections.GuildListSettings;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildHandler;
import me.glaremasters.guilds.guild.GuildMember;
import me.glaremasters.guilds.guild.GuildRole;
import me.glaremasters.guilds.utils.GuiBuilder;
import me.glaremasters.guilds.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Glare
 * Date: 5/12/2019
 * Time: 1:57 PM
 */
public class InfoMembersGUI {

    private Guilds guilds;
    private SettingsManager settingsManager;
    private GuildHandler guildHandler;

    public InfoMembersGUI(Guilds guilds, SettingsManager settingsManager, GuildHandler guildHandler) {
        this.guilds = guilds;
        this.settingsManager = settingsManager;
        this.guildHandler = guildHandler;
    }

    public Gui getInfoMembersGUI(Guild guild) {
        String name = settingsManager.getProperty(GuildInfoMemberSettings.GUI_NAME).replace("{name}", guild.getName());

        Gui gui = new GuiBuilder(guilds).setName(name).setRows(6).addBackground(9, 6).blockGlobalClick().build();

        // Prevent players from being able to items into the GUIs
        gui.setOnOutsideClick(event -> {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            Guild g = guildHandler.getGuild(player);
            if (g == null) {
                guilds.getGuiHandler().getListGUI().getListGUI().show(event.getWhoClicked());
            } else {
                guilds.getGuiHandler().getInfoGUI().getInfoGUI(g, player).show(event.getWhoClicked());
            }
        });

        // Create the pane for the main items
        OutlinePane foregroundPane = new OutlinePane(0, 0, 9, 6, Pane.Priority.NORMAL);

        // Add the items to the foreground pane
        createForegroundItems(foregroundPane, guild);


        // Add the foreground pane to the GUI
        gui.addPane(foregroundPane);

        // Return the create GUI object
        return gui;
    }

    /**
     * Create the regular items that will be on the GUI
     *
     * @param pane  the pane to be added to
     * @param guild the guild of the player
     */
    private void createForegroundItems(OutlinePane pane, Guild guild) {

        List<GuildMember> members = guild.getMembers();

        String sortOrder = settingsManager.getProperty(GuildInfoMemberSettings.SORT_ORDER).toUpperCase();

        switch (sortOrder) {
            default:
            case "ROLE":
                members.sort(Comparator.comparingInt(g -> g.getRole().getLevel()));
                break;
            case "NAME":
                members.sort(Comparator.comparing(GuildMember::getName));
                break;
            case "AGE":
                members.sort(Comparator.comparingLong(GuildMember::getJoinDate));
                break;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(settingsManager.getProperty(GuildListSettings.GUI_TIME_FORMAT));

        members.forEach(m -> {

            // Create a variable for the status
            String status = settingsManager.getProperty(m.isOnline() ? GuildInfoMemberSettings.MEMBERS_ONLINE : GuildInfoMemberSettings.MEMBERS_OFFLINE);

            // Create a variable for the role
            GuildRole role = guildHandler.getGuildRole(m.getRole().getLevel());

            pane.addItem(new GuiItem(GuiUtils.createItem(settingsManager.getProperty(GuildInfoMemberSettings.MEMBERS_MATERIAL),
                    settingsManager.getProperty(GuildInfoMemberSettings.MEMBERS_NAME).replace("{player}", m.getName()),
                    settingsManager.getProperty(GuildInfoMemberSettings.MEMBERS_LORE).stream().map(l ->
                            l.replace("{name}", m.getName())
                                    .replace("{role}", role.getName())
                                    .replace("{join}", sdf.format(new Date(m.getJoinDate())))
                                    .replace("{login}", sdf.format(new Date(m.getLastLogin())))
                                    .replace("{status}", status)).collect(Collectors.toList())),
                    event -> event.setCancelled(true)));
        });
    }
}
