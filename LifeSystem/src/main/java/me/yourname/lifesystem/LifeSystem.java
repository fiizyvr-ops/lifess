package me.yourname.lifesystem;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LifeSystem extends JavaPlugin implements Listener {

    private static final Key HEART_FONT = Key.key("lifesystem", "hearts");

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, TextDisplay> displays = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLives();

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor((sender, command, label, args) -> {

                if (args.length != 3 || !args[0].equalsIgnoreCase("set")) {
                    sender.sendMessage("Usage: /lives set <player> <amount>");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null) {
                    sender.sendMessage("That player is not online.");
                    return true;
                }

                int amount;

                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Enter a valid number.");
                    return true;
                }

                if (amount < 0) {
                    sender.sendMessage("Lives cannot be negative.");
                    return true;
                }

                setLives(target, amount);

                sender.sendMessage(
                        "Set " + target.getName() + "'s lives to " + amount + "."
                );

                target.sendMessage("Your lives have been set to " + amount + ".");

                return true;
            });
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }

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

    // LOAD SAVED LIVES

    private void loadLives() {
        lives.clear();

        if (!getConfig().isConfigurationSection("lives")) {
            return;
        }

        for (String key : getConfig()
                .getConfigurationSection("lives").getKeys(false)) {

            try {
                UUID uuid = UUID.fromString(key);
                lives.put(uuid, getConfig().getInt("lives." + key, 5));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid player UUID: " + key);
            }
        }
    }

    // SAVE LIVES

    private void saveLives() {
        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            getConfig().set(
                    "lives." + entry.getKey(),
                    entry.getValue()
            );
        }

        saveConfig();
    }

    // GET PLAYER LIVES

    private int getLives(Player player) {
        return lives.getOrDefault(player.getUniqueId(), 5);
    }

    // GET HEART TEXTURE

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

    // UPDATE PLAYER DISPLAYS

    private void updatePlayer(Player player) {

        int amount = getLives(player);
        Component heart = createHeart(amount);

        // TAB LIST HEART

        player.playerListName(
                Component.text(player.getName() + "  ").append(heart)
        );

        // FLOATING HEART

        TextDisplay display = displays.get(player.getUniqueId());

        if (display == null || !display.isValid()) {

            display = player.getWorld().spawn(
                    player.getLocation().add(0, 2.2, 0),
                    TextDisplay.class
            );

            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setSilent(true);

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);

            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            display.setSeeThrough(true);
            display.setShadowed(false);

            boolean attached = player.addPassenger(display);

            if (!attached) {
                display.remove();
                getLogger().warning(
                        "Could not attach heart display to " + player.getName()
                );
                return;
            }

            displays.put(player.getUniqueId(), display);
        }

        display.text(heart);
    }

    // UPDATE EVERY PLAYER

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    // SET LIVES

    public void setLives(Player player, int amount) {

        lives.put(player.getUniqueId(), Math.max(0, amount));

        saveLives();
        updateAllPlayers();
    }

    // ADD LIFE

    public void addLife(Player player) {
        setLives(player, getLives(player) + 1);
    }

    // REMOVE LIFE

    public void removeLife(Player player) {
        setLives(player, Math.max(0, getLives(player) - 1));
    }

    // PLAYER JOINS

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> updatePlayer(event.getPlayer()),
                2L
        );
    }

    // PLAYER QUITS

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        TextDisplay display = displays.remove(uuid);

        if (display != null && display.isValid()) {
            display.remove();
        }

        saveLives();
    }
}
