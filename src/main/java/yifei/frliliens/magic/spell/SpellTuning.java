package yifei.frliliens.magic.spell;

import java.util.HashMap;
import java.util.Map;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.config.Config;

/**
 * 法术基础数值的集中查询入口。
 *
 * <p>把 {@link Config} 里按法术 id 定义的魔力消耗／冷却汇总成一张表，法术类只需
 * 调用 {@link #manaCost(String)}，不必各自 import 配置项。
 *
 * <p><b>注意读取时机</b>：配置在模组构造之后才加载，而 {@code SpellRegistry} 的静态
 * 初始化更早。所以这里<b>不能</b>把值缓存进静态字段，必须每次现读——否则会拿到
 * {@code ModConfigSpec} 未加载时的异常或过期值。开销只是几次 map 查找，无所谓。
 */
public final class SpellTuning {

    /** 法术 id → 配置项。 */
    private static final Map<String, Config.SpellSettings> BY_ID = new HashMap<>();

    /** 找不到 id 时的兜底值，避免法术完全无法施放。 */
    private static final int FALLBACK_MANA_COST = 0;
    private static final int FALLBACK_COOLDOWN = 20;

    static {
        BY_ID.put("zoltraak", Config.ZOLTRAAK);
        BY_ID.put("defense", Config.DEFENSE);
        BY_ID.put("flight", Config.FLIGHT);
        BY_ID.put("judradjim", Config.JUDRADJIM);
        BY_ID.put("vollzanbel", Config.VOLLZANBEL);
        BY_ID.put("black_light", Config.BLACK_LIGHT);
        BY_ID.put("mana_strike", Config.MANA_STRIKE);
        BY_ID.put("seal", Config.SEAL);
        BY_ID.put("blast", Config.BLAST);
        BY_ID.put("shaved_ice", Config.SHAVED_ICE);
        BY_ID.put("float", Config.FLOAT);
        BY_ID.put("clean", Config.CLEAN);
        BY_ID.put("mana_sense", Config.MANA_SENSE);
        BY_ID.put("mana_hide", Config.MANA_HIDE);
        BY_ID.put("long_range", Config.LONG_RANGE);
    }

    private SpellTuning() {
    }

    /**
     * 取法术的魔力消耗。
     *
     * @param spellId 法术 id
     * @return 配置值；该 id 没有配置项时返回兜底值并记一条日志
     */
    public static int manaCost(String spellId) {
        Config.SpellSettings settings = BY_ID.get(spellId);
        if (settings == null) {
            warnMissing(spellId);
            return FALLBACK_MANA_COST;
        }
        return settings.manaCost().getAsInt();
    }

    /**
     * 取法术的冷却（tick）。
     *
     * @param spellId 法术 id
     * @return 配置值；该 id 没有配置项时返回兜底值并记一条日志
     */
    public static int cooldown(String spellId) {
        Config.SpellSettings settings = BY_ID.get(spellId);
        if (settings == null) {
            warnMissing(spellId);
            return FALLBACK_COOLDOWN;
        }
        return settings.cooldown().getAsInt();
    }

    /** 已登记的 id 数量，供自检使用。 */
    public static int registeredCount() {
        return BY_ID.size();
    }

    /** 某个 id 是否有对应的配置项，供自检使用。 */
    public static boolean hasSettings(String spellId) {
        return BY_ID.containsKey(spellId);
    }

    private static void warnMissing(String spellId) {
        FriliensMagic.LOGGER.warn(
                "Spell {} has no Config tuning entry, using fallback values"
                + " (register it in SpellTuning.BY_ID)",
                spellId);
    }
}
