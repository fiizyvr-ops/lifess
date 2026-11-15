
package me.yourname.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public final class LifeSystem extends JavaPlugin implements Listener {

    private static final int STARTING_LIVES = 5;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor((sender, command, label, args) -> {

                // /lives
                if (args.length == 0) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("This command is for players.");
                        return true;
                    }

                    sender.sendMessage(ChatColor.GREEN + "You have "
                            + getLives(player) + " lives.");

                    if (hasCustomLife(player)) {
                        sender.sendMessage(ChatColor.BLACK + "You also have a custom 🖤 life!");
                    }

                    return true;
                }

                if (!sender.hasPermission("lifesystem.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }

                // /lives custom <player>
                if (args.length == 2
                        && args[0].equalsIgnoreCase("custom")) {

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found online.");
                        return true;
                    }

                    setCustomLife(target, true);

                    sender.sendMessage(ChatColor.GREEN
                            + "Gave " + target.getName() + " a custom 🖤 life!");
                    return true;
                }

                // /lives custom remove <player>
                if (args.length == 3
                        && args[0].equalsIgnoreCase("custom")
                        && args[1].equalsIgnoreCase("remove")) {

                    Player target = findPlayer(args[2]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found online.");
                        return true;
                    }

                    setCustomLife(target, false);

                    sender.sendMessage(ChatColor.GREEN
                            + "Removed " + target.getName() + "'s custom life.");
                    return true;
                }

                // /lives set random
                if (args.length == 2
                        && args[0].equalsIgnoreCase("set")
                        && args[1].equalsIgnoreCase("random")) {

                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ChatColor.RED
                                + "Console must use /lives set <player> random.");
                        return true;
                    }

                    int amount = ThreadLocalRandom.current().nextInt(1, 6);

                    setLives(player, amount);
                    updateGameMode(player);

                    sender.sendMessage(ChatColor.GREEN
                            + "Your lives were randomly set to " + amount + ".");
                    return true;
                }

                // /lives set <player> random
                if (args.length == 3
                        && args[0].equalsIgnoreCase("set")
                        && args[2].equalsIgnoreCase("random")) {

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found online.");
                        return true;
                    }

                    int amount = ThreadLocalRandom.current().nextInt(1, 6);

                    setLives(target, amount);
                    updateGameMode(target);

                    sender.sendMessage(ChatColor.GREEN + "Set "
                            + target.getName() + "'s lives randomly to " + amount + ".");
                    return true;
                }

                // /lives set <player> <amount>
                if (args.length == 3
                        && args[0].equalsIgnoreCase("set")) {

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found online.");
                        return true;
                    }

                    try {
                        int amount = Integer.parseInt(args[2]);

                        if (amount < 0) {
                            sender.sendMessage(ChatColor.RED
                                    + "Lives cannot be negative.");
                            return true;
                        }

                        setLives(target, amount);
                        updateGameMode(target);

                        sender.sendMessage(ChatColor.GREEN + "Set "
                                + target.getName() + "'s lives to " + amount + ".");

                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED
                                + "Enter a valid number or random.");
                    }

                    return true;
                }

                // /lives reset <player>
                if (args.length == 2
                        && args[0].equalsIgnoreCase("reset")) {

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found online.");
                        return true;
                    }

                    setLives(target, STARTING_LIVES);
                    updateGameMode(target);

                    sender.sendMessage(ChatColor.GREEN
                            + "Reset " + target.getName() + " to 5 lives.");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Lives commands:");
                sender.sendMessage("/lives");
                sender.sendMessage("/lives set <player> <amount>");
                sender.sendMessage("/lives set <player> random");
                sender.sendMessage("/lives set random");
                sender.sendMessage("/lives reset <player>");
                sender.sendMessage("/lives custom <player>");
                sender.sendMessage("/lives custom remove <player>");

                return true;
            });
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            initializePlayer(player);
        }

        getLogger().info("LifeSystem enabled!");
    }

    // Set starting lives when a player joins for the first time
    private void initializePlayer(Player player) {
        String path = "players." + player.getUniqueId();

        if (!getConfig().contains(path)) {
            getConfig().set(path, STARTING_LIVES);
            saveConfig();
        }

        updateDisplay(player);
        updateGameMode(player);
    }

    // Get player's lives
    private int getLives(Player player) {
        return getConfig().getInt(
                "players." + player.getUniqueId(),
                STARTING_LIVES
        );
    }

    // Set player's lives
    private void setLives(Player player, int amount) {
        getConfig().set("players." + player.getUniqueId(), amount);
        saveConfig();
        updateDisplay(player);
    }

    // Check whether player has the special black heart
    private boolean hasCustomLife(Player player) {
        return getConfig().getBoolean(
                "customPlayers." + player.getUniqueId(),
                false
        );
    }

    // Give or remove the special black heart
    private void setCustomLife(Player player, boolean enabled) {
        getConfig().set(
                "customPlayers." + player.getUniqueId(),
                enabled
        );

        saveConfig();
        updateDisplay(player);
    }

    // Find players by username or Bukkit-visible nickname
    private Player findPlayer(String input) {
        String search = ChatColor.stripColor(input).trim();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getName().equalsIgnoreCase(search)) {
                return player;
            }

            String displayName = ChatColor.stripColor(
                    player.getDisplayName()
            );

            if (displayName.equalsIgnoreCase(search)) {
                return player;
            }

            String listName = ChatColor.stripColor(
                    player.getPlayerListName()
            );

            if (listName.equalsIgnoreCase(search)) {
                return player;
            }
        }

        return null;
    }

    // Heart based on lives
    private String getHeart(int lives) {
        switch (lives) {
            case 5:
                return "🩵";
            case 4:
                return "💚";
            case 3:
                return "💛";
            case 2:
                return "🧡";
            case 1:
                return "❤️";
            default:
                return "❓";
        }
    }

    // Name color based on lives
    private ChatColor getLifeColor(int lives) {
        switch (lives) {
            case 5:
                return ChatColor.AQUA;
            case 4:
                return ChatColor.GREEN;
            case 3:
                return ChatColor.YELLOW;
            case 2:
                return ChatColor.GOLD;
            case 1:
                return ChatColor.RED;
            default:
                return ChatColor.LIGHT_PURPLE;
        }
    }

    // Update tab list
    private void updateDisplay(Player player) {
        int lives = getLives(player);

        ChatColor color = getLifeColor(lives);

        String display = color
                + player.getName()
                + " "
                + getHeart(lives);

        // Add custom black heart alongside normal life hearts
        if (hasCustomLife(player)) {
            display += " " + ChatColor.BLACK + "🖤";
        }

        player.setPlayerListName(display);
    }

    // Update spectator/survival mode
    private void updateGameMode(Player player) {
        if (getLives(player) == 0) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        initializePlayer(event.getPlayer());
    }

    // Remove one life after death
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        int remaining = Math.max(0, getLives(player) - 1);

        setLives(player, remaining);

        if (remaining == 0) {
            Bukkit.broadcastMessage(ChatColor.RED
                    + player.getName() + " has run out of lives!");
        }
    }

    // Put players with zero lives into spectator after respawning
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (getLives(player) == 0) {
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline() && getLives(player) == 0) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            });
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
