package yifei.frliliens.magic.attachment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 玩家每把法杖各自选中的战斗法术。
 *
 * <p>两把法杖是平行玩法，各自记各的选择，切换法杖不会互相覆盖。
 * 法杖 id 用物品注册名（如 {@code frieren_staff}）。
 *
 * <p>序列化用「字符串 → 字符串」的 map：磁盘上是
 * {@code {"frieren_staff":"zoltraak"}}，网络上是 varint 长度的键值对序列。
 */
public record SelectedSpellData(Map<String, String> byStaff) {

    public static final SelectedSpellData EMPTY = new SelectedSpellData(Map.of());

    /** 磁盘格式：JSON 对象。 */
    private static final Codec<Map<String, String>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    /**
     * 持久化编解码器，带旧存档容错。
     *
     * <p>旧版本把选中法术存成裸字符串（{@code "zoltraak"}），与当前 map 格式不兼容。
     * 解码失败时退回 {@link #EMPTY} 并记一条日志，而不是让整个玩家数据加载失败——
     * 代价只是升级后要重新选一次法术。
     */
    public static final Codec<SelectedSpellData> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SelectedSpellData, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<Map<String, String>, T>> parsed = MAP_CODEC.decode(ops, input);
            if (parsed.result().isPresent()) {
                return parsed.map(pair -> Pair.of(new SelectedSpellData(pair.getFirst()),
                        pair.getSecond()));
            }
            FriliensMagic.LOGGER.warn("选中法术数据格式不兼容（可能是旧版本存档），已重置");
            // 必须写全限定名：Codec 也有一个同名的 EMPTY 常量，简写会被解析成它
            return DataResult.success(
                    Pair.<SelectedSpellData, T>of(SelectedSpellData.EMPTY, input));
        }

        @Override
        public <T> DataResult<T> encode(SelectedSpellData input, DynamicOps<T> ops, T prefix) {
            return MAP_CODEC.encode(input.byStaff, ops, prefix);
        }
    };

    /** 网络同步：varint 长度的键值对序列。 */
    public static final StreamCodec<ByteBuf, SelectedSpellData> STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(
                            LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)
                    .map(SelectedSpellData::new, SelectedSpellData::byStaff);

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
