package yifei.frliliens.magic.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.quest.QuestPoolEntry.Reward;
import yifei.frliliens.magic.quest.QuestPoolEntry.Target;

/**
 * 从数据包读取委托池条目，并按命名空间归组。
 *
 * <p>路径：{@code data/<命名空间>/friliensmagic/quest_pools/{kill,collect,rewards}.json}
 * <br>格式：{@code {"entries": [ {...}, {...} ]}}
 *
 * <p><b>合并规则</b>：先放内置默认值，再用数据包条目按 {@code id} 覆盖同名项、
 * 追加新项。数据包只需写要改的条目。
 *
 * <p><b>容错（一律不抛出，出问题就跳过并记日志）</b>：
 * <ul>
 *   <li>单条解析失败 → 只跳过该条，同文件其它条目照常</li>
 *   <li>整个文件读不出 → 该文件贡献 0 条，其它文件照常</li>
 *   <li>随后由 {@link QuestPoolReloadListener} 决定池为空时回退内置默认值</li>
 * </ul>
 */
final class QuestPoolReader {

    /** 数据包内子目录。 */
    static final String DIR = "friliensmagic/quest_pools";

    /** 一个池的读取结果：合并后的条目 + 有贡献的数据包命名空间。 */
    record PoolResult<T>(List<T> entries, Set<String> namespaces) {
    }

    private QuestPoolReader() {
    }

    /**
     * 读取一个池文件并与内置默认值合并。
     *
     * @param manager    资源管理器
     * @param fileName   kill.json / collect.json / rewards.json
     * @param codec      条目编解码器
     * @param filterType 期望的委托类型；{@code null} 表示不校验（奖励文件）
     * @param builtin    内置默认值
     * @param idOf       取条目 id 的函数（用于合并）
     * @param poolName   日志里显示的池名
     */
    static <T> PoolResult<T> read(net.minecraft.server.packs.resources.ResourceManager manager,
                                  String fileName, Codec<T> codec, QuestType filterType,
                                  List<T> builtin, java.util.function.Function<T, String> idOf,
                                  String poolName) {
        Map<String, T> merged = new LinkedHashMap<>();
        for (T t : builtin) {
            merged.put(idOf.apply(t), t);
        }

        Set<String> namespaces = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, Resource> entry : findFiles(manager, fileName).entrySet()) {
            String ns = entry.getKey().getNamespace();
            int added = readOne(entry.getValue(), entry.getKey().toString(), codec, filterType,
                    merged, idOf, builtin, poolName);
            if (added > 0) {
                namespaces.add(ns);
            }
        }
        return new PoolResult<>(List.copyOf(merged.values()), namespaces);
    }

    /** 枚举所有命名空间下的同名池文件。 */
    private static Map<ResourceLocation, Resource> findFiles(
            net.minecraft.server.packs.resources.ResourceManager manager, String fileName) {
        try {
            return manager.listResources(DIR, rl -> rl.getPath().endsWith("/" + fileName));
        } catch (Exception e) {
            FriliensMagic.LOGGER.warn("Failed to enumerate quest pool file {}: {}", fileName, e.toString());
            return Map.of();
        }
    }

    /**
     * 读一个文件并合并进 {@code merged}。
     *
     * @return 该文件成功贡献的条目数（含覆盖内置的），用于判断这个命名空间是否算「有参与」
     */
    private static <T> int readOne(Resource resource, String path, Codec<T> codec,
                                   QuestType filterType, Map<String, T> merged,
                                   java.util.function.Function<T, String> idOf,
                                   List<T> builtin, String poolName) {
        int added = 0;
        try (var reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                FriliensMagic.LOGGER.warn("Quest pool file {} is not a JSON object, skipping", path);
                return 0;
            }
            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("entries") || !obj.get("entries").isJsonArray()) {
                FriliensMagic.LOGGER.warn("Quest pool file {} has no \"entries\" array, skipping", path);
                return 0;
            }
            JsonArray array = obj.getAsJsonArray("entries");
            for (int i = 0; i < array.size(); i++) {
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, array.get(i));
                var parsed = result.result();
                if (parsed.isEmpty()) {
                    FriliensMagic.LOGGER.warn("Failed to parse quest pool entry {}[{}]: {}", path, i,
                            result.error().map(DataResult.Error::message).orElse("unknown error"));
                    continue;
                }
                T value = parsed.get();
                if (filterType != null && value instanceof Target t && t.type() != filterType) {
                    FriliensMagic.LOGGER.warn(
                            "Quest pool entry {}[{}] has type {} which does not match its file ({}), skipping",
                            path, i, t.type().serialName(), filterType.serialName());
                    continue;
                }
                String id = idOf.apply(value);
                T previous = merged.put(id, value);
                added++;
                if (previous != null) {
                    // 覆盖内置是正常用法（记 debug）；同层重复才值得警告
                    if (builtin.contains(previous)) {
                        FriliensMagic.LOGGER.debug("Quest pool {} entry \"{}\" overridden by {}",
                                poolName, id, path);
                    } else {
                        FriliensMagic.LOGGER.warn("Duplicate id \"{}\" in quest pool {}, overridden by {}",
                                id, poolName, path);
                    }
                }
            }
        } catch (Exception e) {
            FriliensMagic.LOGGER.warn("Failed to read quest pool file {}: {}", path, e.toString());
            return 0;
        }
        return added;
    }

    /** 空的命名空间集合，便于书写。 */
    static Set<String> noNamespaces() {
        return Set.of();
    }

    /** 合并多个池结果的命名空间。 */
    @SafeVarargs
    static Set<String> union(PoolResult<?>... results) {
        Set<String> out = new LinkedHashSet<>();
        for (PoolResult<?> r : results) {
            out.addAll(r.namespaces());
        }
        return out;
    }

    /** 便于调试：列出条目 id。 */
    static <T> List<String> idsOf(List<T> list, java.util.function.Function<T, String> idOf) {
        List<String> out = new ArrayList<>(list.size());
        for (T t : list) {
            out.add(idOf.apply(t));
        }
        return out;
    }
}
