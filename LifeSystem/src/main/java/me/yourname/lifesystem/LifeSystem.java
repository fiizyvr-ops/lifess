
package me.yourname.lifesystem;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LifeSystem extends JavaPlugin implements Listener {

    private static final Key HEART_FONT =
            Key.key("lifesystem", "hearts");

    private static final String DISPLAY_TAG =
            "lifesystem_heart_display";

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, TextDisplay> displays = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLives();

        getServer().getPluginManager().registerEvents(this, this);

        removeOldDisplays();
        removeOldScoreboards();

        Bukkit.getScheduler().runTaskTimer(
                this,
                this::updateAllPlayers,
                1L,
                1L
        );

        getLogger().info("LifeSystem enabled!");
    }

    @Override
    public void onDisable() {
        saveLives();

        for (TextDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        displays.clear();
    }

    private void loadLives() {
        lives.clear();

        if (getConfig().isConfigurationSection("lives")) {
            for (String key :
                    getConfig().getConfigurationSection("lives").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    lives.put(uuid, getConfig().getInt("lives." + key, 5));
                } catch (IllegalArgumentException ignored) {
                    getLogger().warning("Invalid UUID: " + key);
                }
            }
        }
    }

    private void saveLives() {
        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            getConfig().set(
                    "lives." + entry.getKey(),
                    entry.getValue()
            );
        }

        saveConfig();
    }

    private int getLives(Player player) {
        return lives.getOrDefault(player.getUniqueId(), 5);
    }

    private Component createHeart(int amount) {
        String glyph = switch (amount) {
            case 5 -> "\uE000";
            case 4 -> "\uE001";
            case 3 -> "\uE002";
            case 2 -> "\uE003";
            case 1 -> "\uE004";
            case 0 -> "\uE005";
            default -> "\uE006";
        };

        return Component.text(glyph).font(HEART_FONT);
    }

    private void updatePlayer(Player player) {
        int amount = getLives(player);
        Component heart = createHeart(amount);

        // TAB LIST HEART
        player.playerListName(
                Component.text(player.getName() + "  ")
                        .append(heart)
        );

        // CREATE OR UPDATE TRANSPARENT HEART DISPLAY
        TextDisplay display = displays.get(player.getUniqueId());

        if (display == null ||
                !display.isValid() ||
                !display.getWorld().equals(player.getWorld())) {

            if (display != null && display.isValid()) {
                display.remove();
            }

            Location location = player.getLocation().clone()
                    .add(0, 2.35, 0);

            display = player.getWorld().spawn(
                    location,
                    TextDisplay.class,
                    entity -> {
                        entity.addScoreboardTag(DISPLAY_TAG);
                        entity.setPersistent(false);
                        entity.setInvulnerable(true);
                        entity.setSilent(true);

                        entity.setBillboard(Display.Billboard.CENTER);
                        entity.setAlignment(
                                TextDisplay.TextAlignment.CENTER
                        );

                        // REMOVE THE BACKGROUND
                        entity.setDefaultBackground(false);
                        entity.setBackgroundColor(
                                org.bukkit.Color.fromARGB(0, 0, 0, 0)
                        );

                        entity.setSeeThrough(true);
                        entity.setShadowed(false);
                    }
            );

            displays.put(player.getUniqueId(), display);
        }

        // Keep the heart above the player's head
        display.teleport(
                player.getLocation().clone().add(0, 2.35, 0)
        );

        display.text(heart);
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private void removeOldDisplays() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay &&
                        entity.getScoreboardTags().contains(DISPLAY_TAG)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeOldScoreboards() {
        Scoreboard main =
                Bukkit.getScoreboardManager().getMainScoreboard();

        Objective mainObjective = main.getObjective("lifesystem");

        if (mainObjective != null) {
            mainObjective.unregister();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = player.getScoreboard();

            Objective objective = board.getObjective("lifesystem");

            if (objective != null) {
                objective.unregister();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(this,
                () -> updatePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TextDisplay display = displays.remove(
                event.getPlayer().getUniqueId()
        );

        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updatePlayer(event.getPlayer());
    }

    public void setLives(Player player, int amount) {
        lives.put(player.getUniqueId(), Math.max(0, amount));
        saveLives();
        updateAllPlayers();
    }

    public void addLife(Player player) {
        setLives(player, getLives(player) + 1);
    }

    public void removeLife(Player player) {
        setLives(player, Math.max(0, getLives(player) - 1));
    }
}
