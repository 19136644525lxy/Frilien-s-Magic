package yifei.frliliens.magic.attachment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * 玩家已学法术数据。
 *
 * <p>存储已学习的法术 id 集合。玩家需通过右键卷轴学习法术后方可使用。
 * 基础能力也通过此系统学习后由按键触发。
 */
public record LearnedSpellsData(Set<String> spellIds) {

    /**
     * 初始已学法术：三个基础能力 + 三个基础战斗法术默认学会，无需卷轴。
     * 攻击/防御/飞行作为魔法使的入门法术，开局即可使用。
     */
    private static final Set<String> DEFAULT_SPELLS = Set.of(
            "mana_sense", "mana_hide", "long_range",
            "zoltraak", "defense", "flight");

    public static final LearnedSpellsData EMPTY = new LearnedSpellsData(DEFAULT_SPELLS);

    private static final String TAG_SPELLS = "spells";

    /** 紧凑构造器：杜绝 null 与可变引用。 */
    public LearnedSpellsData {
        spellIds = Collections.unmodifiableSet(new HashSet<>(spellIds));
    }

    /** 写入 NBT：字符串列表。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : spellIds) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_SPELLS, list);
        return tag;
    }

    /**
     * 从 NBT 读取。
     *
     * <p>缺字段时回退 {@link #EMPTY}。
     * 有数据时与 {@link #DEFAULT_SPELLS} 取<b>并集</b>——
     * 这样以后往默认列表里加新法术，老玩家进入游戏会自动学会，
     * 不需要开新存档。代价是玩家无法「取消」默认法术（本来也不该能取消）。
     */
    public static LearnedSpellsData load(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_SPELLS, Tag.TAG_LIST)) {
            return EMPTY;
        }
        Set<String> ids = new HashSet<>();
        ListTag list = tag.getList(TAG_SPELLS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return EMPTY;
        }
        ids.addAll(DEFAULT_SPELLS);
        return new LearnedSpellsData(ids);
    }

    /** 是否已学习指定法术。 */
    public boolean hasLearned(String spellId) {
        return spellIds.contains(spellId);
    }

    /** 学习一个法术，返回新实例。 */
    public LearnedSpellsData learn(String spellId) {
        Set<String> next = new HashSet<>(this.spellIds);
        next.add(spellId);
        return new LearnedSpellsData(next);
    }
}
