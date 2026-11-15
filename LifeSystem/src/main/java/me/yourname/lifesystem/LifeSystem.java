
package me.yourname.lifesystem;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LifeSystem extends JavaPlugin
        implements Listener, CommandExecutor {

    private static final int STARTING_LIVES = 5;

    private static final String OBJECTIVE_NAME = "lifesystem";

    // Custom resource pack font
    private static final Key HEART_FONT =
            Key.key("lifesystem", "hearts");

    private Objective underNameObjective;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        setupUnderNameDisplay();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ensurePlayerData(player);
            applyPlayerMode(player);
            updateDisplay(player);
        }

        getLogger().info("LifeSystem enabled!");
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    // SCOREBOARD SETUP

    private void setupUnderNameDisplay() {

        Scoreboard scoreboard =
                Bukkit.getScoreboardManager().getMainScoreboard();

        underNameObjective =
                scoreboard.getObjective(OBJECTIVE_NAME);

        if (underNameObjective == null) {

            underNameObjective =
                    scoreboard.registerNewObjective(
                            OBJECTIVE_NAME,
                            Criteria.DUMMY,
                            Component.empty()
                    );
        }

        underNameObjective.setDisplaySlot(
                DisplaySlot.BELOW_NAME
        );
    }

    // PLAYER DATA

    private String playerPath(UUID uuid) {
        return "players." + uuid;
    }

    private String customPath(UUID uuid) {
        return "customPlayers." + uuid;
    }

    private void ensurePlayerData(Player player) {

        String path = playerPath(player.getUniqueId());

        if (!getConfig().contains(path)) {
            getConfig().set(path, STARTING_LIVES);
            saveConfig();
        }
    }

    private int getLives(Player player) {

        return Math.max(
                0,
                getConfig().getInt(
                        playerPath(player.getUniqueId()),
                        STARTING_LIVES
                )
        );
    }

    private boolean hasCustomLife(Player player) {

        return getConfig().getBoolean(
                customPath(player.getUniqueId()),
                false
        );
    }

    // LIFE MANAGEMENT

    private void setNormalLives(Player player, int lives) {

        int safeLives = Math.max(0, Math.min(5, lives));

        getConfig().set(
                customPath(player.getUniqueId()),
                null
        );

        getConfig().set(
                playerPath(player.getUniqueId()),
                safeLives
        );

        saveConfig();

        applyPlayerMode(player);
        updateDisplay(player);
    }

    private void setCustomLife(Player player) {

        getConfig().set(
                playerPath(player.getUniqueId()),
                null
        );

        getConfig().set(
                customPath(player.getUniqueId()),
                true
        );

        saveConfig();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        updateDisplay(player);
    }

    private void removeCustomLife(Player player) {

        getConfig().set(
                customPath(player.getUniqueId()),
                null
        );

        getConfig().set(
                playerPath(player.getUniqueId()),
                STARTING_LIVES
        );

        saveConfig();

        applyPlayerMode(player);
        updateDisplay(player);
    }

    // CUSTOM HEART FONT

    private Component getCustomHeart(Player player) {

        int character;

        if (hasCustomLife(player)) {

            character = 0xE006;

        } else {

            switch (getLives(player)) {

                case 5:
                    character = 0xE000;
                    break;

                case 4:
                    character = 0xE001;
                    break;

                case 3:
                    character = 0xE002;
                    break;

                case 2:
                    character = 0xE003;
                    break;

                case 1:
                    character = 0xE004;
                    break;

                default:
                    character = 0xE005;
                    break;
            }
        }

        String glyph = new String(
                Character.toChars(character)
        );

        return Component.text(glyph)
                .font(HEART_FONT);
    }

    // UPDATE BOTH DISPLAYS

    private void updateDisplay(Player player) {

        Component heart = getCustomHeart(player);

        // TAB LIST

        player.playerListName(
                Component.text(player.getName() + " ")
                        .append(heart)
        );

        // BELOW PLAYER NAME

        if (underNameObjective != null) {

            Score score =
                    underNameObjective.getScore(player);

            score.setScore(1);

            score.numberFormat(
                    NumberFormat.fixed(heart)
            );
        }
    }

    // PLAYER GAME MODE

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

    // FIND ONLINE PLAYER

    private Player findOnlinePlayer(String name) {

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }

        return null;
    }

    // COMMANDS

    private void sendHelp(CommandSender sender) {

        sender.sendMessage(ChatColor.GOLD + "LifeSystem Commands:");

        sender.sendMessage(ChatColor.GRAY + "/lives");

        sender.sendMessage(
                ChatColor.GRAY +
                "/lives set <player> <0-5|random>"
        );

        sender.sendMessage(
                ChatColor.GRAY +
                "/lives custom <player>"
        );

        sender.sendMessage(
                ChatColor.GRAY +
                "/lives custom remove <player>"
        );

        sender.sendMessage(
                ChatColor.GRAY +
                "/lives set random"
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can check their lives.");
                return true;
            }

            if (hasCustomLife(player)) {

                player.sendMessage(
                        ChatColor.LIGHT_PURPLE +
                        "You have a custom life!"
                );

            } else {

                player.sendMessage(
                        ChatColor.GREEN +
                        "Your lives: " +
                        getLives(player)
                );
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        // SET LIVES

        if (args[0].equalsIgnoreCase("set")) {

            if (args.length == 2 &&
                    args[1].equalsIgnoreCase("random")) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(
                            "Only players can randomize their own lives."
                    );
                    return true;
                }

                int lives =
                        ThreadLocalRandom.current().nextInt(1, 6);

                setNormalLives(player, lives);

                player.sendMessage(
                        ChatColor.GREEN +
                        "Your lives are now " + lives
                );

                return true;
            }

            if (!sender.hasPermission("lifesystem.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            if (args.length != 3) {
                sender.sendMessage(
                        "Usage: /lives set <player> <0-5|random>"
                );
                return true;
            }

            Player target = findOnlinePlayer(args[1]);

            if (target == null) {
                sender.sendMessage("Player must be online.");
                return true;
            }

            int lives;

            if (args[2].equalsIgnoreCase("random")) {

                lives = ThreadLocalRandom.current().nextInt(1, 6);

            } else {

                try {
                    lives = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage("Enter a number from 0 to 5.");
                    return true;
                }

                if (lives < 0 || lives > 5) {
                    sender.sendMessage("Lives must be 0-5.");
                    return true;
                }
            }

            setNormalLives(target, lives);

            sender.sendMessage(
                    ChatColor.GREEN +
                    target.getName() +
                    " now has " + lives + " lives."
            );

            target.sendMessage(
                    ChatColor.GREEN +
                    "Your lives are now " + lives + "."
            );

            return true;
        }

        // CUSTOM LIFE

        if (args[0].equalsIgnoreCase("custom")) {

            if (!sender.hasPermission("lifesystem.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            if (args.length == 2) {

                Player target = findOnlinePlayer(args[1]);

                if (target == null) {
                    sender.sendMessage("Player must be online.");
                    return true;
                }

                setCustomLife(target);

                sender.sendMessage(
                        target.getName() +
                        " now has a custom life."
                );

                return true;
            }

            if (args.length == 3 &&
                    args[1].equalsIgnoreCase("remove")) {

                Player target = findOnlinePlayer(args[2]);

                if (target == null) {
                    sender.sendMessage("Player must be online.");
                    return true;
                }

                removeCustomLife(target);

                sender.sendMessage(
                        "Removed custom life from " +
                        target.getName()
                );

                return true;
            }

            sender.sendMessage(
                    "Usage: /lives custom <player>"
            );

            return true;
        }

        sendHelp(sender);
        return true;
    }

    // DEATH EVENT

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (hasCustomLife(player)) {
            return;
        }

        int remaining = Math.max(
                0,
                getLives(player) - 1
        );

        getConfig().set(
                playerPath(player.getUniqueId()),
                remaining
        );

        saveConfig();

        Bukkit.getScheduler().runTask(this, () -> {

            applyPlayerMode(player);
            updateDisplay(player);

        });
    }

    // JOIN EVENT

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        ensurePlayerData(player);

        Bukkit.getScheduler().runTask(this, () -> {

            applyPlayerMode(player);
            updateDisplay(player);

        });
    }

    // RESPAWN EVENT

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {

            applyPlayerMode(player);
            updateDisplay(player);

        });
    }
}
