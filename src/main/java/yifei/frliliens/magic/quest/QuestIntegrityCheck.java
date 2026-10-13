package yifei.frliliens.magic.quest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.config.Config;

/**
 * 启动自检：核对代码与数据文件里手写的资源 id 是否真实存在。
 *
 * <p>这些 id 都是字符串字面量，写错了不会编译报错，只会在运行期静默失效：
 * <ul>
 *   <li>{@code BuiltInRegistries.ITEM.get()} 对未知 id 返回 AIR——玩家拿到空气</li>
 *   <li>loot modifier 的物品用 {@code ItemStack.OPTIONAL_CODEC}，未知 id 解成
 *       {@code ItemStack.EMPTY}——箱子里多出一个空栈，且日志里没有任何提示</li>
 * </ul>
 * 所以在 common setup 阶段主动查一遍注册表并打日志。
 */
public final class QuestIntegrityCheck {

    /** 战利品注入文件所在目录（相对数据包根）。 */
    private static final String LOOT_MODIFIER_DIR = "loot_modifiers";

    private QuestIntegrityCheck() {
    }

    /** 执行全部自检，返回发现的问题条数。 */
    public static int run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int spells = 0;

        // ---- 委托目标与奖励 ----
        // 只校验「被数据包改动过或新增」的条目：
        //  * 内置默认值的 id 已经人工核对过，不必每次启动刷一遍
        //  * 数据包写的 id 才容易出错，也正是需要报出来的
        // 判断方法：与 BuiltinQuestPools 里同 id 的条目逐字段比对，不同即为数据包所改。
        Set<String> namespaces = QuestPoolAnnouncer.loadedNamespaces();
        for (QuestPoolEntry.Target target : QuestRegistry.allKillTargets()) {
            if (isCustomTarget(target, namespaces)) {
                checkEntity(target.target(), "击杀目标[" + target.id() + "]", bad);
            }
        }
        for (QuestPoolEntry.Target target : QuestRegistry.allCollectTargets()) {
            if (isCustomTarget(target, namespaces)) {
                checkItem(target.target(), "收集目标[" + target.id() + "]", bad);
            }
        }
        for (QuestPoolEntry.Reward reward : QuestRegistry.allRewards()) {
            if (isCustomReward(reward, namespaces)) {
                checkItem(reward.item(), "委托奖励[" + reward.id() + "]", bad);
            }
        }
        if (!namespaces.isEmpty()) {
            FriliensMagic.LOGGER.info("自检范围：数据包 {} 的条目 + 全部内置默认值已跳过",
                    namespaces);
        }

        // ---- 战利品注入文件 ----
        int lootFiles = 0;
        int lootEntries = 0;
        if (server != null) {
            int[] counts = checkLootModifiers(server, bad);
            lootFiles = counts[0];
            lootEntries = counts[1];
        } else {
            // 正常路径不会走到这里：调用方是 ServerStartedEvent，服务端必然已就绪
            bad.add("自检未拿到服务端实例，loot_modifiers 未经校验");
        }

        // ---- 法术数值配置 ----
        // 新增法术若忘了在 SpellTuning 里登记，会静默退回兜底值（0 消耗 / 20 冷却）
        for (var spell : yifei.frliliens.magic.spell.SpellRegistry.getAll()) {
            spells++;
            if (!yifei.frliliens.magic.spell.SpellTuning.hasSettings(spell.getId())) {
                bad.add("法术缺少数值配置: " + spell.getId()
                        + "（请在 SpellTuning.BY_ID 中登记）");
            }
        }

        // 扫到的条目数也打出来：只报 "passed" 时无法区分「全都对」和「什么都没扫到」
        if (bad.isEmpty()) {
            FriliensMagic.LOGGER.info(
                    "Integrity check passed ({} 条任务 id, {} 个法术数值,"
                            + " {} 个战利品文件 / {} 条物品 id)",
                    QuestRegistry.allKillTargets().size()
                            + QuestRegistry.allCollectTargets().size()
                            + QuestRegistry.allRewards().size(),
                    spells, lootFiles, lootEntries);
        } else {
            FriliensMagic.LOGGER.error("Integrity check found {} problem(s):", bad.size());
            for (String b : bad) {
                FriliensMagic.LOGGER.error("  - {}", b);
            }
        }

