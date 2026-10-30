package com.lightutils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod(LightUtils.MODID)
public class LightUtils {
    public static final String MODID = "lightutils";
    /** Tag interno que marca todas las luces del mod. */
    public static final String BASE_TAG = "lightutils_light";

    public LightUtils() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        LightCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 10 != 0) return;

        for (Entity m : level.getEntities(EntityType.MARKER, e -> e.getTags().contains(BASE_TAG))) {
            CompoundTag d = m.getPersistentData();
            if (!d.contains("lu_color")) continue;

            int c = d.getInt("lu_color");
            int intensity = d.getInt("lu_intensity");
            double radius = d.getDouble("lu_radius");
            BlockPos pos = m.blockPosition();

            // Mantiene el bloque de luz (se vuelve a poner si alguien lo quito)
            if (intensity > 0 && level.isLoaded(pos)) {
                BlockState cur = level.getBlockState(pos);
                BlockState want = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, intensity);
                if (cur.isAir() || (cur.is(Blocks.LIGHT) && cur != want)) {
                    level.setBlock(pos, want, 3);
                }
            }

            // Halo de color con particulas
            Vector3f col = new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
            DustParticleOptions opt = new DustParticleOptions(col, 1.5f);
            int count = (int) Math.min(25, 5 + radius * 2);
            for (int i = 0; i < count; i++) {
                double dx, dy, dz;
                do {
                    dx = level.random.nextDouble() * 2 - 1;
                    dy = level.random.nextDouble() * 2 - 1;
                    dz = level.random.nextDouble() * 2 - 1;
                } while (dx * dx + dy * dy + dz * dz > 1);
                level.sendParticles(opt, m.getX() + dx * radius, m.getY() + dy * radius,
                        m.getZ() + dz * radius, 1, 0, 0, 0, 0);
            }
        }
    }
}
