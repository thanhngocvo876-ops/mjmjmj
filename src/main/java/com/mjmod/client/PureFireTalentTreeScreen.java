package com.mjmod.client;

import com.mjmod.MJMod;
import com.mjmod.network.MJNetwork;
import com.mjmod.skill.PureFireSkillManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 纯质阳炎天赋树界面（O 键打开）
 * 体术/法术/构建三页共用立绘背景，展示 1~10 技能点亮与 8 快捷槽位装配
 */
public class PureFireTalentTreeScreen extends Screen {
    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/talent_tree_bg.png");

    private static final ResourceLocation[] SKILL_ICONS = new ResourceLocation[] {
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_1.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_2.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_3.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_4.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_5.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_6.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_7.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_8.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_9.png"),
            new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_10.png")
    };

    private static final String[] SKILL_NAMES = new String[] {
            "双手合焰·漩涡火焰",
            "纯质阳炎·双手附焰",
            "纯质阳炎·直冲拳",
            "纯质阳炎·增压",
            "纯阳护体",
            "纯质阳炎·横斩",
            "纯质阳炎·穿刺",
            "纯质阳炎·火墙",
            "纯质阳炎·烈阳火球",
            "纯质阳炎·阳炎领域"
    };

    private static final String[] SKILL_TYPES = new String[] {
            "法术", // 1 双手合焰·漩涡火焰
            "体术", // 2 纯质阳炎·双手附焰
            "体术", // 3 纯质阳炎·直冲拳
            "体术", // 4 纯质阳炎·跃起爆冲
            "构建", // 5 纯阳护体
            "体术", // 6 纯质阳炎·横斩
            "体术", // 7 纯质阳炎·穿刺
            "构建", // 8 纯质阳炎·火墙
            "法术",  // 9 纯质阳炎·烈阳火球
            "构建"   // 10 纯质阳炎·阳炎领域
    };

    private static final String[] SKILL_DESCS = new String[] {
            "【法术体系】汇聚周身纯质阳炎于胸前掌心，向前持续喷涌超高热漩涡烈焰，引发大范围焚尽与烈火风暴。",
            "【体术体系】引火附脉，将至阳纯火无损缠裹于双臂双拳，近战攻击与拳脚伤害大幅暴增并附带燃魂灼烧。",
            "【体术体系】调动足底气劲与拳锋火劲瞬间突进前冲，轰出击穿护甲的爆裂冲拳，强力击退并引爆目标。",
            "【体术体系】必须先开启二技能双手纯质阳炎。借纯阳炎爆发力旱地拔葱破空跃起，聚千钧之势自九霄垂直下砸，重击地面激荡烈焰冲击波。",
            "【构建体系】以心火引动天地纯阳灵气化为致密交错的气浪与罡气护体面片，形成绝对防护并显著减免伤害。",
            "【体术体系】以臂为柄、凝纯火为宽幅光刃，向前迅猛横斩斩出一道高密度纯火弧光，扇形席卷范围内诸敌。",
            "【体术体系】蓄势火刃突进前刺，火刃从右手凝聚而出，刺完后留在手上5秒，期间每次攻击附加重伤。",
            "【构建体系】右手凝聚火刃向前推掌，在前方平地筑起通天火墙。火刃在手上保留5秒，期间每次攻击附加重伤。",
            "【法术体系】单手托举凝聚微缩太阳般的烈阳火球，蓄力后猛烈下砸，直接命中造成40点伤害。",
            "【构建体系】展开150×150、 高60格的阳炎领域，持续60秒。金红描边与辉光从中心向四周蔓延，地面沿真实方块轮廓绘制；领域内施法者获得抗性提升II与速度I，其他生物获得缓慢II与发光；每停留10秒抽取2%当前攻击力并吸收。开启即进入5分钟冷却。"
    };

    private static final int NODE_SIZE = 28;
    private final Button[] pageTabs = new Button[3];
    private int currentPage = 0;
    private int selectedSkillForAssign = 0; // 选中的技能（准备装配到快捷槽）