        dumpConfigLangKeys();
        return bad.size();
    }

    /**
     * 把全部配置项的语言键导出到 {@code run/config-lang-keys.txt}。
     *
     * <p>配置词条靠字符串拼接，手写容易漏。导出权威列表后交给
     * {@code tools/gen_config_lang.py} 生成中英词条，就能保证不漏不重。
     * 只想看结果的话可以删掉这次调用。
     */
    private static void dumpConfigLangKeys() {
        try {
            java.nio.file.Path out = java.nio.file.Path.of("config-lang-keys.txt");
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (var entry : Config.SPEC.getValues().entrySet()) {
                String path = entry.getKey();
                if (path == null || path.isEmpty()) {
                    continue;
                }
                keys.add("friliensmagic.configuration." + path.substring(path.lastIndexOf('.') + 1));
            }
            java.util.Collections.sort(keys);
            java.nio.file.Files.write(out, keys);
            FriliensMagic.LOGGER.info("已导出 {} 个配置语言键到 {}", keys.size(), out.toAbsolutePath());
        } catch (Exception e) {
            FriliensMagic.LOGGER.warn("导出配置语言键失败: {}", e.getMessage());
        }
    }

    /**
     * 扫描本模组的 loot_modifiers/*.json，核对其中引用的物品 id。
     *
     * @return {@code [扫描到的文件数, 校验过的物品条目数]}
     */
    private static int[] checkLootModifiers(MinecraftServer server, List<String> bad) {
        var resourceManager = server.getResourceManager();
        var found = resourceManager.listResources(LOOT_MODIFIER_DIR,
                rl -> rl.getNamespace().equals(FriliensMagic.MODID));

        int files = 0;
        int entries = 0;
        for (var entry : found.entrySet()) {
            String file = entry.getKey().toString();
            // 1.21 的 listResources 每个 id 只返回一个 Resource（资源包已合并）
            Resource resource = entry.getValue();
            files++;
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    continue;
                }
                JsonObject obj = root.getAsJsonObject();
                if (!obj.has("items") || !obj.get("items").isJsonArray()) {
                    continue;
                }
                JsonArray items = obj.getAsJsonArray("items");
                for (JsonElement el : items) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = el.getAsJsonObject();
                    if (item.has("id")) {
                        entries++;
                        checkItem(item.get("id").getAsString(), "战利品 " + file, bad);
                    }
                }
            } catch (Exception e) {
                bad.add("战利品文件解析失败 " + file + ": " + e.getMessage());
            }
        }
        return new int[] { files, entries };
    }

    private static void checkItem(String id, String where, List<String> bad) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            bad.add(where + " id 格式非法: " + id);
            return;
        }
        // AIR 是未知 id 的返回值，所以要排除掉真正想引用 air 的情况
        if (BuiltInRegistries.ITEM.get(rl) == Items.AIR
                && !rl.equals(ResourceLocation.withDefaultNamespace("air"))) {
            bad.add(where + " 物品不存在: " + id);
        }
    }

    private static void checkEntity(String id, String where, List<String> bad) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            bad.add(where + " id 格式非法: " + id);
            return;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            bad.add(where + " 实体不存在: " + id);
        }
    }

    // ===== 判定条目是否来自数据包 =====

    /**
     * 该击杀/收集条目是否被数据包改动过或新增。
     *
     * <p>与内置同 id 条目逐字段比对；内置没有这个 id 即为新增。
     * {@code namespaces} 为空（本次没有扫描到任何数据包命名空间）时一律返回 true，
     * 这样在「拿不到命名空间信息」的场合仍然全量校验，不会漏。
     */
    private static boolean isCustomTarget(QuestPoolEntry.Target target, java.util.Set<String> namespaces) {
        if (namespaces.isEmpty()) {
            return true;
        }
        var pool = target.type() == QuestType.KILL
                ? BuiltinQuestPools.killTargets()
                : BuiltinQuestPools.collectTargets();
        for (QuestPoolEntry.Target builtin : pool) {
            if (builtin.id().equals(target.id())) {
                return !builtin.equals(target);
            }
        }
        return true;
    }

    /** 该奖励条目是否被数据包改动过或新增。语义同 {@link #isCustomTarget}。 */
    private static boolean isCustomReward(QuestPoolEntry.Reward reward, java.util.Set<String> namespaces) {
        if (namespaces.isEmpty()) {
            return true;
        }
        for (QuestPoolEntry.Reward builtin : BuiltinQuestPools.rewards()) {
            if (builtin.id().equals(reward.id())) {
                return !builtin.equals(reward);
            }
        }
        return true;
    }
}
