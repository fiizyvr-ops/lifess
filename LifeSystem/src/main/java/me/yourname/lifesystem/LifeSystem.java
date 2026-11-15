
package me.yourname.lifesystem;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.NumberFormat;

import java.util.concurrent.ThreadLocalRandom;

public class LifeSystem extends JavaPlugin implements Listener {

    private static final int STARTING_LIVES = 5;
    private static final String OBJECTIVE_NAME = "lifesystem";

    private Objective underNameObjective;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        setupUnderNameDisplay();

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor((sender, command, label, args) -> {

                if (args.length == 0) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Only players can use this command.");
                        return true;
                    }

                    if (hasCustomLife(player)) {
                        player.sendMessage(ChatColor.BLACK + "Your life: 🖤");
                    } else {
                        player.sendMessage(ChatColor.GREEN + "You have "
                                + getLives(player) + " lives. " + getHeart(player));
                    }
                    return true;
                }

                if (args[0].equalsIgnoreCase("set")) {

                    if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage("Only players can use this command.");
                            return true;
                        }

                        int random = ThreadLocalRandom.current().nextInt(1, 6);
                        setNormalLives(player, random);
                        player.sendMessage(ChatColor.GREEN
                                + "Your lives have been randomly set to " + random + ".");
                        return true;
                    }

                    if (!sender.hasPermission("lifesystem.admin")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission.");
                        return true;
                    }

                    if (args.length != 3) {
                        sender.sendMessage(ChatColor.RED
                                + "Usage: /lives set <player> <amount|random>");
                        return true;
                    }

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }

                    if (args[2].equalsIgnoreCase("random")) {
                        int random = ThreadLocalRandom.current().nextInt(1, 6);
                        setNormalLives(target, random);
                        sender.sendMessage(ChatColor.GREEN
                                + "Set " + target.getName() + "'s lives to " + random + ".");
                        return true;
                    }

                    try {
                        int amount = Integer.parseInt(args[2]);

                        if (amount < 0) {
                            sender.sendMessage(ChatColor.RED + "Lives cannot be negative.");
                            return true;
                        }

                        setNormalLives(target, amount);
                        sender.sendMessage(ChatColor.GREEN
                                + "Set " + target.getName() + "'s lives to " + amount + ".");
                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED + "Enter a valid number or random.");
                    }

                    return true;
                }

                if (args[0].equalsIgnoreCase("reset")) {
                    if (!sender.hasPermission("lifesystem.admin")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission.");
                        return true;
                    }

                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /lives reset <player>");
                        return true;
                    }

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }

                    resetPlayer(target);
                    sender.sendMessage(ChatColor.GREEN
                            + "Reset " + target.getName() + " to 5 normal lives.");
                    return true;
                }

                if (args[0].equalsIgnoreCase("custom")) {
                    if (!sender.hasPermission("lifesystem.admin")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission.");
                        return true;
                    }

                    if (args.length == 2 && args[1].equalsIgnoreCase("remove")) {
                        sender.sendMessage(ChatColor.RED
                                + "Usage: /lives custom remove <player>");
                        return true;
                    }

                    if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                        Player target = findPlayer(args[2]);

                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                            return true;
                        }

                        resetPlayer(target);
                        sender.sendMessage(ChatColor.GREEN
                                + "Removed custom life from " + target.getName()
                                + " and restored 5 normal lives.");
                        return true;
                    }

                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED
                                + "Usage: /lives custom <player>");
                        sender.sendMessage(ChatColor.RED
                                + "Usage: /lives custom remove <player>");
                        return true;
                    }

                    Player target = findPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }

                    enableCustomLife(target);
                    sender.sendMessage(ChatColor.GREEN
                            + target.getName() + " now has custom life mode.");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "LifeSystem commands:");
                sender.sendMessage(ChatColor.GRAY + "/lives");
                sender.sendMessage(ChatColor.GRAY + "/lives set random");
                sender.sendMessage(ChatColor.GRAY + "/lives set <player> <amount|random>");
                sender.sendMessage(ChatColor.GRAY + "/lives reset <player>");
                sender.sendMessage(ChatColor.GRAY + "/lives custom <player>");
                sender.sendMessage(ChatColor.GRAY + "/lives custom remove <player>");
                return true;
            });
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerMode(player);
            updateDisplay(player);
        }

        getLogger().info("LifeSystem enabled!");
    }

    private void setupUnderNameDisplay() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        underNameObjective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (underNameObjective == null) {
            underNameObjective = scoreboard.registerNewObjective(
                    OBJECTIVE_NAME,
                    Criteria.DUMMY,
                    Component.empty()
            );
        }

        underNameObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    private void setNormalLives(Player player, int amount) {
        getConfig().set("customPlayers." + player.getUniqueId(), false);
        getConfig().set("players." + player.getUniqueId(), amount);
        saveConfig();

        applyPlayerMode(player);
        updateDisplay(player);
    }

    private void resetPlayer(Player player) {
        setNormalLives(player, STARTING_LIVES);
    }

    private void enableCustomLife(Player player) {
        getConfig().set("customPlayers." + player.getUniqueId(), true);
        getConfig().set("players." + player.getUniqueId(), null);
        saveConfig();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        updateDisplay(player);
    }

    private boolean hasCustomLife(Player player) {
        return getConfig().getBoolean(
                "customPlayers." + player.getUniqueId(), false
        );
    }

    private int getLives(Player player) {
        if (hasCustomLife(player)) {
            return 0;
        }

        return getConfig().getInt(
                "players." + player.getUniqueId(), STARTING_LIVES
        );
    }

    private void applyPlayerMode(Player player) {
        if (hasCustomLife(player)) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            return;
        }

        if (getLives(player) <= 0) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private String getHeart(Player player) {
        if (hasCustomLife(player)) {
            return "🖤";
        }

        return switch (getLives(player)) {
            case 5 -> "🩵";
            case 4 -> "💚";
            case 3 -> "💛";
            case 2 -> "🧡";
            case 1 -> "❤️";
            default -> "💜";
        };
    }

    private ChatColor getLifeColor(Player player) {
        if (hasCustomLife(player)) {
            return ChatColor.BLACK;
        }

        return switch (getLives(player)) {
            case 5 -> ChatColor.AQUA;
            case 4 -> ChatColor.GREEN;
            case 3 -> ChatColor.YELLOW;
            case 2 -> ChatColor.GOLD;
            case 1 -> ChatColor.RED;
            default -> ChatColor.LIGHT_PURPLE;
        };
    }

    private void updateDisplay(Player player) {
        String heart = getHeart(player);
        ChatColor color = getLifeColor(player);

        // Tab list: player name and one colored heart.
        player.setPlayerListName(
                color + player.getName() + " " + heart
        );

        // Under-name display: one heart, without a number.
        if (underNameObjective != null) {
            Score score = underNameObjective.getScore(player);
            score.setScore(1);
            score.setNumberFormat(
                    NumberFormat.fixed(Component.text(heart))
            );
        }
    }

    private Player findPlayer(String input) {
        String search = ChatColor.stripColor(input).trim();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(search)) {
                return player;
            }

            String displayName = ChatColor.stripColor(player.getDisplayName());
            if (displayName.equalsIgnoreCase(search)) {
                return player;
            }

            String listName = ChatColor.stripColor(player.getPlayerListName());
            if (listName.equalsIgnoreCase(search)) {
                return player;
            }
        }

        return null;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Custom-life players are not part of normal LifeSystem lives.
        if (hasCustomLife(player)) {
            return;
        }

        int lives = getLives(player);

        if (lives > 0) {
            lives--;
            getConfig().set("players." + player.getUniqueId(), lives);
            saveConfig();
        }

        Bukkit.getScheduler().runTask(this, () -> updateDisplay(player));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            applyPlayerMode(player);
            updateDisplay(player);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            applyPlayerMode(player);
            updateDisplay(player);
        });
    }
}
