package com.lightutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class LightCommand {

    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("rojo", 0xFF0000);     COLORS.put("red", 0xFF0000);
        COLORS.put("verde", 0x00FF00);    COLORS.put("green", 0x00FF00);
        COLORS.put("azul", 0x0055FF);     COLORS.put("blue", 0x0055FF);
        COLORS.put("amarillo", 0xFFFF00); COLORS.put("yellow", 0xFFFF00);
        COLORS.put("naranja", 0xFF8800);  COLORS.put("orange", 0xFF8800);
        COLORS.put("morado", 0xAA00FF);   COLORS.put("purple", 0xAA00FF);
        COLORS.put("rosa", 0xFF55AA);     COLORS.put("pink", 0xFF55AA);
        COLORS.put("cian", 0x00FFFF);     COLORS.put("cyan", 0x00FFFF);
        COLORS.put("blanco", 0xFFFFFF);   COLORS.put("white", 0xFFFFFF);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("lightutils")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(COLORS.keySet(), b))
                                .then(Commands.argument("intensidad", IntegerArgumentType.integer(0, 15))
                                        .then(Commands.argument("radio", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("tag", StringArgumentType.word())
                                                        .executes(LightCommand::add))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(existingTags(c.getSource()), b))
                                .executes(LightCommand::clear))));
    }

    private static int parseColor(String s) {
        String key = s.toLowerCase(Locale.ROOT);
        if (COLORS.containsKey(key)) return COLORS.get(key);
        if (key.startsWith("#")) key = key.substring(1);
        if (key.length() != 6) return -1;
        try { return Integer.parseInt(key, 16); } catch (NumberFormatException e) { return -1; }
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        String colorStr = StringArgumentType.getString(ctx, "color");
        int intensity = IntegerArgumentType.getInteger(ctx, "intensidad");
        int radius = IntegerArgumentType.getInteger(ctx, "radio");
        String tag = StringArgumentType.getString(ctx, "tag");

        int color = parseColor(colorStr);
        if (color < 0) {
            src.sendFailure(Component.literal("Color invalido. Usa un nombre (rojo, azul...) o hex de 6 digitos (ej: ff8800)."));
            return 0;
        }
        if (tag.equals(LightUtils.BASE_TAG)) {
            src.sendFailure(Component.literal("Ese tag esta reservado."));
            return 0;
        }

        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());

        Marker marker = EntityType.MARKER.create(level);
        if (marker == null) return 0;
        marker.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        marker.addTag(LightUtils.BASE_TAG);
        marker.addTag(tag);
        marker.getPersistentData().putInt("lu_color", color);
        marker.getPersistentData().putInt("lu_intensity", intensity);
        marker.getPersistentData().putDouble("lu_radius", radius);
        level.addFreshEntity(marker);

        if (intensity > 0 && level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, intensity), 3);
        }

        src.sendSuccess(() -> Component.literal("Luz '" + tag + "' creada en " + pos.toShortString()
                + " (#" + String.format("%06X", color) + ", intensidad " + intensity + ", radio " + radius + ")"), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String tag = StringArgumentType.getString(ctx, "tag");
        int removed = 0;

        for (ServerLevel level : src.getServer().getAllLevels()) {
            for (Entity m : level.getEntities(EntityType.MARKER,
                    e -> e.getTags().contains(LightUtils.BASE_TAG) && e.getTags().contains(tag))) {
                BlockPos pos = m.blockPosition();
                if (level.getBlockState(pos).is(Blocks.LIGHT)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                m.discard();
                removed++;
            }
        }

        if (removed == 0) {
            src.sendFailure(Component.literal("No se encontro ninguna luz con el tag '" + tag + "'."));
            return 0;
        }
        final int n = removed;
        src.sendSuccess(() -> Component.literal("Se eliminaron " + n + " luz/luces con el tag '" + tag + "'."), true);
        return removed;
    }

    private static Set<String> existingTags(CommandSourceStack src) {
        Set<String> tags = new TreeSet<>();
        for (ServerLevel level : src.getServer().getAllLevels()) {
            for (Entity m : level.getEntities(EntityType.MARKER, e -> e.getTags().contains(LightUtils.BASE_TAG))) {
                for (String t : m.getTags()) if (!t.equals(LightUtils.BASE_TAG)) tags.add(t);
            }
        }
        return tags;
    }
}
