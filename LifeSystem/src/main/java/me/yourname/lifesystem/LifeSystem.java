
package me.yourname.lifesystem;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

import java.util.concurrent.ThreadLocalRandom;

public final class LifeSystem extends JavaPlugin implements Listener {

    private static final int STARTING_LIVES = 5;
    private Objective livesObjective;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDisplay();

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor((sender, command, label, args) -> {

                if (args.length == 0) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("This command is for players.");
                        return true;
                    }

                    sender.sendMessage(ChatColor.GREEN + "You have "
                            + getLives(player) + " lives.");
                    return true;
                }

                if (!sender.hasPermission("lifesystem.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
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
                    restoreSurvivalIfAlive(player);

                    sender.sendMessage(ChatColor.GREEN
                            + "Your lives were randomly set to " + amount + ".");
                    return true;
                }

                // /lives set <player> random
                if (args.length == 3
                        && args[0].equalsIgnoreCase("set")
                        && args[2].equalsIgnoreCase("random")) {

                    Player target = Bukkit.getPlayerExact(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "That player must be online.");
                        return true;
                    }

                    int amount = ThreadLocalRandom.current().nextInt(1, 6);
                    setLives(target, amount);
                    restoreSurvivalIfAlive(target);

                    sender.sendMessage(ChatColor.GREEN + "Set "
                            + target.getName() + "'s lives randomly to " + amount + ".");
                    return true;
                }

                // /lives set <player> <amount>
                if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                    Player target = Bukkit.getPlayerExact(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "That player must be online.");
                        return true;
                    }

                    try {
                        int amount = Integer.parseInt(args[2]);

                        if (amount < 0) {
                            sender.sendMessage(ChatColor.RED + "Lives cannot be negative.");
                            return true;
                        }

                        setLives(target, amount);
                        restoreSurvivalIfAlive(target);

                        sender.sendMessage(ChatColor.GREEN + "Set "
                                + target.getName() + "'s lives to " + amount + ".");

                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED
                                + "Enter a valid number or random.");
                    }

                    return true;
                }

                // /lives reset <player>
                if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                    Player target = Bukkit.getPlayerExact(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "That player must be online.");
                        return true;
                    }

                    setLives(target, STARTING_LIVES);
                    restoreSurvivalIfAlive(target);

                    sender.sendMessage(ChatColor.GREEN + "Reset "
                            + target.getName() + " to " + STARTING_LIVES + " lives.");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Usage:");
                sender.sendMessage("/lives");
                sender.sendMessage("/lives set <player> <amount>");
                sender.sendMessage("/lives set <player> random");
                sender.sendMessage("/lives set random");
                sender.sendMessage("/lives reset <player>");

                return true;
            });
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            initializePlayer(player);
        }

        getLogger().info("LifeSystem enabled.");
    }

    private void setupDisplay() {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        livesObjective = scoreboard.getObjective("ls_lives");

        if (livesObjective == null) {
            livesObjective = scoreboard.registerNewObjective(
                    "ls_lives",
                    Criteria.DUMMY,
                    Component.text("Lives"),
                    RenderType.INTEGER
            );
        }

        livesObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    private void initializePlayer(Player player) {
        String path = "players." + player.getUniqueId();

        if (!getConfig().contains(path)) {
            getConfig().set(path, STARTING_LIVES);
            saveConfig();
        }

        updateDisplay(player);

        if (getLives(player) == 0) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private int getLives(Player player) {
        return getConfig().getInt(
                "players." + player.getUniqueId(),
                STARTING_LIVES
        );
    }

    private void setLives(Player player, int amount) {
        getConfig().set("players." + player.getUniqueId(), amount);
        saveConfig();
        updateDisplay(player);
    }

    private NamedTextColor getLifeColor(int lives) {
        if (lives >= 5) {
            return NamedTextColor.AQUA;
        } else if (lives == 4) {
            return NamedTextColor.GREEN;
        } else if (lives == 3) {
            return NamedTextColor.YELLOW;
        } else if (lives == 2) {
            return NamedTextColor.GOLD;
        } else if (lives == 1) {
            return NamedTextColor.RED;
        } else {
            return NamedTextColor.LIGHT_PURPLE;
        }
    }

    private void updateDisplay(Player player) {
        int lives = getLives(player);
        NamedTextColor color = getLifeColor(lives);

        // Colored name in the tab list
        player.setPlayerListName(
                LegacyComponentSerializer.legacySection().serialize(
                        Component.text(
                                player.getName() + " [" + lives + "]",
                                color
                        )
                )
        );

        // Colored number below the player's name
        var score = livesObjective.getScoreFor(player);
        score.setScore(lives);
        score.numberFormat(NumberFormat.styled(Style.style(color)));
    }

    private void restoreSurvivalIfAlive(Player player) {
        if (getLives(player) > 0
                && player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        initializePlayer(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        int remaining = Math.max(0, getLives(player) - 1);
        setLives(player, remaining);

        if (remaining == 0) {
            Bukkit.broadcastMessage(ChatColor.RED + player.getName()
                    + " has run out of lives and is now eliminated!");

        }
    }

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
