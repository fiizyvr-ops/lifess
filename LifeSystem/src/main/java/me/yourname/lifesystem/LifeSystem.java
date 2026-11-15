
package me.yourname.lifesystem;

import net.kyori.adventure.text.Component;
import io.papermc.paper.scoreboard.numbers.NumberFormat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
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

public class LifeSystem extends JavaPlugin implements Listener, CommandExecutor {

    private static final int STARTING_LIVES = 5;
    private static final String OBJECTIVE_NAME = "lifesystem";

    private Objective underNameObjective;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("lives") != null) {
            getCommand("lives").setExecutor(this);
        } else {
            getLogger().severe("The lives command is missing from plugin.yml!");
        }

        setupUnderNameDisplay();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hasCustomLife(player)
                    && !getConfig().contains("players." + player.getUniqueId())) {
                setNormalLives(player, STARTING_LIVES);
            }

            applyPlayerMode(player);
            updateDisplay(player);
        }

        getLogger().info("LifeSystem has been enabled!");
    }

    private void setupUnderNameDisplay() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        underNameObjective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (underNameObjective == null) {
            underNameObjective = scoreboard.registerNewObjective(
                    OBJECTIVE_NAME,
                    Criteria.DUMMY,
                    Component.text("")
            );
        }

        underNameObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
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
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            if (hasCustomLife(player)) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Your life: 🖤");
            } else {
                player.sendMessage(ChatColor.GREEN + "Your lives: "
                        + getHeart(player) + ChatColor.GRAY
                        + " (" + getLives(player) + ")");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("random")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this form.");
                    return true;
                }

                int randomLives = ThreadLocalRandom.current().nextInt(1, 6);
                setNormalLives(player, randomLives);

                player.sendMessage(ChatColor.GREEN + "Your lives were randomly set to "
                        + randomLives + " " + getHeart(player));
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
                sender.sendMessage(ChatColor.RED
                        + "That player must be online. Check their username or nickname.");
                return true;
            }

            if (args[2].equalsIgnoreCase("random")) {
                int randomLives = ThreadLocalRandom.current().nextInt(1, 6);
                setNormalLives(target, randomLives);

                sender.sendMessage(ChatColor.GREEN + target.getName()
                        + "'s lives were randomly set to " + randomLives + ".");
                target.sendMessage(ChatColor.GREEN
                        + "Your lives were randomly set to "
                        + randomLives + " " + getHeart(target));
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);

                if (amount < 0) {
                    sender.sendMessage(ChatColor.RED + "Lives cannot be negative.");
                    return true;
                }

                setNormalLives(target, amount);

                sender.sendMessage(ChatColor.GREEN + "Set "
                        + target.getName() + "'s lives to " + amount + ".");
                target.sendMessage(ChatColor.GREEN
                        + "Your lives were set to " + amount + " "
                        + getHeart(target));

            } catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED
                        + "Enter a valid number or use random.");
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

            OfflinePlayer target = getOfflineTarget(args[1]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            setNormalLives(target.getUniqueId(), STARTING_LIVES);

            sender.sendMessage(ChatColor.GREEN
                    + "Reset that player's lives to 5.");

            if (target.isOnline()) {
                target.getPlayer().sendMessage(ChatColor.GREEN
                        + "Your lives have been reset to 5 🩵");
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("custom")) {
            if (!sender.hasPermission("lifesystem.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }

            if (args.length == 2) {
                Player target = findPlayer(args[1]);

                if (target == null) {
                    sender.sendMessage(ChatColor.RED
                            + "That player must be online.");
                    return true;
                }

                enableCustomLife(target);

                sender.sendMessage(ChatColor.GREEN
                        + "Enabled custom life for " + target.getName() + ".");
                target.sendMessage(ChatColor.LIGHT_PURPLE
                        + "You now have a custom life: 🖤");
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                Player target = findPlayer(args[2]);

                if (target == null) {
                    sender.sendMessage(ChatColor.RED
                            + "That player must be online.");
                    return true;
                }

                setNormalLives(target, STARTING_LIVES);

                sender.sendMessage(ChatColor.GREEN
                        + "Removed custom life from " + target.getName()
                        + " and restored 5 normal lives.");
                target.sendMessage(ChatColor.GREEN
                        + "Your custom life was removed. You now have 5 normal lives 🩵");
                return true;
            }

            sender.sendMessage(ChatColor.RED
                    + "Usage: /lives custom <player>");
            sender.sendMessage(ChatColor.RED
                    + "Usage: /lives custom remove <player>");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "LifeSystem commands:");
        sender.sendMessage(ChatColor.GRAY + "/lives");
        sender.sendMessage(ChatColor.GRAY + "/lives set random");
        sender.sendMessage(ChatColor.GRAY
                + "/lives set <player> <amount|random>");
        sender.sendMessage(ChatColor.GRAY + "/lives reset <player>");
        sender.sendMessage(ChatColor.GRAY + "/lives custom <player>");
        sender.sendMessage(ChatColor.GRAY
                + "/lives custom remove <player>");

        return true;
    }

    private Player findPlayer(String input) {
        String search = ChatColor.stripColor(input).trim();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(search)) {
                return player;
            }

            String displayName =
                    ChatColor.stripColor(player.getDisplayName());

            if (displayName.equalsIgnoreCase(search)) {
                return player;
            }
        }

        return null;
    }

    private OfflinePlayer getOfflineTarget(String input) {
        Player online = findPlayer(input);

        if (online != null) {
            return online;
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null
                    && offline.getName().equalsIgnoreCase(input)) {
                return offline;
            }
        }

        return null;
    }

    private void setNormalLives(Player player, int amount) {
        setNormalLives(player.getUniqueId(), amount);
        applyPlayerMode(player);
        updateDisplay(player);
    }

    private void setNormalLives(UUID uuid, int amount) {
        getConfig().set("customPlayers." + uuid, null);
        getConfig().set("players." + uuid, amount);
        saveConfig();

        Player online = Bukkit.getPlayer(uuid);

        if (online != null) {
            applyPlayerMode(online);
            updateDisplay(online);
        }
    }

    private void enableCustomLife(Player player) {
        UUID uuid = player.getUniqueId();

        getConfig().set("players." + uuid, null);
        getConfig().set("customPlayers." + uuid, true);
        saveConfig();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        updateDisplay(player);
    }

    private boolean hasCustomLife(Player player) {
        return getConfig().getBoolean(
                "customPlayers." + player.getUniqueId(),
                false
        );
    }

    private int getLives(Player player) {
        return getConfig().getInt(
                "players." + player.getUniqueId(),
                STARTING_LIVES
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

        int lives = getLives(player);

        return switch (lives) {
            case 5 -> "🩵";
            case 4 -> "💚";
            case 3 -> "💛";
            case 2 -> "🧡";
            case 1 -> "❤️";
            case 0 -> "💜";
            default -> lives > 5 ? "🩵" : "💜";
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
            case 0 -> ChatColor.LIGHT_PURPLE;
            default -> ChatColor.GRAY;
        };
    }

    private void updateDisplay(Player player) {
        String heart = getHeart(player);
        ChatColor color = getLifeColor(player);

        // Heart beside the player's name in the tab list.
        player.setPlayerListName(
                color + player.getName() + " " + heart
        );

        // Heart below the player's nametag instead of a number.
        if (underNameObjective != null) {
            Score score = underNameObjective.getScore(player);
            score.setScore(1);
            score.numberFormat(NumberFormat.fixed(Component.text(heart)));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (hasCustomLife(player)) {
            return;
        }

        int lives = getLives(player);

        if (lives > 0) {
            int newLives = lives - 1;

            getConfig().set(
                    "players." + player.getUniqueId(),
                    newLives
            );

            saveConfig();

            Bukkit.getScheduler().runTask(this, () -> {
                updateDisplay(player);
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!hasCustomLife(player)
                && !getConfig().contains("players." + player.getUniqueId())) {
            setNormalLives(player, STARTING_LIVES);
        }

        Bukkit.getScheduler().runTask(this, () -> {
            applyPlayerMode(player);
            updateDisplay(player);
        });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            applyPlayerMode(player);
            updateDisplay(player);
        });
    }
}
