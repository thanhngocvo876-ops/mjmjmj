package com.mjmod.entity;

import com.mjmod.skill.RelationshipManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Friendly, non-attacking relationship NPC. */
public class ShenQingShuiZhiYaoEntity extends PathfinderMob {
    private final Map<UUID, Long> dialoguePlayers = new HashMap<>();
    private boolean stayingHere;
    private double stayX;
    private double stayY;
    private double stayZ;

    public ShenQingShuiZhiYaoEntity(EntityType<? extends ShenQingShuiZhiYaoEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.50D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Deliberately no target goals: the leech demon is friendly and never
        // initiates combat.
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        beginDialogue(serverPlayer);
        RelationshipManager.startDialogueMusic(serverPlayer);
        ItemStack held = player.getItemInHand(hand);
        if (serverPlayer.isShiftKeyDown() && held.isEmpty()) {
            RelationshipManager.addAffection(serverPlayer, this, 5,
                    pick("夜色这么安静……再靠近一点，也不是不可以。",
                            "别乱动，我只是觉得你的气息很让人安心。",
                            "今晚的河水很凉。你身边，倒是刚刚好。"));
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            giveGift(serverPlayer, held);
            return InteractionResult.CONSUME;
        }
        RelationshipManager.addAffection(serverPlayer, this, 1,
                conversation(RelationshipManager.getAffection(serverPlayer)));
        return InteractionResult.CONSUME;
    }

    /** Executes one of the clickable chat choices after validating it server-side. */
    public boolean performDialogueChoice(ServerPlayer player, String action) {
        return switch (action) {
            case "gift" -> {
                ItemStack gift = player.getMainHandItem();
                if (gift.isEmpty()) gift = player.getOffhandItem();
                if (gift.isEmpty()) {
                    RelationshipManager.sendDialogue(player, this,
                            "你摸了摸口袋，却没找到准备送出的礼物。\n水蛭妖：空着手也来送礼吗？下次可别忘了。",
                            RelationshipManager.getAffection(player));
                } else {
                    giveGift(player, gift);
                }
                yield true;
            }
            case "joke" -> {
                RelationshipManager.addAffection(player, this, 2,
                        pick(player.getScoreboardName() + "：水蛭会讲笑话吗？\n水蛭妖：不会，但我会看着讲笑话的人脸红。",
                                player.getScoreboardName() + "：河里的水为什么不会迷路？因为一直顺流而行。\n水蛭妖：笑话有点冷……不过你认真讲的样子很有趣。",
                                player.getScoreboardName() + "：听完这个笑话，你可不许不笑。\n水蛭妖：那要看你愿不愿意再靠近一点讲。"));
                yield true;
            }
            case "kiss" -> {
                int affection = RelationshipManager.getAffection(player);
                if (affection < 40) {
                    RelationshipManager.sendDialogue(player, this,
                            player.getScoreboardName() + "试着靠近亲吻她。\n水蛭妖轻轻抵住你的额头：还没熟到这种程度呢。",
                            affection);
                } else {
                    int amount = affection >= 80 ? 5 : 3;
                    RelationshipManager.addAffection(player, this, amount,
                            affection >= 80
                                    ? player.getScoreboardName() + "轻轻吻了她。\n水蛭妖没有躲开，第三只眼却害羞地闭上了。"
                                    : player.getScoreboardName() + "试探着吻了她的脸颊。\n水蛭妖：只准这一次……至少今天只准一次。" );
                }
                yield true;
            }
            case "stay" -> {
                stayHere();
                RelationshipManager.sendDialogue(player, this,
                        player.getScoreboardName() + "：你可以留在这里等我吗？\n水蛭妖：好。我会留在这里，等你回来。",
                        RelationshipManager.getAffection(player));
                yield true;
            }
            default -> false;
        };
    }

    /** Holds the NPC still while a player has this NPC's dialogue screen open. */
    public void beginDialogue(ServerPlayer player) {
        dialoguePlayers.put(player.getUUID(), level().getGameTime() + 1200L);
        getNavigation().stop();
    }

    public void endDialogue(UUID playerId) {
        dialoguePlayers.remove(playerId);
    }

    private void stayHere() {
        stayingHere = true;
        stayX = getX();
        stayY = getY();
        stayZ = getZ();
        getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            long now = level().getGameTime();
            Iterator<Map.Entry<UUID, Long>> iterator = dialoguePlayers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                ServerPlayer player = ((net.minecraft.server.level.ServerLevel) level())
                        .getServer().getPlayerList().getPlayer(entry.getKey());
                if (entry.getValue() < now || player == null || player.level() != level()
                        || player.distanceToSqr(this) > 100.0D) {
                    iterator.remove();
                }
            }
        }
        if (stayingHere || !dialoguePlayers.isEmpty()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (stayingHere && distanceToSqr(stayX, stayY, stayZ) > 0.04D) {
                teleportTo(stayX, stayY, stayZ);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("StayHere", stayingHere);
        if (stayingHere) {
            tag.putDouble("StayX", stayX);
            tag.putDouble("StayY", stayY);
            tag.putDouble("StayZ", stayZ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stayingHere = tag.getBoolean("StayHere");
        if (stayingHere) {
            stayX = tag.getDouble("StayX");
            stayY = tag.getDouble("StayY");
            stayZ = tag.getDouble("StayZ");
        }
    }

    private void giveGift(ServerPlayer player, ItemStack held) {
        boolean emerald = held.is(Items.EMERALD);
        boolean edible = held.isEdible();
        int amount = emerald ? 20 : (edible ? 12 : 6);
        if (!player.getAbilities().instabuild) held.shrink(1);
        RelationshipManager.addAffection(player, this, amount,
                player.getScoreboardName() + "：这个送给你。\n水蛭妖：" + (emerald
                        ? pick("这么闪亮的礼物……是想让我多看你几眼吗？",
                                "我收下了。下次见面，也要记得先看我。")
                        : edible
                        ? pick("味道不错。你挑礼物的眼光，比我想象中好。",
                                "分我一半？那我也分你一点秘密吧。")
                        : pick("只要是你认真挑的，我都会好好收着。",
                                "礼物普通了一点，不过送礼物的人还算有趣。")));
    }

    private String conversation(int affection) {
        if (affection >= 80) {
            return pick("你终于来了。我刚才一直在数河面的波纹，数到第几道时会见到你。",
                    "第三只眼能看见血脉的流向，却总是先在人群里看见你。",
                    "再靠近些吧。我不会咬你……至少今天不会。",
                    "换血功不是残忍的法术，它只是让真心无法继续伪装。你想学吗？");
        }
        if (affection >= 40) {
            return pick("别一直盯着我的眼睛看。第三只也会害羞的。",
                    "河水记得每一个来过的人，我也一样。",
                    "你身上的血脉很特别。放心，我只是有一点好奇。",
                    "今天愿意陪我沿着河岸走一会儿吗？");
        }
        return pick("陌生人，你是被河水带到这里的吗？",
                "我有三只眼，所以能比别人多看见一点真心。",
                "先说好，我不吃人。至于会不会捉弄人……那要看心情。",
                "想和我说话？那就别站得那么远。");
    }

    private String pick(String... lines) {
        return lines[getRandom().nextInt(lines.length)];
    }

    @Override
    public boolean isPersistenceRequired() { return true; }
}
