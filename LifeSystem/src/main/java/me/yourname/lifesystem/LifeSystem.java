package me.yourname.lifesystem;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
                                + "The console must use /lives set <player> random.");
                        return true;
                    }

                    int amount = ThreadLocalRandom.current().nextInt(1, 6);
                    setLives(player, amount);

                    sender.sendMessage(ChatColor.GREEN
                            + "You now have " + amount + " lives!");
                    return true;
                }

                // /lives set <player> <amount or random>
                if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                    Player target = Bukkit.getPlayerExact(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED
                                + "That player must be online.");
                        return true;
                    }

                    int amount;

                    if (args[2].equalsIgnoreCase("random")) {
                        amount = ThreadLocalRandom.current().nextInt(1, 6);
                    } else {
                        try {
                            amount = Integer.parseInt(args[2]);
                        } catch (NumberFormatException exception) {
                            sender.sendMessage(ChatColor.RED
                                    + "Enter a valid whole number or random.");
                            return true;
                        }
                    }

                    if (amount < 0) {
                        sender.sendMessage(ChatColor.RED
                                + "Lives cannot be negative.");
                        return true;
                    }

                    setLives(target, amount);

                    if (amount > 0) {
                        Bukkit.getBanList(BanList.Type.NAME)
                                .pardon(target.getName());

                        if (target.getGameMode() == GameMode.SPECTATOR) {
                            target.setGameMode(GameMode.SURVIVAL);
                        }
                    }

                    sender.sendMessage(ChatColor.GREEN + "Set "
                            + target.getName() + "'s lives to " + amount + ".");
                    return true;
                }

                // /lives reset <player>
                if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                    Player target = Bukkit.getPlayerExact(args[1]);

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED
                                + "That player must be online.");
                        return true;
                    }

                    setLives(target, STARTING_LIVES);

                    Bukkit.getBanList(BanList.Type.NAME)
                            .pardon(target.getName());

                    target.setGameMode(GameMode.SURVIVAL);

                    sender.sendMessage(ChatColor.GREEN + "Reset "
                            + target.getName() + " to "
                            + STARTING_LIVES + " lives.");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Usage:");
                sender.sendMessage("/lives");
                sender.sendMessage("/lives set random");
                sender.sendMessage("/lives set <player> <amount>");
                sender.sendMessage("/lives set <player> random");
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

    private void updateDisplay(Player player) {
        int lives = getLives(player);

        NamedTextColor color;

        if (lives >= 5) {
            color = NamedTextColor.AQUA;
        } else if (lives == 4) {
            color = NamedTextColor.GREEN;
        } else if (lives == 3) {
            color = NamedTextColor.YELLOW;
        } else if (lives == 2) {
            color = NamedTextColor.GOLD;
        } else if (lives == 1) {
            color = NamedTextColor.RED;
        } else {
            color = NamedTextColor.LIGHT_PURPLE;
        }

        // Tab list name
        player.setPlayerListName(
                Component.text(player.getName() + " [" + lives + "]", color)
        );

        // Below-name number, using the player's entity scoreboard entry
        var score = livesObjective.getScoreFor(player);
        score.setScore(lives);
        score.numberFormat(NumberFormat.styled(Style.style(color)));
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
                    + " has been eliminated after running out of lives.");

            Bukkit.getScheduler().runTask(this, () -> {
                Bukkit.getBanList(BanList.Type.NAME).addBan(
                        player.getName(),
                        "You have run out of lives.",
                        null,
                        "LifeSystem"
                );

                player.kick(Component.text("You have run out of lives."));
            });
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
