package yifei.frliliens.magic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Mod configuration options. */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Log the staves' registry names once during common setup. */
    public static final ModConfigSpec.BooleanValue LOG_STAVES = BUILDER
            .comment("Log the staff items during common setup")
            .define("logStaves", true);

    // ===== 防御魔法 / Defense Magic =====

    /** 护盾最大强度（HP）。 */
    public static final ModConfigSpec.IntValue SHIELD_MAX_STRENGTH = BUILDER
            .comment("Maximum shield strength (HP) when the defense spell is cast")
            .defineInRange("shieldMaxStrength", 100, 1, 10000);

    /** 护盾半径（格）。 */
    public static final ModConfigSpec.DoubleValue SHIELD_RADIUS = BUILDER
            .comment("Shield radius in blocks")
            .defineInRange("shieldRadius", 2.5D, 0.5D, 10.0D);

    /** 施法后法杖冷却（tick，20 tick = 1 秒）。 */
    public static final ModConfigSpec.IntValue SHIELD_CAST_COOLDOWN = BUILDER
            .comment("Staff cooldown in ticks after casting the defense spell (20 ticks = 1 second)")
            .defineInRange("shieldCastCooldown", 40, 0, 600);

    // ===== 魔力条 HUD =====

    /** 魔力条左上角 X 坐标（GUI 像素）。 */
    public static final ModConfigSpec.IntValue MANA_BAR_X = BUILDER
            .comment("Mana bar X position (top-left, GUI pixels)")
            .defineInRange("manaBarX", 10, 0, 10000);

    /** 魔力条左上角 Y 坐标（GUI 像素）。 */
    public static final ModConfigSpec.IntValue MANA_BAR_Y = BUILDER
            .comment("Mana bar Y position (top-left, GUI pixels)")
            .defineInRange("manaBarY", 10, 0, 10000);

    /** 魔力条是否显示。 */
    public static final ModConfigSpec.BooleanValue MANA_BAR_VISIBLE = BUILDER
            .comment("Whether the mana bar HUD is visible")
            .define("manaBarVisible", true);

    /** 魔力条缩放比例。 */
    public static final ModConfigSpec.DoubleValue MANA_BAR_SCALE = BUILDER
            .comment("Mana bar scale factor (0.5 ~ 3.0)")
            .defineInRange("manaBarScale", 1.0D, 0.5D, 3.0D);

    /** 是否显示魔力值文字。 */
    public static final ModConfigSpec.BooleanValue MANA_BAR_SHOW_TEXT = BUILDER
            .comment("Whether to show the mana value text on the bar")
            .define("manaBarShowText", true);

    // ===== 委托系统 =====

    /** 委托村民刷新概率（Nitwit 中标记比例）。 */
    public static final ModConfigSpec.DoubleValue QUEST_VILLAGER_CHANCE = BUILDER
            .comment("Probability that a Nitwit villager becomes a quest giver")
            .defineInRange("questVillagerChance", 0.15D, 0.0D, 1.0D);

    /** 委托村民冷却时间（tick，6000=5分钟）。 */
    public static final ModConfigSpec.IntValue QUEST_VILLAGER_COOLDOWN = BUILDER
            .comment("Quest villager cooldown in ticks after accept/decline (6000 = 5 min)")
            .defineInRange("questVillagerCooldown", 6000, 0, 72000);

    /** 委托收集进度检查间隔（tick）。 */
    public static final ModConfigSpec.IntValue QUEST_COLLECT_CHECK_INTERVAL = BUILDER
            .comment("Interval in ticks to check collect quest progress")
            .defineInRange("questCollectCheckInterval", 100, 20, 600);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
