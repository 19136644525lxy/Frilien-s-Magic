package yifei.frliliens.magic.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;

/**
 * 委托村民实体（流浪的魔法使）。
 *
 * <p>继承原版 {@link Villager}，固定为傻子村民（Nitwit）职业。
 * 发光、持久化、委托标记等属性由 {@code QuestVillagerHandler} 在
 * {@code EntityJoinLevelEvent} 中统一设置，避免构造函数中附件未就绪的问题。
 */
public class QuestVillagerEntity extends Villager {

    public QuestVillagerEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        // 固定为傻子村民纹理
        this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NITWIT));
    }
}