    public PureFireTalentTreeScreen() {
        super(Component.literal("纯质阳炎·天赋树"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int bottomY = height - 28;
        addRenderableWidget(Button.builder(Component.literal("关闭界面"), b -> onClose())
                .bounds(cx - 35, bottomY, 70, 20).build());

        int bgW = PureFireTalentLayout.backgroundWidth(width, height);
        int bgY = PureFireTalentLayout.backgroundY(width, height);
        int tabW = Math.min(70, (bgW - 36) / 3);
        int startX = cx - (3 * tabW + 10) / 2;
        for (int page = 0; page < PureFireTalentLayout.pageCount(); page++) {
            final int targetPage = page;
            pageTabs[page] = addRenderableWidget(Button.builder(
                    Component.literal(PureFireTalentLayout.pageName(page)),
                    b -> setPage(targetPage))
                    .bounds(startX + page * (tabW + 5), bgY + 46, tabW, 18).build());
        }
        updatePageTabs();
    }

    private void setPage(int page) {
        currentPage = page;
        updatePageTabs();
    }

    private void updatePageTabs() {
        for (int page = 0; page < PureFireTalentLayout.pageCount(); page++) {
            pageTabs[page].setMessage(Component.literal(page == currentPage
                    ? "§6§l【" + PureFireTalentLayout.pageName(page) + "】"
                    : PureFireTalentLayout.pageName(page)));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);

        int cx = width / 2;
        int cy = height / 2;

        // 1. 渲染背景立绘图（东方月初/苏苏/桃树）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int bgW = PureFireTalentLayout.backgroundWidth(width, height);
        int bgH = PureFireTalentLayout.backgroundHeight(width, height);
        int bgX = cx - bgW / 2;
        int bgY = PureFireTalentLayout.backgroundY(width, height);
        gg.blit(BG_TEXTURE, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);

        // 柔和半透明暗色遮罩提亮UI对比度
        gg.fill(bgX, bgY, bgX + bgW, bgY + bgH, 0x55100806);
        gg.renderOutline(bgX, bgY, bgW, bgH, 0xFFE09030);

        // 2. 标题与天资信息栏
        gg.drawCenteredString(font, "§6§l【纯质阳炎·天赋神树】", cx, bgY + 8, 0xFFFFFF);
        
        int pts = ClientEvents.PURE_FIRE_TALENT_POINTS;
        int layers = ClientEvents.PURE_FIRE_MEDITATION_LAYERS;
        long cd = ClientEvents.PURE_FIRE_MEDITATION_CD_REMAINING;
        float drop = 10.0F + (layers * 5.0F);
        String cdText = cd <= 0 ? "§a已就绪" : String.format("§c%02d分%02d秒", cd / 60000L, (cd % 60000L) / 1000L);

        String info1 = String.format("§e可用天赋点: §6§l%d §f| §b今日入定冥想: §e%d/6 层 §7(击杀掉落率 +%.0f%%)",
                pts, layers, drop);
        String info2 = String.format("§7冥想CD: %s §7| §6操作说明: 点击已习得技能后，再点击下方槽位即可装配", cdText);
        gg.drawCenteredString(font, info1, cx, bgY + 22, 0xFFFFFF);
        gg.drawCenteredString(font, info2, cx, bgY + 33, 0xFFFFFF);

        // 所有页面用同一张背景；其它两页的基础技能在法术页，不绘制误导性的跨页连线。
        gg.drawCenteredString(font, currentPage == 1
                        ? "§7基础技能 1 是其他技能的前置"
                        : "§7前置：法术页技能 1 · 已习得技能可跨页装配",
                cx, bgY + 69, 0xFFFFFF);
        // 4. 只渲染当前页的技能节点，技能 ID 与原存档及服务端保持一致。
        int hoveredSkill = 0;
        int nodeSize = NODE_SIZE;
        for (int i : PureFireTalentLayout.skillsForPage(currentPage)) {
            int nx = PureFireTalentLayout.nodeCenterX(width, height, currentPage, i) - nodeSize / 2;
            int ny = PureFireTalentLayout.nodeCenterY(width, height) - nodeSize / 2;
            boolean unlocked = isSkillUnlocked(i);
            boolean canLearn = !unlocked && pts >= 4 && (i == 1 || isSkillUnlocked(1));
            boolean selected = (selectedSkillForAssign == i);

            // 边框底色
            int border = selected ? 0xFFFFFFFF : (unlocked ? 0xFFFFD700 : (canLearn ? 0xFF00FF7F : 0xFF555555));
            int bgBox = unlocked ? 0xDD3A1500 : 0xBB181818;
            gg.fill(nx - 2, ny - 2, nx + nodeSize + 2, ny + nodeSize + 2, border);
            gg.fill(nx, ny, nx + nodeSize, ny + nodeSize, bgBox);

            // 图标渲染
            RenderSystem.setShaderColor(unlocked ? 1.0F : 0.45F, unlocked ? 1.0F : 0.45F, unlocked ? 1.0F : 0.45F, 1.0F);
            gg.blit(SKILL_ICONS[i - 1], nx + 2, ny + 2, 0, 0, nodeSize - 4, nodeSize - 4, nodeSize - 4, nodeSize - 4);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 标号
            gg.drawString(font, String.valueOf(i), nx + 3, ny + 3, unlocked ? 0xFFFF55 : 0xAAAAAA, false);

            if (mouseX >= nx && mouseX <= nx + nodeSize && mouseY >= ny && mouseY <= ny + nodeSize) {
                hoveredSkill = i;
            }
        }

        // 5. 底部 8 快捷槽位栏 (用于快捷装配)
        int slotW = 26;
        int slotH = 26;
        int slotSpacing = 4;
        int totalSlotBarW = 8 * slotW + 7 * slotSpacing;
        int slotStartX = cx - totalSlotBarW / 2;
        int slotY = bgY + bgH - 42;

        gg.drawCenteredString(font, "§e§l【快捷技能槽位装配 (1~8)】", cx, slotY - 12, 0xFFFFFF);

        int hoveredSlot = -1;
        for (int s = 0; s < 8; s++) {
            int sx = slotStartX + s * (slotW + slotSpacing);
            int equipped = (s < ClientEvents.PURE_FIRE_SLOT_SKILLS.length) ? ClientEvents.PURE_FIRE_SLOT_SKILLS[s] : 0;
            boolean isCurSelectedSlot = (ClientEvents.PURE_FIRE_SELECTED_SLOT == s);

            int slotBorder = isCurSelectedSlot ? 0xFFFF4500 : 0xFF997744;
            gg.fill(sx - 1, slotY - 1, sx + slotW + 1, slotY + slotH + 1, slotBorder);
            gg.fill(sx, slotY, sx + slotW, slotY + slotH, 0xDD1A1008);

            if (equipped >= 1 && equipped <= SKILL_NAMES.length) {
                gg.blit(SKILL_ICONS[equipped - 1], sx + 2, slotY + 2, 0, 0, slotW - 4, slotH - 4, slotW - 4, slotH - 4);
            }

            // 槽位序号标识
            gg.drawString(font, "§7" + (s + 1), sx + 2, slotY + 2, 0xFFFFFF, false);

            if (mouseX >= sx && mouseX <= sx + slotW && mouseY >= slotY && mouseY <= slotY + slotH) {
                hoveredSlot = s;
            }
        }

        super.render(gg, mouseX, mouseY, partialTick);

        // 6. Tooltip 提示
        if (hoveredSkill > 0) {
            boolean unlocked = isSkillUnlocked(hoveredSkill);
            String type = SKILL_TYPES[hoveredSkill - 1];
            String typeColor = switch (type) {
                case "体术" -> "§c"; // 红
                case "法术" -> "§b"; // 蓝
                default -> "§a";   // 绿 (构建)
            };
            java.util.List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.literal("§6§l技能 " + hoveredSkill + "：" + SKILL_NAMES[hoveredSkill - 1] + "  " + typeColor + "【" + type + "】"));
            tip.add(Component.literal("§7" + SKILL_DESCS[hoveredSkill - 1]));
            tip.add(Component.literal(""));
            if (unlocked) {
                tip.add(Component.literal("§a✔ 已点亮该纯阳技能"));
                tip.add(Component.literal("§e▶ 左键点击选择，再点击下方快捷槽进行装配"));
            } else {
                tip.add(Component.literal("§c✘ 尚未习得"));
                if (hoveredSkill > 1 && !isSkillUnlocked(1)) {
                    tip.add(Component.literal("§8需要前置：领悟纯质阳炎一技能"));
                } else if (pts >= 4) {
                    tip.add(Component.literal("§6▶ 点击消耗 4 点天赋点立即领悟！"));
                } else {
                    tip.add(Component.literal("§c天赋点不足（需要 4 点，当前: " + pts + " 点）"));
                }
            }
            gg.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (hoveredSlot >= 0) {
            int equipped = (hoveredSlot < ClientEvents.PURE_FIRE_SLOT_SKILLS.length) ? ClientEvents.PURE_FIRE_SLOT_SKILLS[hoveredSlot] : 0;
            java.util.List<Component> tip = new java.util.ArrayList<>();
            tip.add(Component.literal("§e快捷槽位 " + (hoveredSlot + 1)));
            if (equipped >= 1 && equipped <= SKILL_NAMES.length) {
                String type = SKILL_TYPES[equipped - 1];
                String typeColor = switch (type) {
                    case "体术" -> "§c";
                    case "法术" -> "§b";
                    default -> "§a";
                };
                tip.add(Component.literal("§6已装配：" + SKILL_NAMES[equipped - 1] + "  " + typeColor + "【" + type + "】"));
            } else {
                tip.add(Component.literal("§7当前为空"));
            }
            if (selectedSkillForAssign > 0) {
                tip.add(Component.literal("§b▶ 点击装配选中的「" + SKILL_NAMES[selectedSkillForAssign - 1] + "」"));
            } else {
                tip.add(Component.literal("§8先在上方选择技能，再点击此处进行装配"));
            }
            tip.add(Component.literal("§8右键点击可清空此槽位"));
            gg.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private boolean isSkillUnlocked(int skillId) {
        return (ClientEvents.PURE_FIRE_UNLOCKED_MASK & (1 << (skillId - 1))) != 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = width / 2;
        int cy = height / 2;
        int nodeSize = NODE_SIZE;

        // 1. 仅允许点击当前页面上实际显示的节点。
        for (int i : PureFireTalentLayout.skillsForPage(currentPage)) {
            int nx = PureFireTalentLayout.nodeCenterX(width, height, currentPage, i) - nodeSize / 2;
            int ny = PureFireTalentLayout.nodeCenterY(width, height) - nodeSize / 2;
            if (mouseX >= nx && mouseX <= nx + nodeSize && mouseY >= ny && mouseY <= ny + nodeSize) {
                if (isSkillUnlocked(i)) {
                    // 已解锁：选择用于装配
                    selectedSkillForAssign = (selectedSkillForAssign == i) ? 0 : i;
                    return true;
                } else {
                    // 未解锁：尝试消耗天赋点领悟
                    MJNetwork.sendPureFireSkill(8, i, false);
                    return true;
                }
            }
        }

        // 2. 点击底部快捷槽位
        int bgH = PureFireTalentLayout.backgroundHeight(width, height);
        int bgY = PureFireTalentLayout.backgroundY(width, height);
        int slotW = 26;
        int slotH = 26;
        int slotSpacing = 4;
        int totalSlotBarW = 8 * slotW + 7 * slotSpacing;
        int slotStartX = cx - totalSlotBarW / 2;
        int slotY = bgY + bgH - 42;

        for (int s = 0; s < 8; s++) {
            int sx = slotStartX + s * (slotW + slotSpacing);
            if (mouseX >= sx && mouseX <= sx + slotW && mouseY >= slotY && mouseY <= slotY + slotH) {
                if (button == 1) {
                    // 右键清空槽位
                    MJNetwork.sendPureFireSlotAssign(s, 0);
                    ClientEvents.PURE_FIRE_SLOT_SKILLS[s] = 0;
                    return true;
                } else if (button == 0) {
                    // 左键：若选中了技能则装配进去
                    if (selectedSkillForAssign > 0 && isSkillUnlocked(selectedSkillForAssign)) {
                        MJNetwork.sendPureFireSlotAssign(s, selectedSkillForAssign);
                        ClientEvents.PURE_FIRE_SLOT_SKILLS[s] = selectedSkillForAssign;
                        selectedSkillForAssign = 0; // 装配后取消选中
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
