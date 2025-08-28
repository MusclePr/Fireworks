package com.abcd.fireworks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Color;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class FireworksPlugin extends JavaPlugin {
    private BukkitTask fireworkTask;
    private boolean running = false;
    private int fireworkStage = 0;
    private long stageStartTick = 0;

    @Override
    public void onEnable() {
        getLogger().info("FireworksPlugin enabled!");
        getCommand("fireworks").setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                sender.sendMessage("§c使い方: /fireworks <start|stop>");
                return true;
            }
            if (args[0].equalsIgnoreCase("start")) {
                if (running) {
                    sender.sendMessage("§eすでに打ち上げ中です。");
                    return true;
                }
                sender.sendMessage("§a花火の打ち上げを開始しました。パターン演出モード");
                running = true;
                scheduleNextFirework();
                return true;
            } else if (args[0].equalsIgnoreCase("stop")) {
                if (running) {
                    running = false;
                    if (fireworkTask != null) {
                        fireworkTask.cancel();
                        fireworkTask = null;
                    }
                    sender.sendMessage("§e花火の打ち上げを停止しました。");
                } else {
                    sender.sendMessage("§c打ち上げは開始されていません。");
                }
                return true;
            } else {
                sender.sendMessage("§c使い方: /fireworks <start|stop>");
                return true;
            }
        });
    }

    // 花火打ち上げをランダム間隔で再スケジューリング
    private void scheduleNextFirework() {
        fireworkStage = 0;
        stageStartTick = Bukkit.getServer().getCurrentTick();
        scheduleNextFireworkStage();
    }

    private void scheduleNextFireworkStage() {
        if (!running) return;
        long elapsed = Bukkit.getServer().getCurrentTick() - stageStartTick;
        int delay;
        if (fireworkStage == 0) { // 30秒間 2~3秒
            if (elapsed < 600) {
                delay = 40 + (int)(Math.random() * 21); // 40~60tick
                scheduleFireworkAndNext(delay, 0);
            } else {
                fireworkStage = 1;
                scheduleNextFireworkStage();
            }
        } else if (fireworkStage == 1) { // 30秒間 1~2秒
            if (elapsed < 1200) {
                delay = 20 + (int)(Math.random() * 21); // 20~40tick
                scheduleFireworkAndNext(delay, 1);
            } else {
                fireworkStage = 2;
                scheduleNextFireworkStage();
            }
        } else if (fireworkStage == 2) { // 30秒間 0.5秒
            if (elapsed < 1800) {
                delay = 10; // 0.5秒
                scheduleFireworkAndNext(delay, 2);
            } else {
                fireworkStage = 3;
                scheduleNextFireworkStage();
            }
        } else if (fireworkStage == 3) { // フィナーレ前アイドル3秒
            fireworkTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                fireworkStage = 4;
                scheduleNextFireworkStage();
            }, 60L);
        } else if (fireworkStage == 4) { // フィナーレ大花火（ハート型）
            fireworkTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getWorlds().forEach(world -> {
                    long time = world.getTime();
                    if (time >= 13000 && time <= 23000) {
                        world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class).stream()
                            .filter(as -> as.getCustomName() != null && as.getCustomName().startsWith("花火"))
                            .forEach(as -> launchGrateWallFirework(world, as));
                    }
                });
                fireworkStage = 5;
                scheduleNextFireworkStage();
            }, 0L);
        } else if (fireworkStage == 5) { // フィナーレ後30秒アイドル→自動再開
            fireworkTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                if (running) {
                    // 再開
                    scheduleNextFirework();
                }
            }, 600L);
        }
    }

    private void scheduleFireworkAndNext(int delay, int stage) {
        fireworkTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getWorlds().forEach(world -> {
                long time = world.getTime();
                if (time >= 13000 && time <= 23000) {
                    world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class).stream()
                        .filter(as -> as.getCustomName() != null && as.getCustomName().startsWith("花火"))
                        .forEach(as -> launchPatternFireworks(world, as));
                }
            });
            scheduleNextFireworkStage();
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
        getLogger().info("FireworksPlugin disabled!");
    }
}
