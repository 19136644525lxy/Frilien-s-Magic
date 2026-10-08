package yifei.frliliens.magic.attachment;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 玩家当前选中的战斗法术。
 *
 * <p>存储法术 id，法杖右键释放时读取。空字符串表示未选中。
 */
public record SelectedSpellData(String spellId) {

    public static final SelectedSpellData EMPTY = new SelectedSpellData("");

    public static final Codec<SelectedSpellData> CODEC = Codec.STRING
            .xmap(SelectedSpellData::new, SelectedSpellData::spellId);

    public static final StreamCodec<ByteBuf, SelectedSpellData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(SelectedSpellData::new, SelectedSpellData::spellId);

    public boolean isEmpty() {
        return spellId == null || spellId.isEmpty();
    }
}
