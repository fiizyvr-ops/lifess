
package me.yourname.lifesystem;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class LifeSystem extends JavaPlugin implements Listener {

    private static final Key HEART_FONT = Key.key("lifesystem", "hearts");

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Set<UUID> customHearts = new HashSet<>();
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("lives") == null) {
            getLogger().severe("The lives command is missing from plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("lives").setExecutor(this::handleCommand);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
            applyZeroLivesRule(player);
        }

        getLogger().info("LifeSystem enabled!");
    }

    @Override
    public void onDisable() {
        saveData();

        for (TextDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        displays.clear();
    }

    // DATA

    private void loadData() {
        lives.clear();
        customHearts.clear();

        if (getConfig().isConfigurationSection("lives")) {
            for (String key : getConfig().getConfigurationSection("lives").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    lives.put(uuid, Math.max(0, getConfig().getInt("lives." + key, 5)));
                } catch (IllegalArgumentException exception) {
                    getLogger().warning("Invalid UUID in lives data: " + key);
                }
            }
        }

        for (String key : getConfig().getStringList("custom-hearts")) {
            try {
                customHearts.add(UUID.fromString(key));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Invalid UUID in custom-hearts: " + key);
            }
        }
    }

    private void saveData() {
        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            getConfig().set("lives." + entry.getKey(), entry.getValue());
        }

        getConfig().set(
                "custom-hearts",
                customHearts.stream().map(UUID::toString).toList()
        );

        saveConfig();
    }

    private int getLives(Player player) {
        return lives.getOrDefault(player.getUniqueId(), 5);
    }

    // HEART FONT

    private Component createHeart(Player player) {
        String glyph;

        if (customHearts.contains(player.getUniqueId())) {
            glyph = "\uE006";
        } else {
            glyph = switch (getLives(player)) {
                case 5 -> "\uE000";
                case 4 -> "\uE001";
                case 3 -> "\uE002";
                case 2 -> "\uE003";
                case 1 -> "\uE004";
                case 0 -> "\uE005";
                default -> "\uE006";
            };
        }

        return Component.text(glyph).font(HEART_FONT);
    }

    // PLAYER DISPLAY

    private void updatePlayer(Player player) {
        Component heart = createHeart(player);

        player.playerListName(
                Component.text(player.getName() + "  ").append(heart)
        );

        if (player.isDead()) {
            removeDisplay(player);
            return;
        }

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

            display.setTransformation(
                    new Transformation(
                            new Vector3f(0f, 0.8f, 0f),
                            new Quaternionf(),
                            new Vector3f(1f, 1f, 1f),
                            new Quaternionf()
                    )
            );

            if (!player.addPassenger(display)) {
                display.remove();
                getLogger().warning(
                        "Could not attach overhead heart to " + player.getName()
                );
                return;
            }

            player.hideEntity(this, display);

            displays.put(player.getUniqueId(), display);
        }

        display.text(heart);
    }

    private void removeDisplay(Player player) {
        TextDisplay display = displays.remove(player.getUniqueId());

        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    // LIFE MANAGEMENT

    public void setLives(Player player, int amount) {
        int safeAmount = Math.max(0, amount);

        lives.put(player.getUniqueId(), safeAmount);

        saveData();
        updateAllPlayers();

        if (safeAmount == 0 && !player.isDead()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void addLife(Player player) {
        setLives(player, getLives(player) + 1);
    }

    public void removeLife(Player player) {
        setLives(player, Math.max(0, getLives(player) - 1));
    }

    private void applyZeroLivesRule(Player player) {
        if (getLives(player) <= 0 && !player.isDead()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    // COMMANDS

    private boolean handleCommand(
            org.bukkit.command.CommandSender sender,
            org.bukkit.command.Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console: use /lives set <player> <amount>");
                return true;
            }

            sender.sendMessage("You have " + getLives(player) + " lives.");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("lifesystem.admin")) {
                sender.sendMessage("You don't have permission.");
                return true;
            }

            if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console must specify a player.");
                    return true;
                }

                int amount = random.nextInt(5) + 1;
                setLives(player, amount);
                sender.sendMessage("Your lives were randomly set to " + amount + ".");
                return true;
            }

            if (args.length != 3) {
                sender.sendMessage("Usage: /lives set <player> <0-5|random>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage("Player not found or not online.");
                return true;
            }

            int amount;

            if (args[2].equalsIgnoreCase("random")) {
                amount = random.nextInt(5) + 1;
            } else {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage("Enter a number from 0 to 5, or random.");
                    return true;
                }
            }

            if (amount < 0 || amount > 5) {
                sender.sendMessage("Lives must be between 0 and 5.");
                return true;
            }

            setLives(target, amount);
            sender.sendMessage("Set " + target.getName() + "'s lives to " + amount + ".");
            target.sendMessage("Your lives were set to " + amount + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("custom")) {
            if (!sender.hasPermission("lifesystem.admin")) {
                sender.sendMessage("You don't have permission.");
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                Player target = Bukkit.getPlayerExact(args[2]);

                if (target == null) {
                    sender.sendMessage("Player not found or not online.");
                    return true;
                }

                customHearts.remove(target.getUniqueId());
                saveData();
                updateAllPlayers();

                sender.sendMessage("Removed the custom heart from " + target.getName() + ".");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("Usage: /lives custom <player>");
                sender.sendMessage("Or: /lives custom remove <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage("Player not found or not online.");
                return true;
            }

            customHearts.add(target.getUniqueId());
            saveData();
            updateAllPlayers();

            sender.sendMessage("Gave " + target.getName() + " the custom heart.");
            target.sendMessage("You now have the custom heart.");
            return true;
        }

        sender.sendMessage("Commands: /lives, /lives set, /lives custom");
        return true;
    }

    // EVENTS

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        lives.putIfAbsent(player.getUniqueId(), 5);
        saveData();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;

            updatePlayer(player);
            applyZeroLivesRule(player);
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeDisplay(event.getPlayer());
        saveData();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        int previousLives = getLives(player);
        int newLives = Math.max(0, previousLives - 1);

        setLives(player, newLives);
        removeDisplay(player);

        // Announce only when the player loses their final life.
        if (previousLives == 1) {
            Bukkit.broadcastMessage(
                    "§c" + player.getName() + " has lost all their lives!"
            );
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;

            updatePlayer(player);
            applyZeroLivesRule(player);
        }, 2L);
    }
}
