package yifei.frliliens.magic.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单个玩家数据的 capability 实现（接口 + provider 二合一）。
 *
 * <p>Forge 1.20.1 的 capability 需要三样东西：能力声明
 * （{@link Capability}）、provider（{@link net.minecraftforge.common.capabilities.ICapabilityProvider}）、
 * 以及序列化（{@link ICapabilitySerializable}）。
 * 5 个数据类如果各写一遍会有大量重复样板，所以抽成这个泛型基类。
 *
 * <h2>两个泛型参数为什么是必要的</h2>
 * Forge 的 {@code Capability<T>} 里 {@code T} 是<b>能力接口类型</b>，不是数据类型。
 * 也就是说应该是 {@code Capability<ManaCapability>} 而不是 {@code Capability<ManaData>}——
 * 后者编译不过。而业务代码想要的是 {@code ManaData}。所以两个都要：
 * <ul>
 *   <li>{@code D} —— 数据类型（{@code ManaData}），业务代码用</li>
 *   <li>{@code I} —— 能力接口类型（{@code ManaCapability}），Forge 查询用</li>
 * </ul>
 * 查询时 {@code getCapability(MANA)} 返回 {@code LazyOptional<ManaCapability>}，
 * 拿到后调 {@link #getValue()} 才得到 {@code ManaData}。
 *
 * <p><b>不可变性约定</b>：本体系的数据类都是不可变 record，
 * 所以 {@link #setValue(Object)} 是「整体替换」而非原地修改。
 * 这样读路径（渲染、判定）永远拿到一致的快照，不会读到半更新状态。
 *
 * <p><b>同步</b>：1.20.1 的 capability <b>不会</b>自动同步到客户端
 * （1.21 NeoForge 的 {@code AttachmentType.sync} 是那边的特性）。
 * 改动后需要由 {@code ModNetwork} 发包，客户端才有最新值。
 *
 * @param <D> 数据类型，必须是不可变对象
 * @param <I> 能力接口类型，即本类的子类型
 */
public abstract class PlayerDataCapability<D, I extends PlayerDataCapability<D, I>>
        implements ICapabilitySerializable<CompoundTag> {

    /** 当前值。 */
    private D value;

    /** 提供给外部查询的句柄。 */
    private final LazyOptional<I> holder = LazyOptional.of(this::self);

    protected PlayerDataCapability(D initial) {
        this.value = initial;
    }

    /** 该数据对应的能力声明（在 {@link ModCapabilities} 里创建），泛型参数是接口类型。 */
    protected abstract Capability<I> capability();

    /** 从 NBT 构造数据；由 {@link #deserializeNBT} 调用。 */
    protected abstract D read(CompoundTag tag);

    /** 把数据写入 NBT；由 {@link #serializeNBT} 调用。 */
    protected abstract void write(D value, CompoundTag tag);

    /** 数据缺失时的初始值（新玩家、损坏存档）。 */
    public abstract D defaultValue();

    /** 取当前值。 */
    public final D getValue() {
        return value;
    }

    /**
     * 整体替换当前值。
     *
     * <p>注意不会自动同步到客户端，调用方需要自行发包。
     */
    public final void setValue(D newValue) {
        this.value = newValue;
    }

    /** 把 this 转成接口类型，供 {@link LazyOptional} 使用。 */
    @SuppressWarnings("unchecked")
    private I self() {
        return (I) this;
    }

    // ===== ICapabilityProvider =====

    @Override
    public @NotNull <R> LazyOptional<R> getCapability(@NotNull Capability<R> cap,
                                                      @Nullable Direction side) {
        // 玩家数据与朝向无关，side 一律忽略
        return capability().orEmpty(cap, holder);
    }

    /** 使句柄失效，玩家退出/重建时必须调用，否则会内存泄漏。 */
    public final void invalidate() {
        holder.invalidate();
    }

    // ===== ICapabilitySerializable =====

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        write(value, tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        D loaded = read(tag);
        this.value = loaded == null ? defaultValue() : loaded;
    }
}
