package org.Cry_Legende.sHSignEdit;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SHSignEdit extends JavaPlugin implements Listener {

    private String denyMessage;
    private String notYourIslandMessage; // Nouveau message pour l'île
    private List<String> whitelistedWorlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        denyMessage = ChatColor.translateAlternateColorCodes('&', config.getString("denyMessage", "&7&l« &c&lSH &8&l» &7Vous devez être minimum &fVIP &7pour pouvoir éditer les pancartes"));
        notYourIslandMessage = ChatColor.translateAlternateColorCodes('&', config.getString("notYourIslandMessage", "&7&l« &c&lSH &8&l» &7Vous ne pouvez modifier que les pancartes sur votre propre île"));
        whitelistedWorlds = config.getStringList("whitelisted-worlds");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Vérifier la permission admin pour contourner toutes les restrictions
        if (player.hasPermission("shsignedit.bypass")) {
            return; // Autoriser l'événement sans autres vérifications
        }

        // Vérifier si le monde est dans la whitelist
        if (!whitelistedWorlds.contains(location.getWorld().getName())) {
            event.setCancelled(true);
            return;
        }

        // Vérifier si le joueur est sur son île (SuperiorSkyBlock2)
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        if (island != null) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            if (!island.isMember(superiorPlayer)) {
                event.setCancelled(true);
                player.sendMessage(notYourIslandMessage); // Message spécifique pour l'île
                return;
            }
        }

        // Vérifier si le panneau est vide (nouvelle création)
        boolean isNewSign = isEmptySign(event.getBlock());
        if (isNewSign) {
            return; // Autoriser la création initiale du panneau
        }

        // Vérifier la permission globale
        if (!player.hasPermission("shsignedit.use")) {
            event.setCancelled(true);
            player.sendMessage(denyMessage); // Message générique pour manque de permission
            return;
        }

        // Si toutes les vérifications sont passées, la modification est autorisée
    }

    private boolean isEmptySign(org.bukkit.block.Block block) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (String line : sign.getLines()) {
                if (line != null && !line.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}