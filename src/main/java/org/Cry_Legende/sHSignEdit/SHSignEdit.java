package org.Cry_Legende.sHSignEdit;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
        String refusal = refusalFor(event.getPlayer(), event.getBlock());
        if (refusal != null) {
            event.setCancelled(true);
            if (!refusal.isEmpty()) {
                event.getPlayer().sendMessage(refusal);
            }
        }
    }

    // Refuse l'éditeur avant que le joueur ne tape, pour les mêmes règles que onSignChange
    @EventHandler
    public void onOpenSign(PlayerOpenSignEvent event) {
        // Éditeur ouvert par un autre plugin (saisie de texte) : pas une édition de pancarte par le joueur
        if (event.getCause() == PlayerOpenSignEvent.Cause.PLUGIN) {
            return;
        }
        String refusal = refusalFor(event.getPlayer(), event.getSign().getBlock());
        if (refusal != null) {
            event.setCancelled(true);
            if (!refusal.isEmpty()) {
                event.getPlayer().sendMessage(refusal);
            }
        }
    }

    /**
     * Règles d'édition d'une pancarte. Renvoie null si l'édition est autorisée,
     * une chaîne vide si elle est refusée sans message, sinon le message à envoyer.
     */
    private String refusalFor(Player player, Block block) {
        // Vérifier la permission admin pour contourner toutes les restrictions
        if (player.hasPermission("shsignedit.bypass")) {
            return null;
        }

        // Vérifier si le monde est dans la whitelist
        Location location = block.getLocation();
        if (!whitelistedWorlds.contains(location.getWorld().getName())) {
            return "";
        }

        // Vérifier si le joueur est sur son île (SuperiorSkyBlock2)
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        if (island != null) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            if (!island.isMember(superiorPlayer)) {
                return notYourIslandMessage; // Message spécifique pour l'île
            }
        }

        // Vérifier si le panneau est vide (nouvelle création)
        if (isEmptySign(block)) {
            return null; // Autoriser la création initiale du panneau
        }

        // Vérifier la permission globale
        if (!player.hasPermission("shsignedit.use")) {
            return denyMessage; // Message générique pour manque de permission
        }

        // Si toutes les vérifications sont passées, la modification est autorisée
        return null;
    }

    private boolean isEmptySign(Block block) {
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