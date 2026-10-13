package yifei.frliliens.magic.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Mod configuration options. */
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** Log the staves' registry names once during common setup. */
    public static final ForgeConfigSpec.BooleanValue LOG_STAVES = BUILDER
            .comment("Log the staff items during common setup")
            .translation("friliensmagic.configuration.logStaves")
            .define("logStaves", true);

    // ===== 防御魔法 / Defense Magic =====

    /** 护盾最大强度（HP）。 */
    public static final ForgeConfigSpec.IntValue SHIELD_MAX_STRENGTH = BUILDER
            .comment("Maximum shield strength (HP) when the defense spell is cast")
            .translation("friliensmagic.configuration.shieldMaxStrength")
            .defineInRange("shieldMaxStrength", 100, 1, 10000);

    /** 护盾半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue SHIELD_RADIUS = BUILDER
            .comment("Shield radius in blocks")
            .translation("friliensmagic.configuration.shieldRadius")
            .defineInRange("shieldRadius", 2.5D, 0.5D, 10.0D);

    /** 施法后法杖冷却（tick，20 tick = 1 秒）。 */
    public static final ForgeConfigSpec.IntValue SHIELD_CAST_COOLDOWN = BUILDER
            .comment("Staff cooldown in ticks after casting the defense spell (20 ticks = 1 second)")
            .translation("friliensmagic.configuration.shieldCastCooldown")
            .defineInRange("shieldCastCooldown", 40, 0, 600);

    // ===== 魔力条 HUD =====

    /** 魔力条左上角 X 坐标（GUI 像素）。 */
    public static final ForgeConfigSpec.IntValue MANA_BAR_X = BUILDER
            .comment("Mana bar X position (top-left, GUI pixels)")
            .translation("friliensmagic.configuration.manaBarX")
            .defineInRange("manaBarX", 10, 0, 10000);

    /** 魔力条左上角 Y 坐标（GUI 像素）。 */
    public static final ForgeConfigSpec.IntValue MANA_BAR_Y = BUILDER
            .comment("Mana bar Y position (top-left, GUI pixels)")
            .translation("friliensmagic.configuration.manaBarY")
            .defineInRange("manaBarY", 10, 0, 10000);

    /** 魔力条是否显示。 */
    public static final ForgeConfigSpec.BooleanValue MANA_BAR_VISIBLE = BUILDER
            .comment("Whether the mana bar HUD is visible")
            .translation("friliensmagic.configuration.manaBarVisible")
            .define("manaBarVisible", true);

    /** 魔力条缩放比例。 */
    public static final ForgeConfigSpec.DoubleValue MANA_BAR_SCALE = BUILDER
            .comment("Mana bar scale factor (0.5 ~ 3.0)")
            .translation("friliensmagic.configuration.manaBarScale")
            .defineInRange("manaBarScale", 1.0D, 0.5D, 3.0D);

    /** 是否显示魔力值文字。 */
    public static final ForgeConfigSpec.BooleanValue MANA_BAR_SHOW_TEXT = BUILDER
            .comment("Whether to show the mana value text on the bar")
            .translation("friliensmagic.configuration.manaBarShowText")
            .define("manaBarShowText", true);

    // ===== 委托系统 =====

    /** 委托村民刷新概率（Nitwit 中标记比例）。 */
    public static final ForgeConfigSpec.DoubleValue QUEST_VILLAGER_CHANCE = BUILDER
            .comment("Probability that a Nitwit villager becomes a quest giver")
            .translation("friliensmagic.configuration.questVillagerChance")
            .defineInRange("questVillagerChance", 0.15D, 0.0D, 1.0D);

    /** 委托村民冷却时间（tick，6000=5分钟）。 */
    public static final ForgeConfigSpec.IntValue QUEST_VILLAGER_COOLDOWN = BUILDER
            .comment("Quest villager cooldown in ticks after accept/decline (6000 = 5 min)")
            .translation("friliensmagic.configuration.questVillagerCooldown")
            .defineInRange("questVillagerCooldown", 6000, 0, 72000);

    /** 委托收集进度检查间隔（tick）。 */
    public static final ForgeConfigSpec.IntValue QUEST_COLLECT_CHECK_INTERVAL = BUILDER
            .comment("Interval in ticks to check collect quest progress")
            .translation("friliensmagic.configuration.questCollectCheckInterval")
            .defineInRange("questCollectCheckInterval", 100, 20, 600);

    // ===== 欢迎语 =====
    // 欢迎语以「标题」形式显示（屏幕中央，不是聊天栏），所以需要三个时长。

    /** 欢迎语淡入时长（tick）。 */
    public static final ForgeConfigSpec.IntValue WELCOME_TITLE_FADE_IN = BUILDER
            .comment("Welcome title fade-in time in ticks")
            .translation("friliensmagic.configuration.welcomeTitleFadeIn")
            .defineInRange("welcomeTitleFadeIn", 10, 0, 200);

    /** 欢迎语停留时长（tick，20 = 1 秒）。 */
    public static final ForgeConfigSpec.IntValue WELCOME_TITLE_STAY = BUILDER
            .comment("Welcome title stay time in ticks (20 = 1 second)")
            .translation("friliensmagic.configuration.welcomeTitleStay")
            .defineInRange("welcomeTitleStay", 70, 1, 400);

    /** 欢迎语淡出时长（tick）。 */
    public static final ForgeConfigSpec.IntValue WELCOME_TITLE_FADE_OUT = BUILDER
            .comment("Welcome title fade-out time in ticks")
            .translation("friliensmagic.configuration.welcomeTitleFadeOut")
            .defineInRange("welcomeTitleFadeOut", 20, 0, 200);

    // ===== 法术基础数值 =====
    // 每个法术的魔力消耗与冷却。id 必须与 SpellRegistry 里注册的 id 一致，
    // 否则该法术会退回默认值 0 消耗 / 20 冷却（SpellTuning 会打日志提醒）。

    /** 攻击魔法 Zoltraak。 */
    public static final SpellSettings ZOLTRAAK = spell("zoltraak", 50, 20);
    /** 防御魔法。 */
    public static final SpellSettings DEFENSE = spell("defense", 80, 20);
    /** 飞行魔法。 */
    public static final SpellSettings FLIGHT = spell("flight", 60, 20);
    /** 破灭之雷 Judradjim。 */
    public static final SpellSettings JUDRADJIM = spell("judradjim", 120, 20);
    /** 地狱业火 Vollzanbel。 */
    public static final SpellSettings VOLLZANBEL = spell("vollzanbel", 150, 20);
    /** 破坏黑光。 */
    public static final SpellSettings BLACK_LIGHT = spell("black_light", 200, 20);
    /** 魔力挤压 Mana Strike。 */
    public static final SpellSettings MANA_STRIKE = spell("mana_strike", 100, 20);
    /** 封印魔法。 */
    public static final SpellSettings SEAL = spell("seal", 90, 20);
    /** 爆破魔法。 */
    public static final SpellSettings BLAST = spell("blast", 70, 20);
    /** 变出刨冰。 */
    public static final SpellSettings SHAVED_ICE = spell("shaved_ice", 10, 20);
    /** 漂浮魔法。 */
    public static final SpellSettings FLOAT = spell("float", 40, 20);
    /** 清洁魔法（消耗是动态的，见 COOLDOWN/CLEAN_* ）。 */
    public static final SpellSettings CLEAN = spell("clean", 0, 20);
    /** 魔力探知（基础能力，不耗魔力）。 */
    public static final SpellSettings MANA_SENSE = spell("mana_sense", 0, 20);
    /** 魔力隐藏（基础能力，不耗魔力）。 */
    public static final SpellSettings MANA_HIDE = spell("mana_hide", 0, 20);
    /** 长距离魔法（被动，无消耗无冷却）。 */
    public static final SpellSettings LONG_RANGE = spell("long_range", 0, 0);

    // ===== 各法术的可调数值 =====

    /** 攻击魔法：基础射程（未学「长距离魔法」时，单位格）。 */
    public static final ForgeConfigSpec.DoubleValue ZOLTRAAK_RANGE = BUILDER
            .comment("Zoltraak base range in blocks (before learning Long Range)")
            .translation("friliensmagic.configuration.zoltraakRange")
            .defineInRange("zoltraakRange", 30.0D, 1.0D, 256.0D);

    /** 攻击魔法：伤害。 */
    public static final ForgeConfigSpec.DoubleValue ZOLTRAAK_DAMAGE = BUILDER
            .comment("Zoltraak damage per hit")
            .translation("friliensmagic.configuration.zoltraakDamage")
            .defineInRange("zoltraakDamage", 520.0D, 0.0D, 100000.0D);

    /** 攻击魔法：伤害判定半径（注意：大于视觉光柱半径）。 */
    public static final ForgeConfigSpec.DoubleValue ZOLTRAAK_DAMAGE_RADIUS = BUILDER
            .comment("Zoltraak damage radius in blocks; the damage cylinder is wider"
                    + " than the visible beam")
            .translation("friliensmagic.configuration.zoltraakDamageRadius")
            .defineInRange("zoltraakDamageRadius", 5.0D, 0.1D, 32.0D);

    /** 攻击魔法：学「长距离魔法」后的视距倍率基数。 */
    public static final ForgeConfigSpec.DoubleValue ZOLTRAAK_RANGE_MULTIPLIER = BUILDER
            .comment("Zoltraak range = viewDistance * this, once Long Range is learned")
            .translation("friliensmagic.configuration.zoltraakRangeMultiplier")
            .defineInRange("zoltraakRangeMultiplier", 16.0D, 1.0D, 64.0D);

    /** 攻击魔法：每多少点魔力上限额外 +1 倍率。 */
    public static final ForgeConfigSpec.DoubleValue ZOLTRAAK_MANA_PER_MULTIPLIER = BUILDER
            .comment("Extra Zoltraak range multiplier per this much max mana")
            .translation("friliensmagic.configuration.zoltraakManaPerMultiplier")
            .defineInRange("zoltraakManaPerMultiplier", 500.0D, 1.0D, 100000.0D);

    /** 破坏黑光：伤害。 */
    public static final ForgeConfigSpec.DoubleValue BLACK_LIGHT_DAMAGE = BUILDER
            .comment("Black Light damage per hit")
            .translation("friliensmagic.configuration.blackLightDamage")
            .defineInRange("blackLightDamage", 25.0D, 0.0D, 100000.0D);

    /** 破坏黑光：作用长度（格）。 */
    public static final ForgeConfigSpec.DoubleValue BLACK_LIGHT_LENGTH = BUILDER
            .comment("Black Light reach in blocks")
            .translation("friliensmagic.configuration.blackLightLength")
            .defineInRange("blackLightLength", 10.0D, 1.0D, 128.0D);

    /** 破坏黑光：作用半宽（格）。 */
    public static final ForgeConfigSpec.DoubleValue BLACK_LIGHT_WIDTH = BUILDER
            .comment("Black Light beam half-width in blocks")
            .translation("friliensmagic.configuration.blackLightWidth")
            .defineInRange("blackLightWidth", 2.0D, 0.1D, 32.0D);

    /** 破灭之雷：落点选取距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue JUDRADJIM_RANGE = BUILDER
            .comment("Judradjim target picking distance in blocks")
            .translation("friliensmagic.configuration.judradjimRange")
            .defineInRange("judradjimRange", 30.0D, 1.0D, 128.0D);

    /** 破灭之雷：同时召唤的闪电道数。 */
    public static final ForgeConfigSpec.IntValue JUDRADJIM_BOLTS = BUILDER
            .comment("How many lightning bolts Judradjim summons")
            .translation("friliensmagic.configuration.judradjimBolts")
            .defineInRange("judradjimBolts", 3, 1, 32);

    /** 破灭之雷：散布半径（格，按高斯分布）。 */
    public static final ForgeConfigSpec.DoubleValue JUDRADJIM_SPREAD = BUILDER
            .comment("Judradjim lightning scatter radius in blocks (gaussian sigma)")
            .translation("friliensmagic.configuration.judradjimSpread")
            .defineInRange("judradjimSpread", 2.0D, 0.0D, 16.0D);

    /** 地狱业火：施法距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue VOLLZANBEL_RANGE = BUILDER
            .comment("Vollzanbel cast distance in blocks")
            .translation("friliensmagic.configuration.vollzanbelRange")
            .defineInRange("vollzanbelRange", 8.0D, 1.0D, 64.0D);

    /** 地狱业火：火焰云半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue VOLLZANBEL_RADIUS = BUILDER
            .comment("Vollzanbel flame cloud radius in blocks")
            .translation("friliensmagic.configuration.vollzanbelRadius")
            .defineInRange("vollzanbelRadius", 3.0D, 0.5D, 16.0D);

    /** 地狱业火：火焰云持续时间（tick）。 */
    public static final ForgeConfigSpec.IntValue VOLLZANBEL_DURATION = BUILDER
            .comment("Vollzanbel flame cloud duration in ticks")
            .translation("friliensmagic.configuration.vollzanbelDuration")
            .defineInRange("vollzanbelDuration", 100, 20, 2400);

    /** 地狱业火：点燃地面的方形半径（格）。 */
    public static final ForgeConfigSpec.IntValue VOLLZANBEL_FIRE_RADIUS = BUILDER
            .comment("Vollzanbel ground fire square radius in blocks")
            .translation("friliensmagic.configuration.vollzanbelFireRadius")
            .defineInRange("vollzanbelFireRadius", 2, 0, 8);

    /** 爆破魔法：施法距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue BLAST_RANGE = BUILDER
            .comment("Blast cast distance in blocks")
            .translation("friliensmagic.configuration.blastRange")
            .defineInRange("blastRange", 6.0D, 1.0D, 64.0D);

    /** 爆破魔法：爆炸半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue BLAST_RADIUS = BUILDER
            .comment("Blast explosion radius in blocks")
            .translation("friliensmagic.configuration.blastRadius")
            .defineInRange("blastRadius", 6.0D, 1.0D, 32.0D);

    /** 爆破魔法：是否破坏地形。 */
    public static final ForgeConfigSpec.BooleanValue BLAST_BREAK_BLOCKS = BUILDER
            .comment("Whether Blast destroys terrain")
            .translation("friliensmagic.configuration.blastBreakBlocks")
            .define("blastBreakBlocks", true);

    /** 封印魔法：作用半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue SEAL_RADIUS = BUILDER
            .comment("Seal effect radius in blocks")
            .translation("friliensmagic.configuration.sealRadius")
            .defineInRange("sealRadius", 6.0D, 1.0D, 32.0D);

    /** 封印魔法：负面效果持续（tick）。 */
    public static final ForgeConfigSpec.IntValue SEAL_DURATION = BUILDER
            .comment("Seal debuff duration in ticks")
            .translation("friliensmagic.configuration.sealDuration")
            .defineInRange("sealDuration", 160, 20, 7200);

    /** 封印魔法：缓慢效果等级（0 = I 级）。 */
    public static final ForgeConfigSpec.IntValue SEAL_SLOWNESS_LEVEL = BUILDER
            .comment("Seal Slowness amplifier (0 = level I)")
            .translation("friliensmagic.configuration.sealSlownessLevel")
            .defineInRange("sealSlownessLevel", 4, 0, 9);

    /** 封印魔法：失明效果等级（0 = I 级）。 */
    public static final ForgeConfigSpec.IntValue SEAL_BLINDNESS_LEVEL = BUILDER
            .comment("Seal Blindness amplifier (0 = level I)")
            .translation("friliensmagic.configuration.sealBlindnessLevel")
            .defineInRange("sealBlindnessLevel", 0, 0, 9);

    /** 封印魔法：挖掘疲劳效果等级（0 = I 级）。 */
    public static final ForgeConfigSpec.IntValue SEAL_FATIGUE_LEVEL = BUILDER
            .comment("Seal Mining Fatigue amplifier (0 = level I)")
            .translation("friliensmagic.configuration.sealFatigueLevel")
            .defineInRange("sealFatigueLevel", 3, 0, 9);

    /** 漂浮魔法：作用半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue FLOAT_RADIUS = BUILDER
            .comment("Float effect radius in blocks")
            .translation("friliensmagic.configuration.floatRadius")
            .defineInRange("floatRadius", 5.0D, 1.0D, 32.0D);

    /** 漂浮魔法：悬浮持续时间（tick）。 */
    public static final ForgeConfigSpec.IntValue FLOAT_DURATION = BUILDER
            .comment("Float levitation duration in ticks")
            .translation("friliensmagic.configuration.floatDuration")
            .defineInRange("floatDuration", 60, 10, 1200);

    /** 漂浮魔法：对非生物实体施加的向上初速。 */
    public static final ForgeConfigSpec.DoubleValue FLOAT_IMPULSE = BUILDER
            .comment("Upward velocity Float applies to non-living entities")
            .translation("friliensmagic.configuration.floatImpulse")
            .defineInRange("floatImpulse", 0.4D, 0.0D, 8.0D);

    /** 魔力挤压：增益持续时间（tick）。 */
    public static final ForgeConfigSpec.IntValue MANA_STRIKE_DURATION = BUILDER
            .comment("Mana Strike buff duration in ticks")
            .translation("friliensmagic.configuration.manaStrikeDuration")
            .defineInRange("manaStrikeDuration", 200, 20, 7200);

    /** 魔力挤压：力量效果等级（0 = I 级）。 */
    public static final ForgeConfigSpec.IntValue MANA_STRIKE_STRENGTH = BUILDER
            .comment("Mana Strike Strength amplifier (0 = level I)")
            .translation("friliensmagic.configuration.manaStrikeStrength")
            .defineInRange("manaStrikeStrength", 2, 0, 9);

    /** 魔力挤压：速度效果等级（0 = I 级）。 */
    public static final ForgeConfigSpec.IntValue MANA_STRIKE_SPEED = BUILDER
            .comment("Mana Strike Speed amplifier (0 = level I)")
            .translation("friliensmagic.configuration.manaStrikeSpeed")
            .defineInRange("manaStrikeSpeed", 1, 0, 9);

    /** 魔力探知：感知半径（格）。 */
    public static final ForgeConfigSpec.DoubleValue MANA_SENSE_RADIUS = BUILDER
            .comment("Mana Sense radius in blocks")
            .translation("friliensmagic.configuration.manaSenseRadius")
            .defineInRange("manaSenseRadius", 32.0D, 1.0D, 256.0D);

    /** 魔力探知／隐藏：基础持续时间（tick）。 */
    public static final ForgeConfigSpec.IntValue CONCEAL_BASE_DURATION = BUILDER
            .comment("Base duration in ticks for Mana Sense / Mana Hide")
            .translation("friliensmagic.configuration.concealBaseDuration")
            .defineInRange("concealBaseDuration", 200, 20, 7200);

    /** 魔力探知／隐藏：每多少点魔力上限额外 +1 tick。 */
    public static final ForgeConfigSpec.IntValue CONCEAL_MANA_PER_TICK = BUILDER
            .comment("Extra ticks for Mana Sense / Mana Hide per this much max mana")
            .translation("friliensmagic.configuration.concealManaPerTick")
            .defineInRange("concealManaPerTick", 10, 1, 100000);

    /** 清洁魔法：基础魔力消耗。 */
    public static final ForgeConfigSpec.IntValue CLEAN_BASE_MANA = BUILDER
            .comment("Clean spell base mana cost")
            .translation("friliensmagic.configuration.cleanBaseMana")
            .defineInRange("cleanBaseMana", 50, 0, 100000);

    /** 清洁魔法：每 1 点耐久修复额外消耗的魔力。 */
    public static final ForgeConfigSpec.IntValue CLEAN_MANA_PER_DAMAGE = BUILDER
            .comment("Extra mana per point of durability repaired by Clean")
            .translation("friliensmagic.configuration.cleanManaPerDamage")
            .defineInRange("cleanManaPerDamage", 2, 0, 1000);

    /** 清洁魔法：基础冷却（tick）。 */
    public static final ForgeConfigSpec.IntValue CLEAN_BASE_COOLDOWN = BUILDER
            .comment("Clean spell base cooldown in ticks")
            .translation("friliensmagic.configuration.cleanBaseCooldown")
            .defineInRange("cleanBaseCooldown", 100, 0, 72000);

    /** 清洁魔法：每 1 点耐久修复额外增加的冷却（tick）。 */
    public static final ForgeConfigSpec.IntValue CLEAN_COOLDOWN_PER_DAMAGE = BUILDER
            .comment("Extra cooldown ticks per point of durability repaired by Clean")
            .translation("friliensmagic.configuration.cleanCooldownPerDamage")
            .defineInRange("cleanCooldownPerDamage", 2, 0, 1000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    /**
     * 定义一个法术的魔力消耗与冷却。
     *
     * <p>配置键用 camelCase（如 {@code mana_sense} → {@code manaSenseManaCost}），
     * 和文件里其它配置项保持一致的风格；法术 id 本身仍用下划线形式。
     *
     * @param id       法术 id，必须与 {@code SpellRegistry} 中注册的一致
     * @param manaCost 默认魔力消耗
     * @param cooldown 默认冷却（tick）
     */
    private static SpellSettings spell(String id, int manaCost, int cooldown) {
        String key = toCamelCase(id);
        return new SpellSettings(
                BUILDER.comment("Mana cost of " + id)
                        .translation("friliensmagic.configuration." + key + "ManaCost")
                        .defineInRange(key + "ManaCost", manaCost, 0, 1000000),
                BUILDER.comment("Cooldown of " + id + " in ticks (20 = 1 second)")
                        .translation("friliensmagic.configuration." + key + "Cooldown")
                        .defineInRange(key + "Cooldown", cooldown, 0, 72000));
    }

    /** 把 {@code mana_sense} 这样的 id 转成 {@code manaSense}。 */
    private static String toCamelCase(String id) {
        StringBuilder sb = new StringBuilder(id.length());
        boolean upper = false;
        for (char c : id.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    /**
     * 单个法术的基础数值。
     *
     * @param manaCost 魔力消耗配置项
     * @param cooldown 冷却配置项（tick）
     */
    public record SpellSettings(ForgeConfigSpec.IntValue manaCost,
                                ForgeConfigSpec.IntValue cooldown) {
    }

    private Config() {
    }
}
