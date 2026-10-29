package com.abcd.fireworks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Color;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FireworksPlugin extends JavaPlugin {
    private static final String USAGE = "§c使い方: /fireworks <start|stop> [ワールド名] [now]";

    // ワールドごとの打ち上げ状態
    private final Map<UUID, WorldShow> shows = new HashMap<>();

    private static class WorldShow {
        private final UUID worldId;
        private BukkitTask task;
        private int stage = 0;
        private long stageStartTick = 0;

        WorldShow(UUID worldId) {
            this.worldId = worldId;
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("FireworksPlugin enabled!");
        var fireworksCommand = getCommand("fireworks");
        fireworksCommand.setExecutor((sender, command, label, args) -> {
            if (args.length == 0 || args.length > 3) {
                sender.sendMessage(USAGE);
                return true;
            }
            boolean start = args[0].equalsIgnoreCase("start");
            boolean stop = args[0].equalsIgnoreCase("stop");
            if (!start && !stop) {
                sender.sendMessage(USAGE);
                return true;
            }
            boolean now = false;
            String worldName = null;
            for (int i = 1; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("now") && !now) {
                    now = true;
                } else if (worldName == null) {
                    worldName = args[i];
                } else {
                    sender.sendMessage(USAGE);
                    return true;
                }
            }
            World world = resolveWorld(sender, worldName);
            if (world == null) {
                return true;
            }
            if (start) {
                startShow(sender, world, now);
            } else {
                stopShow(sender, world, now);
            }
            return true;
        });
        fireworksCommand.setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) {
                return filterPrefix(List.of("start", "stop"), args[0]);
            }
            if (args.length >= 2 && args.length <= 3
                    && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop"))) {
                boolean hasNow = false;
                boolean hasWorld = false;
                for (int i = 1; i < args.length - 1; i++) {
                    if (args[i].equalsIgnoreCase("now")) {
                        hasNow = true;
                    } else {
                        hasWorld = true;
                    }
                }
                List<String> candidates = new ArrayList<>();
                if (!hasWorld) {
                    Bukkit.getWorlds().forEach(w -> candidates.add(w.getName()));
                }
                if (!hasNow) {
                    candidates.add("now");
                }
                return filterPrefix(candidates, args[args.length - 1]);
            }
            return List.of();
        });
    }

    private static List<String> filterPrefix(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private World resolveWorld(CommandSender sender, String worldName) {
        if (worldName == null) {
            if (sender instanceof Player player) {
                return player.getWorld();
            }
            sender.sendMessage("§cコンソールからはワールド名を指定してください。");
            sender.sendMessage(USAGE);
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§cワールドが見つかりません: " + worldName);
        }
        return world;
    }

    private void startShow(CommandSender sender, World world, boolean now) {
        if (now) {
            setWorldTime(sender, world, 18000, "夜（minecraft:midnight）");
        }
        if (shows.containsKey(world.getUID())) {
            sender.sendMessage("§e[" + world.getName() + "] すでに打ち上げ中です。");
            return;
        }
        WorldShow show = new WorldShow(world.getUID());
        shows.put(world.getUID(), show);
        sender.sendMessage("§a[" + world.getName() + "] 花火の打ち上げを開始しました。パターン演出モード");
        scheduleNextFirework(show);
    }

    private void stopShow(CommandSender sender, World world, boolean now) {
        if (now) {
            setWorldTime(sender, world, 6000, "昼（minecraft:noon）");
        }
        WorldShow show = shows.remove(world.getUID());
        if (show != null) {
            cancelShow(show);
            sender.sendMessage("§e[" + world.getName() + "] 花火の打ち上げを停止しました。");
        } else {
            sender.sendMessage("§c[" + world.getName() + "] 打ち上げは開始されていません。");
        }
    }

    private void cancelShow(WorldShow show) {
        if (show.task != null) {
            show.task.cancel();
            show.task = null;
        }
    }

    private void setWorldTime(CommandSender sender, World world, long time, String label) {
        try {
            world.setTime(time);
            sender.sendMessage("§e[" + world.getName() + "] 時刻を" + label + "に設定しました。");
        } catch (IllegalArgumentException e) {
            // ネザー・エンドなどワールドクロックを持たないワールド
            sender.sendMessage("§c[" + world.getName() + "] このワールドは時刻を変更できません。");
        }
    }

    private boolean isActive(WorldShow show) {
        return shows.get(show.worldId) == show;
    }

    // 対象ワールドがアンロードされていたら停止し null を返す
    private World getActiveWorld(WorldShow show) {
        if (!isActive(show)) return null;
        World world = Bukkit.getWorld(show.worldId);
        if (world == null) {
            shows.remove(show.worldId);
            cancelShow(show);
        }
        return world;
    }

    private void launchAtArmorStands(World world, java.util.function.BiConsumer<World, org.bukkit.entity.ArmorStand> launcher) {
        long time = world.getTime();
        if (time >= 13000 && time <= 23000) {
            world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class).stream()
                .filter(as -> as.getCustomName() != null && as.getCustomName().startsWith("花火"))
                .forEach(as -> launcher.accept(world, as));
        }
    }

    // 花火打ち上げをランダム間隔で再スケジューリング
    private void scheduleNextFirework(WorldShow show) {
        show.stage = 0;
        show.stageStartTick = Bukkit.getServer().getCurrentTick();
        scheduleNextFireworkStage(show);
    }

    private void scheduleNextFireworkStage(WorldShow show) {
        if (!isActive(show)) return;
        long elapsed = Bukkit.getServer().getCurrentTick() - show.stageStartTick;
        int delay;
        if (show.stage == 0) { // 30秒間 2~3秒
            if (elapsed < 600) {
                delay = 40 + (int)(Math.random() * 21); // 40~60tick
                scheduleFireworkAndNext(show, delay);
            } else {
                show.stage = 1;
                scheduleNextFireworkStage(show);
            }
        } else if (show.stage == 1) { // 30秒間 1~2秒
            if (elapsed < 1200) {
                delay = 20 + (int)(Math.random() * 21); // 20~40tick
                scheduleFireworkAndNext(show, delay);
            } else {
                show.stage = 2;
                scheduleNextFireworkStage(show);
            }
        } else if (show.stage == 2) { // 30秒間 0.5秒
            if (elapsed < 1800) {
                delay = 10; // 0.5秒
                scheduleFireworkAndNext(show, delay);
            } else {
                show.stage = 3;
                scheduleNextFireworkStage(show);
            }
        } else if (show.stage == 3) { // フィナーレ前アイドル3秒
            show.task = Bukkit.getScheduler().runTaskLater(this, () -> {
                show.stage = 4;
                scheduleNextFireworkStage(show);
            }, 60L);
        } else if (show.stage == 4) { // フィナーレ大花火（ハート型）
            show.task = Bukkit.getScheduler().runTaskLater(this, () -> {
                World world = getActiveWorld(show);
                if (world == null) return;
                launchAtArmorStands(world, this::launchGrateWallFirework);
                show.stage = 5;
                scheduleNextFireworkStage(show);
            }, 0L);
        } else if (show.stage == 5) { // フィナーレ後30秒アイドル→自動再開
            show.task = Bukkit.getScheduler().runTaskLater(this, () -> {
                if (isActive(show)) {
                    // 再開
                    scheduleNextFirework(show);
                }
            }, 600L);
        }
    }

    private void scheduleFireworkAndNext(WorldShow show, int delay) {
        show.task = Bukkit.getScheduler().runTaskLater(this, () -> {
            World world = getActiveWorld(show);
            if (world == null) return;
            launchAtArmorStands(world, this::launchPatternFireworks);
            scheduleNextFireworkStage(show);
        }, delay);
    }

    // フィナーレ用水平1列16発×強さ0,1,2,3の合計64発同時発射
    private void launchGrateWallFirework(org.bukkit.World world, org.bukkit.entity.ArmorStand as) {
    Location baseLoc = as.getLocation().add(0, 10, 0); // 高さ10に水平配置
    // ArmorStand の向き(Yaw)に合わせてローカルXオフセットをワールド座標に回転変換
    float yaw = as.getLocation().getYaw();
    double rad = Math.toRadians(yaw);
    double forwardX = -Math.sin(rad);
    double forwardZ = Math.cos(rad);
    double rightX = Math.cos(rad);
    double rightZ = Math.sin(rad);
        int count = 16;
        double spacing = 4.0;
        for (int power = 0; power <= 3; power++) {
            for (int i = 0; i < count; i++) {
                double x = (i - (count-1)/2.0) * spacing;
        // ローカル座標 (x, 0) をスタンド向きに合わせて回転してワールド座標に変換
        double worldOffsetX = x * rightX + 0.0 * forwardX;
        double worldOffsetZ = x * rightZ + 0.0 * forwardZ;
        Location loc = baseLoc.clone().add(worldOffsetX, 0, worldOffsetZ);
                FireworkEffectParams effect = new FireworkEffectParams();
                effect.type = Type.BALL_LARGE;
                effect.colors = new Color[]{Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.AQUA, Color.PURPLE};
                effect.fadeColors = new Color[]{Color.WHITE};
                effect.flicker = true;
                effect.trail = true;
                effect.power = power;
                createFirework(world, loc, effect);
            }
        }
    }

    private void launchPatternFireworks(org.bukkit.World world, org.bukkit.entity.ArmorStand as) {
        Location baseLoc = as.getLocation().add(0, 2, 0);
        int pattern = (int)(Math.random() * 3); // 0:左右順番, 1:クロス, 2:円形
        int alignType = 1 + (int)(Math.random() * 4); // 1~4: 1,4,9,16発
        int count = getCount(alignType);
        double[] offsets = getOffsets(alignType);
        FireworkEffectParams effect = new FireworkEffectParams();
        // ArmorStand の向き(Yaw)に合わせてローカル座標を回転させる準備
        float yaw = as.getLocation().getYaw();
        double rad = Math.toRadians(yaw);
        // Bukkit の yaw に対する前方ベクトルと右方向ベクトル
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);
        for (int i = 0; i < count; i++) {
            Location loc = baseLoc.clone();
            double localX = 0.0; // スタンド基準の右方向成分
            double localZ = 0.0; // スタンド基準の前方向成分
            if (pattern == 0) {
                // 左右から順番（type0）: ローカル X 軸にオフセット
                localX = offsets[i];
            } else if (pattern == 1) {
                // クロス（type1）: ローカル X/Z 軸に斜めオフセット
                double cross = offsets[i];
                localX = cross;
                localZ = cross * 0.5;
                effect.randomColor();
            } else if (pattern == 2) {
                // 円形（type2）: ローカル座標系で円周上に配置
                double r = 6.0;
                double angle = 2 * Math.PI * i / count;
                localX = r * Math.cos(angle);
                localZ = r * Math.sin(angle);
                effect.randomColor();
                effect.randomPower();
            }
            // ローカル座標 (localX, localZ) を ArmorStand の向きに合わせてワールド座標へ回転変換
            double worldOffsetX = localX * rightX + localZ * forwardX;
            double worldOffsetZ = localX * rightZ + localZ * forwardZ;
            loc.add(worldOffsetX, 0, worldOffsetZ);
            createFirework(world, loc, effect);
        }
    }

    private int getCount(int alignType) {
        switch (alignType) {
            case 1: return 1;
            case 2: return 4;
            case 3: return 9;
            case 4: return 16;
            default: return 1;
        }
    }

    private double[] getOffsets(int alignType) {
        switch (alignType) {
            case 1: return new double[]{0};
            case 2: return new double[]{9,3,-3,-9};
            case 3: return new double[]{20,15,10,5,0,-5,-10,-15,-20};
            case 4: return new double[]{30,26,22,18,14,10,6,2,-2,-6,-10,-14,-18,-22,-26,-30};
            default: return new double[]{0};
        }
    }

    class FireworkEffectParams {
        private FireworkEffect.Type type;
        private Color[] colors;
        private Color[] fadeColors;
        private boolean flicker;
        private boolean trail;
        private int power;

        public FireworkEffectParams() {
            FireworkEffect.Type[] types = {
                Type.BALL, Type.BALL_LARGE, Type.BURST, Type.CREEPER, Type.STAR
            };
            this.type = types[(int)(Math.random() * types.length)];
            randomColor();
            this.flicker = Math.random() < 0.5;
            this.trail = Math.random() < 0.5;
            randomPower();
        }

        public void randomColor() {
            Color[] palette = {
                Color.RED, Color.YELLOW, Color.ORANGE, Color.AQUA, Color.BLUE,
                Color.FUCHSIA, Color.GREEN, Color.LIME, Color.MAROON, Color.NAVY,
                Color.OLIVE, Color.PURPLE, Color.SILVER, Color.TEAL, Color.WHITE
            };
            // メインカラー2色
            int idx1 = (int)(Math.random() * palette.length);
            int idx2 = (int)(Math.random() * palette.length);
            while (idx2 == idx1) idx2 = (int)(Math.random() * palette.length);
            this.colors = new Color[]{palette[idx1], palette[idx2]};
            // 残り色2色
            int idx3 = (int)(Math.random() * palette.length);
            int idx4 = (int)(Math.random() * palette.length);
            while (idx4 == idx3) idx4 = (int)(Math.random() * palette.length);
            this.fadeColors = new Color[]{palette[idx3], palette[idx4]};
        }

        public void randomPower() {
            this.power = 1 + (int)(Math.random() * 3); // 1~3
        }

        public FireworkEffect.Type getType() {
            return type;
        }

        public Color[] getColors() {
            return colors;
        }

        public Color[] getFadeColors() {
            return fadeColors;
        }

        public boolean isFlicker() {
            return flicker;
        }

        public boolean isTrail() {
            return trail;
        }

        public int getPower() {
            return power;
        }
    }

    private void createFirework(org.bukkit.World world, Location loc, FireworkEffectParams effect) {
        Firework firework = world.spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(effect.getType())
                .withColor(effect.getColors())
                .withFade(effect.getFadeColors())
                .flicker(effect.isFlicker())
                .trail(effect.isTrail())
                .build());
        meta.setPower(effect.getPower());
        firework.setFireworkMeta(meta);
    }

    @Override
    public void onDisable() {
        shows.values().forEach(this::cancelShow);
        shows.clear();
        getLogger().info("FireworksPlugin disabled!");
    }
}
