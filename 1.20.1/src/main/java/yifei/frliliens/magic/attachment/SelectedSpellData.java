package yifei.frliliens.magic.attachment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家每把法杖各自选中的战斗法术。
 *
 * <p>两把法杖是平行玩法，各自记各的选择，切换法杖不会互相覆盖。
 * 法杖 id 用物品注册名（如 {@code frieren_staff}）。
 *
 * <p>NBT 格式：{@code { "frieren_staff": "zoltraak", ... }} 直接映射为子标签。
 */
public record SelectedSpellData(Map<String, String> byStaff) {

    public static final SelectedSpellData EMPTY = new SelectedSpellData(Map.of());

    /** 写入 NBT：每个法杖一个字符串子标签。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        byStaff.forEach(tag::putString);
        return tag;
    }

    /**
     * 从 NBT 读取。
     *
     * <p>旧版本把选中法术存成裸字符串而不是复合标签，这种存档会读成空 map。
     * 这时退回 {@link #EMPTY} 而不是报错——代价只是升级后要重新选一次法术，
     * 总好过整个玩家数据加载失败。
     */
    public static SelectedSpellData load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }
        Map<String, String> map = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            // 只接受字符串型子标签，忽略其它类型的残留数据
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                String spell = tag.getString(key);
                if (!spell.isEmpty()) {
                    map.put(key, spell);
                }
            }
        }
        return map.isEmpty() ? EMPTY : new SelectedSpellData(map);
    }

    /** 紧凑构造器：杜绝 null，避免同步时空指针。 */
    public SelectedSpellData {
        byStaff = Map.copyOf(Objects.requireNonNull(byStaff, "byStaff"));
    }

    /**
     * 取该法杖当前选中的法术 id。
     *
     * @param staffId 法杖的物品注册名
     * @return 法术 id；未选择时返回空字符串
     */
    public String spellFor(String staffId) {
        if (staffId == null) {
            return "";
        }
        return byStaff.getOrDefault(staffId, "");
    }

    /**
     * 该法杖是否已选择法术。
     *
     * @param staffId 法杖的物品注册名
     */
    public boolean hasSelection(String staffId) {
        return !spellFor(staffId).isEmpty();
    }

    /**
     * 为指定法杖选择法术，返回新实例。
     *
     * @param staffId 法杖的物品注册名
     * @param spellId 法术 id；空字符串表示清除该法杖的选择
     */
    public SelectedSpellData select(String staffId, String spellId) {
        Map<String, String> next = new HashMap<>(this.byStaff);
        if (spellId == null || spellId.isEmpty()) {
            next.remove(staffId);
        } else {
            next.put(staffId, spellId);
        }
        return new SelectedSpellData(next);
    }
}
