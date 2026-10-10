package yifei.frliliens.magic.attachment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

    /** Codec：持久化到磁盘。 */
    public static final Codec<LearnedSpellsData> CODEC = Codec.STRING.listOf()
            .xmap(list -> new LearnedSpellsData(new HashSet<>(list)),
                    data -> List.copyOf(data.spellIds));

    /** StreamCodec：网络同步到客户端。 */
    public static final StreamCodec<ByteBuf, LearnedSpellsData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
                    .map(list -> new LearnedSpellsData(new HashSet<>(list)),
                            data -> new ArrayList<>(data.spellIds));

    /** 是否已学习指定法术。 */
    public boolean hasLearned(String spellId) {
        return spellIds.contains(spellId);
    }

    /** 学习一个法术，返回新实例。 */
    public LearnedSpellsData learn(String spellId) {
        Set<String> newSet = new HashSet<>(this.spellIds);
        newSet.add(spellId);
        return new LearnedSpellsData(Collections.unmodifiableSet(newSet));
    }
}
