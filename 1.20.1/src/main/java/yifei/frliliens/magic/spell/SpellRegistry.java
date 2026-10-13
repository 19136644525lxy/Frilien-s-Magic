package yifei.frliliens.magic.spell;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.spell.basic.LongRangeSpell;
import yifei.frliliens.magic.spell.basic.ManaHideSpell;
import yifei.frliliens.magic.spell.basic.ManaSenseSpell;
import yifei.frliliens.magic.spell.combat.BlackLightSpell;
import yifei.frliliens.magic.spell.combat.BlastSpell;
import yifei.frliliens.magic.spell.combat.CleanSpell;
import yifei.frliliens.magic.spell.combat.DefenseSpell;
import yifei.frliliens.magic.spell.combat.FireSpell;
import yifei.frliliens.magic.spell.combat.FlightSpell;
import yifei.frliliens.magic.spell.combat.FloatSpell;
import yifei.frliliens.magic.spell.combat.LightningSpell;
import yifei.frliliens.magic.spell.combat.ManaStrikeSpell;
import yifei.frliliens.magic.spell.combat.SealSpell;
import yifei.frliliens.magic.spell.combat.ShavedIceSpell;
import yifei.frliliens.magic.spell.combat.ZoltraakSpell;

/**
 * 法术注册表。
 *
 * <p>集中管理所有可用法术，按 id 索引。新增法术只需在此处注册。
 * 使用 {@link LinkedHashMap} 保持注册顺序，便于创造栏展示。
 */
public final class SpellRegistry {

    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    static {
        // 基础能力（不消耗魔力，按键触发）
        register(new ManaSenseSpell());
        register(new ManaHideSpell());
        register(new LongRangeSpell());
        // 战斗魔法（消耗魔力，法杖/卷轴释放）
        register(new ZoltraakSpell());
        register(new DefenseSpell());
        register(new FlightSpell());
        register(new LightningSpell());
        register(new FireSpell());
        register(new BlackLightSpell());
        register(new ManaStrikeSpell());
        register(new SealSpell());
        register(new BlastSpell());
        register(new ShavedIceSpell());
        register(new FloatSpell());
        register(new CleanSpell());
    }

    private SpellRegistry() {
    }

    private static void register(Spell spell) {
        Spell existing = SPELLS.put(spell.getId(), spell);
        if (existing != null) {
            FriliensMagic.LOGGER.warn("重复注册法术 id: {}", spell.getId());
        }
    }

    /** 按 id 获取法术。 */
    public static Spell get(String id) {
        return SPELLS.get(id);
    }

    /** 获取所有已注册法术（不可变）。 */
    public static Collection<Spell> getAll() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }
}
