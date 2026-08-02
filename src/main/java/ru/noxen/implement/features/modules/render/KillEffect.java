package ru.noxen.implement.features.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.BooleanSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.implement.events.player.TickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KillEffect extends Module implements QuickImports {

    public static KillEffect getInstance() {
        return ru.noxen.common.util.other.Instance.get(KillEffect.class);
    }

    public final BooleanSetting mobsSetting = new BooleanSetting("Mobs", "Also trigger on regular mob deaths, not just players");

    private final Map<String, DeathData> activeDeaths = new ConcurrentHashMap<>();
    private final Map<String, Long> alreadyHandled = new ConcurrentHashMap<>();

    private static final long EFFECT_DURATION = 3000;

    public KillEffect() {
        super("KillEffect", "KillEffect", ModuleCategory.RENDER);
        setup(mobsSetting);
    }

    @Override
    public void deactivate() {
        activeDeaths.clear();
        alreadyHandled.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player) continue;

            String key = entity.getUuid().toString();
            if (living.getHealth() <= 0) {
                if (!alreadyHandled.containsKey(key)) {
                    boolean isPlayer = entity instanceof PlayerEntity;
                    if (mobsSetting.isValue() || isPlayer) {
                        spawnDeathEffect(entity);
                        alreadyHandled.put(key, System.currentTimeMillis());
                    }
                }
            } else {
                alreadyHandled.remove(key);
            }
        }

        long now = System.currentTimeMillis();
        activeDeaths.entrySet().removeIf(entry -> now - entry.getValue().time > EFFECT_DURATION);
        alreadyHandled.entrySet().removeIf(entry -> now - entry.getValue() > 5000);

        for (DeathData data : activeDeaths.values()) {
            float progress = Math.min(1f, (now - data.time) / (float) EFFECT_DURATION);
            renderCross(data.pos, progress);
        }
    }

    private void spawnDeathEffect(Entity entity) {
        mc.world.playSound(mc.player, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        mc.world.playSound(mc.player, entity.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        activeDeaths.put(entity.getUuid().toString(), new DeathData(System.currentTimeMillis(), entity.getPos()));
    }

    private void renderCross(Vec3d pos, float progress) {
        double cx = pos.x, cy = pos.y + 1.5, cz = pos.z;

        // Falling apart into scattered particles near the end of the effect.
        if (progress > 0.85f) {
            float decay = (progress - 0.85f) / 0.15f;
            int count = (int) (200 * decay);
            for (int i = 0; i < count; i++) {
                double angle = Math.random() * Math.PI * 2;
                double pitch = Math.random() * Math.PI * 2;
                double radius = decay;

                double x = cx + Math.cos(angle) * Math.cos(pitch) * radius;
                double y = cy + Math.sin(pitch) * radius;
                double z = cz + Math.sin(angle) * Math.cos(pitch) * radius;

                double vx = (x - cx) * 0.2;
                double vy = (y - cy) * 0.2 + 0.05;
                double vz = (z - cz) * 0.2;

                mc.world.addParticle(Math.random() < 0.6 ? ParticleTypes.END_ROD : ParticleTypes.GLOW, x, y, z, vx, vy, vz);
            }
            return;
        }

        double dx = cx - mc.player.getX();
        double dz = cz - mc.player.getZ();
        double angleToPlayer = Math.atan2(dz, dx);
        double rightX = Math.cos(angleToPlayer);
        double rightZ = Math.sin(angleToPlayer);

        double armLength = 0.7;
        float brightness = 1f - progress * 0.6f;
        int arm = (int) (35 * brightness);

        // Vertical arm of the cross.
        for (int i = 0; i <= arm; i++) {
            double offset = -armLength + (double) i / arm * armLength * 2;
            double y = cy + offset;
            mc.world.addParticle(ParticleTypes.END_ROD, cx, y, cz, 0, 0, 0);
            if (brightness > 0.5f && Math.random() < 0.2f) {
                mc.world.addParticle(ParticleTypes.GLOW,
                        cx + (Math.random() - 0.5) * 0.08, y + (Math.random() - 0.5) * 0.08, cz + (Math.random() - 0.5) * 0.08,
                        0, 0, 0);
            }
        }

        // Horizontal arm, always facing the camera.
        for (int i = 0; i <= arm; i++) {
            double offset = -armLength + (double) i / arm * armLength * 2;
            double x = cx + rightX * offset;
            double z = cz + rightZ * offset;
            mc.world.addParticle(ParticleTypes.END_ROD, x, cy, z, 0, 0, 0);
            if (brightness > 0.5f && Math.random() < 0.2f) {
                mc.world.addParticle(ParticleTypes.GLOW,
                        x + (Math.random() - 0.5) * 0.08, cy + (Math.random() - 0.5) * 0.08, z + (Math.random() - 0.5) * 0.08,
                        0, 0, 0);
            }
        }

        // Glow at the intersection.
        for (int i = 0; i < 12; i++) {
            mc.world.addParticle(ParticleTypes.GLOW,
                    cx + (Math.random() - 0.5) * 0.25, cy + (Math.random() - 0.5) * 0.25, cz + (Math.random() - 0.5) * 0.25,
                    0, 0, 0);
        }

        // Orbiting sparks around the cross.
        if (brightness > 0.6f) {
            for (int i = 0; i < 10; i++) {
                double angle = Math.toRadians(i * 36 + System.currentTimeMillis() / 12.0);
                double radius = 0.55;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + Math.sin(angle * 2) * 0.2;
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.01, 0);
            }
        }

        // Sparks at the arm tips, only at the start of the effect.
        if (progress < 0.35f) {
            double topY = cy + armLength, bottomY = cy - armLength;
            double rightArmX = cx + rightX * armLength, rightArmZ = cz + rightZ * armLength;
            double leftArmX = cx - rightX * armLength, leftArmZ = cz - rightZ * armLength;

            for (int i = 0; i < 6; i++) {
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, leftArmX, cy, leftArmZ, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08);
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, rightArmX, cy, rightArmZ, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.08);
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, cx, topY, cz, (Math.random() - 0.5) * 0.08, 0.08, (Math.random() - 0.5) * 0.08);
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, cx, bottomY, cz, (Math.random() - 0.5) * 0.08, -0.08, (Math.random() - 0.5) * 0.08);
            }
        }
    }

    private record DeathData(long time, Vec3d pos) {}
}
