package yifei.frliliens.magic.spell;

import java.util.List;
import java.util.Map;

import yifei.frliliens.magic.item.ModItems;

/**
 * 法杖与法术的归属关系。
 *
 * <p>每个法杖只显示并施放自己列表内的法术。集中放在这里而不是给
 * {@link Spell} 加方法，是为了新增或调整归属时只改一处，不必碰每个法术类。
 *
 * <p>「已学会」与「本法杖可用」是两套独立判定：学习解锁法术本身，
 * 归属决定哪把法杖能放。两者都满足才能施放。
 */
public final class StaffSpells {

    /** 芙丽莲的法杖：全部战斗魔法 + 刨冰（不含菲伦专属的漂浮、清洁魔法）。 */
    private static final List<String> FRIEREN = List.of(
            "zoltraak", "defense", "flight", "judradjim", "vollzanbel",
            "black_light", "mana_strike", "seal", "blast", "shaved_ice");

    /** 菲伦的法杖：攻击、防御、飞行 + 生活魔法（漂浮、清洁为菲伦专属）。 */
    private static final List<String> PHIREN = List.of(
            "zoltraak", "defense", "flight", "float", "clean");

    /**
     * 法杖注册 id → 该法杖可用的战斗法术。
     *
     * <p>用 {@link ModItems} 的注册名做键，避免字符串写错。这会触发
     * {@code ModItems} 的类初始化，而 {@code ModItems} 又引用
     * {@link SpellRegistry}，所以初始化的先后顺序必须是
     * {@code ModItems → SpellRegistry → ModItems(完成)}。这一点由
     * {@code SpellRegistry} <b>不</b>反向引用本类来保证——后续给法术加归属
     * 判定时，不要把本类塞进 {@link Spell} 的静态初始化里，否则会变成循环。
     */
    private static final Map<String, List<String>> BY_STAFF = Map.of(
            ModItems.FRIEREN_STAFF.getId().getPath(), FRIEREN,
            ModItems.PHIREN_STAFF.getId().getPath(), PHIREN);

    private StaffSpells() {
    }

    /**
     * 取得该法杖可用的战斗法术 id 列表。
     *
     * @param staffId 法杖的物品注册名（如 {@code frieren_staff}）
     * @return 该法杖的法术列表；未知法杖返回空列表
     */
    public static List<String> forStaff(String staffId) {
        return BY_STAFF.getOrDefault(staffId, List.of());
    }

    /**
     * 判断该法杖能否施放指定法术。
     *
     * @param staffId 法杖的物品注册名
     * @param spellId 法术 id
     */
    public static boolean allows(String staffId, String spellId) {
        return spellId != null && forStaff(staffId).contains(spellId);
    }
}
