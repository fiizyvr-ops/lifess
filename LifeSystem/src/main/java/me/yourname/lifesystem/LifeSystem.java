
package me.yourname.lifesystem;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LifeSystem extends JavaPlugin {

    private static final Key HEART_FONT =
            Key.key("lifesystem", "hearts");

    private final Map<UUID, Integer> lives = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLives();

        Bukkit.getScheduler().runTaskTimer(
                this,
                this::updateAllPlayers,
                1L,
                20L
        );

        getLogger().info("LifeSystem enabled!");
    }

    @Override
    public void onDisable() {
        saveLives();
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

        // Tab-list heart
        player.playerListName(
                Component.text(player.getName() + "  ")
                        .append(heart)
        );

        // Below-name scoreboard
        Scoreboard board = player.getScoreboard();

        Objective objective = board.getObjective("lifesystem");

        if (objective == null) {
            objective = board.registerNewObjective(
                    "lifesystem",
                    Criteria.DUMMY,
                    Component.text("Lives")
            );
        }

        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        objective.getScore(player.getName()).setScore(amount);
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
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
