package com.mjmod.item;

import com.mjmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 千里远行符：主手持符、副手持带玩家 UUID 的头发，右键后只传送到
 * 对应玩家附近。普通生物及其掉落物不再作为传送目标。
 */
public class QianLiYuanXingFuItem extends Item {
    public static final String TAG_TARGET_TYPE = "mjmod_teleport_target_type";
    public static final String TAG_TARGET_ENTITY = "mjmod_teleport_target_entity";
    public static final String TAG_TARGET_PLAYER = "mjmod_teleport_target_player";
    public static final String TAG_TARGET_PLAYER_NAME = "mjmod_teleport_target_player_name";
    public static final String TAG_DIMENSION = "mjmod_teleport_dimension";
    public static final String TAG_X = "mjmod_teleport_x";
    public static final String TAG_Y = "mjmod_teleport_y";
    public static final String TAG_Z = "mjmod_teleport_z";

    private static final double COMPANION_RADIUS = 4.0D;

    public QianLiYuanXingFuItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        return tryTeleport(serverPlayer, stack);
    }

    /** Also handle a right-click on a block instead of letting the block
     * interaction swallow the talisman's use action. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return tryTeleport(serverPlayer, context.getItemInHand()).getResult();
    }

    /** Also handles a right-click directly on a mob, whose interaction packet
     * can be resolved before the generic item-use path. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        return tryTeleport(serverPlayer, stack).getResult();
    }

    private static InteractionResultHolder<ItemStack> tryTeleport(ServerPlayer player,
                                                                    ItemStack talisman) {
        ItemStack catalyst = player.getOffhandItem();
        if (!catalyst.is(ModItems.PLAYER_HAIR.get())) {
            player.sendSystemMessage(Component.literal("\u00a7c副手必须持有目标玩家的头发，千里远行符不能传送到生物。"));
            return InteractionResultHolder.fail(talisman);
        }

        MinecraftServer server = player.getServer();
        if (server == null) return InteractionResultHolder.fail(talisman);

        TargetLocation target = resolveTarget(server, player, catalyst);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c这根头发没有记录有效的玩家目标。"));
            return InteractionResultHolder.fail(talisman);
        }

        ServerLevel originLevel = player.serverLevel();
        Vec3 originPosition = player.position();
        List<Companion> companions = collectCompanions(player, originPosition);
        Vec3 destination = target.findDestination(player);
        if (destination == null) {
            player.sendSystemMessage(Component.literal("\u00a7c目标附近没有足够空间传送。"));
            return InteractionResultHolder.fail(talisman);
        }

        teleportPlayer(player, target.level, destination, player.getYRot(), player.getXRot());
        for (int i = 0; i < companions.size(); i++) {
            Companion companion = companions.get(i);
            Vec3 relative = companion.position.subtract(originPosition);
            Vec3 companionDestination = destination.add(relative);
            Vec3 safe = findSafePosition(companion.player, target.level, companionDestination);
            if (safe == null) {
                safe = findSafePosition(companion.player, target.level,
                        destination.add((i + 1) * 1.15D, 0.0D, 0.0D));
            }
            if (safe != null) {
                teleportPlayer(companion.player, target.level, safe,
                        companion.yaw, companion.pitch);
            }
        }

        spawnTeleportEffects(originLevel, originPosition);
        spawnTeleportEffects(target.level, destination);
        if (!player.getAbilities().instabuild) {
            talisman.shrink(1);
        }
        player.level().playSound(null, BlockPos.containing(destination),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.75F, 0.9F);
        player.sendSystemMessage(Component.literal("\u00a7b千里远行符生效，目标已锁定。"));
        return InteractionResultHolder.consume(talisman);
    }

    private static TargetLocation resolveTarget(MinecraftServer server, ServerPlayer user,
                                                ItemStack catalyst) {
        if (!catalyst.is(ModItems.PLAYER_HAIR.get())) return null;
        CompoundTag tag = catalyst.getTag();
        if (tag == null) return null;

        if (tag.hasUUID(TAG_TARGET_PLAYER)) {
            UUID uuid = tag.getUUID(TAG_TARGET_PLAYER);
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);
            if (targetPlayer != null && targetPlayer != user) {
                // Keep the visible label current if the player changed their
                // name since the hair item was created. UUID remains the
                // authoritative target key.
                refreshPlayerHairName(catalyst, targetPlayer);
                return TargetLocation.forEntity(targetPlayer.serverLevel(), targetPlayer);
            }
            return TargetLocation.fromStoredPosition(server, tag);
        }

        return null;
    }

    private static List<Companion> collectCompanions(ServerPlayer owner, Vec3 ownerPosition) {
        List<Companion> result = new ArrayList<>();
        for (ServerPlayer candidate : owner.serverLevel().players()) {
            if (candidate == owner || candidate.isSpectator()) continue;
            if (candidate.position().distanceToSqr(ownerPosition) <= COMPANION_RADIUS * COMPANION_RADIUS) {
                result.add(new Companion(candidate, candidate.position(), candidate.getYRot(), candidate.getXRot()));
            }
        }
        return result;
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel destination,
                                       Vec3 position, float yaw, float pitch) {
        player.stopRiding();
        player.teleportTo(destination, position.x, position.y, position.z, yaw, pitch);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static Vec3 findSafePosition(ServerPlayer player, ServerLevel level, Vec3 wanted) {
        EntityDimensions dimensions = player.getDimensions(Pose.STANDING);
        double[] yOffsets = {0.0D, 1.0D, -1.0D, 2.0D, -2.0D, 3.0D, -3.0D};
        for (double yOffset : yOffsets) {
            Vec3 candidate = wanted.add(0.0D, yOffset, 0.0D);
            AABB box = dimensions.makeBoundingBox(candidate.x, candidate.y, candidate.z);
            BlockPos below = BlockPos.containing(candidate.x, candidate.y - 0.15D, candidate.z);
            if (level.noCollision(player, box)
                    && (level.getBlockState(below).isAir()
                    || !level.getBlockState(below).getCollisionShape(level, below).isEmpty())) {
                return candidate;
            }
        }
        return null;
    }

    private static void spawnTeleportEffects(ServerLevel level, Vec3 position) {
        // Water-themed arrival/departure effect. Do not use the vanilla portal
        // particle: it reads as an Enderman teleport and clashes with the
        // talisman's visual language.
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                position.x, position.y + 0.8D, position.z, 18,
                0.35D, 0.65D, 0.35D, 0.08D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                position.x, position.y + 0.95D, position.z, 12,
                0.3D, 0.55D, 0.3D, 0.025D);
    }

    /** Removes target metadata written onto ordinary mob drops by old builds.
     * Only MJMod's own keys are touched, so enchantments and other mods' NBT
     * remain intact. */
    public static void clearLegacyMobBinding(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.PLAYER_HAIR.get())) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || (!tag.contains(TAG_TARGET_TYPE) && !tag.contains(TAG_TARGET_ENTITY))) {
            return;
        }
        tag.remove(TAG_TARGET_TYPE);
        tag.remove(TAG_TARGET_ENTITY);
        tag.remove(TAG_DIMENSION);
        tag.remove(TAG_X);
        tag.remove(TAG_Y);
        tag.remove(TAG_Z);
        if (tag.isEmpty()) stack.setTag(null);
    }

    public static void bindPlayerHair(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_TARGET_PLAYER, player.getUUID());
        tag.putString(TAG_TARGET_PLAYER_NAME, player.getGameProfile().getName());
        bindPosition(tag, player);
    }

    private static void refreshPlayerHairName(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();
        String currentName = player.getGameProfile().getName();
        if (!currentName.equals(tag.getString(TAG_TARGET_PLAYER_NAME))) {
            tag.putString(TAG_TARGET_PLAYER_NAME, currentName);
        }
    }

    private static void bindPosition(CompoundTag tag, Entity entity) {
        tag.putString(TAG_DIMENSION, entity.level().dimension().location().toString());
        tag.putDouble(TAG_X, entity.getX());
        tag.putDouble(TAG_Y, entity.getY());
        tag.putDouble(TAG_Z, entity.getZ());
    }

    private record Companion(ServerPlayer player, Vec3 position, float yaw, float pitch) {}

    private static final class TargetLocation {
        private final ServerLevel level;
        private final LivingEntity entity;
        private final Vec3 storedPosition;

        private TargetLocation(ServerLevel level, LivingEntity entity, Vec3 storedPosition) {
            this.level = level;
            this.entity = entity;
            this.storedPosition = storedPosition;
        }

        static TargetLocation forEntity(ServerLevel level, LivingEntity entity) {
            return new TargetLocation(level, entity, null);
        }

        static TargetLocation fromStoredPosition(MinecraftServer server, CompoundTag tag) {
            if (!tag.contains(TAG_DIMENSION) || !tag.contains(TAG_X)
                    || !tag.contains(TAG_Y) || !tag.contains(TAG_Z)) return null;
            ResourceLocation location = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
            if (location == null) return null;
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
            ServerLevel level = server.getLevel(key);
            if (level == null) return null;
            return new TargetLocation(level, null,
                    new Vec3(tag.getDouble(TAG_X), tag.getDouble(TAG_Y), tag.getDouble(TAG_Z)));
        }

        Vec3 findDestination(ServerPlayer user) {
            Vec3 center = entity != null
                    ? entity.position().add(0.0D, entity.getBbHeight() * 0.35D, 0.0D)
                    : storedPosition;
            if (center == null) return null;
            Vec3 away = user.position().subtract(center);
            if (away.lengthSqr() < 1.0E-5D) away = new Vec3(0.0D, 0.0D, 1.0D);
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-5D) away = new Vec3(0.0D, 0.0D, 1.0D);
            away = away.normalize();
            double radius = entity == null ? 2.0D : Math.max(2.0D, entity.getBbWidth() * 0.5D + 1.0D);
            Vec3 wanted = center.add(away.scale(radius));
            return findSafePosition(user, level, wanted);
        }
    }
}
