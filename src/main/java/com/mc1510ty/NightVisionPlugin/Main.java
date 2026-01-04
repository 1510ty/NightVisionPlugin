package com.mc1510ty.NightVisionPlugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private final Set<UUID> nightVisionPlayers = new HashSet<>();

    @Override
    public void onEnable() {

        // --- 1. データの読み込み ---
        saveDefaultConfig(); // config.ymlがない場合に作成
        List<String> savedUuids = getConfig().getStringList("players");
        for (String s : savedUuids) {
            nightVisionPlayers.add(UUID.fromString(s));
        }

        // --- 2. イベント登録 ---
        getServer().getPluginManager().registerEvents(this, this);

        LiteralArgumentBuilder<CommandSourceStack> nvcommand = Commands.literal("nv")
                .then(Commands.literal("on")
                        .executes(context -> {
                            Player player = context.getSource().getExecutor() instanceof Player p ? p : null;
                            if (player != null) {
                                nightVisionPlayers.add(player.getUniqueId());
                                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                                player.sendMessage("暗視をONに設定しました");
                            }
                            return 1;

                        }
                ))
                .then(Commands.literal("off")
                        .executes(context -> {
                            Player player = context.getSource().getExecutor() instanceof Player p ? p : null;
                            if (player != null) {
                                nightVisionPlayers.remove(player.getUniqueId());
                                player.removePotionEffect(PotionEffectType.NIGHT_VISION); // 即座に消す
                                player.sendMessage("暗視をOFFに設定しました");
                            }
                            return 1;

                    }
                ));

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(nvcommand.build());
        });


        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : nightVisionPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    // プレイヤーがオンラインの場合のみ処理
                    if (player != null && player.isOnline()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 0秒後から開始し、20ticks(1秒)ごとに実行
    }

    @Override
    public void onDisable() {
        // --- 5. データの保存 ---
        List<String> toSave = nightVisionPlayers.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        getConfig().set("players", toSave);
        saveConfig();
    }

    // --- 6. 初参加時の処理 ---
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            nightVisionPlayers.add(player.getUniqueId());
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
            player.sendMessage("暗視を自動的に有効化しました /nv off で無効化できます");
        }
    }
}
