# 委托系统 · 数据包自定义指南

本模组的委托目标与奖励池都可以用数据包完全自定义。改完执行 `/reload` 即可生效，**不需要重启游戏**，之后会有一条载入通报。

---

## 1. 文件位置

```
你的数据包/
├── pack.mcmeta                                   ← 标准数据包必需
├── data/
│   └── <命名空间>/
│       └── friliensmagic/
│           └── quest_pools/
│               ├── kill.json                     击杀目标
│               ├── collect.json                  收集目标
│               └── rewards.json                  奖励
└── assets/
    └── <命名空间>/
        └── lang/
            ├── zh_cn.json                        ← 可选：通报文案的翻译
            └── en_us.json
```

> **注意 `data/` 下面有两层目录**，这不是笔误：
> - `data/<命名空间>/` 第一层是数据包自己的命名空间
> - `friliensmagic/quest_pools/` 是模组约定的固定目录，**必须保持一致**
>
> 三个 json 互相独立，可以只提供其中一个。文件名必须精确是
> `kill.json` / `collect.json` / `rewards.json`。

**多个数据包共存**：所有命名空间下的同名文件都会被读取，各自覆盖/追加，互不干扰。

### pack.mcmeta

数据包**必须**有 `pack.mcmeta`，否则游戏不会识别。本示例用的内容：

```json
{
  "pack": {
    "description": "芙莉莲的魔法 · 委托系统示例数据包",
    "pack_format": 48
  }
}
```

`pack_format` 对应游戏版本：**1.21.1 是 48**。换版本时改这个数字。

---

## 2. 合并规则

| 情况 | 结果 |
|---|---|
| 数据包条目的 `id` 与内置同名 | **覆盖**内置那条（权重、数量全部替换） |
| 内置没有这个 `id` | **追加**为新条目 |
| 内置有、数据包没提到 | **保留**内置那条 |

所以**只需写想改的条目**，不必抄整套内置数据。

内置数据保存在模组的 Java 代码里（`BuiltinQuestPools`），不会因为你提供数据包而消失——这保证了数据包只写一部分时其余仍是完整内容，也保证加载失败时能回退。

---

## 3. 载入通报与翻译

### 通报行为

`/reload` 之后，如果**有新的数据包参与**，会向所有在线玩家广播一条消息：

```
已载入 2 个委托数据包：pack_a、pack_b
```

规则：

- **一次 `/reload` 只发一条**（聚合所有新命名空间），不会每个数据包刷一条
- **只通报新出现的命名空间**——数据包没变就不吵
- 把数据包删掉再放回来，**会重新通报**
- 池子被清空（数据包全删）时不通报，但会在服务器日志记一条

### 用自己的语言覆盖这条通报

把翻译放进 `assets/<任意命名空间>/lang/<语言>.json` 即可。客户端会遍历**所有**命名空间收集语言文件，所以你的翻译一定会被读到。

翻译键是固定的，与命名空间无关：

```json
{
  "friliensmagic.quest_pool_loaded": "§b已载入 %1$s 个委托数据包：§f%2$s"
}
```

- `%1$s` 数据包数量
- `%2$s` 命名空间列表，形如 `pack_a、pack_b`

**如果客户端没装你的数据包**（比如只装在服务端），翻译拿不到，游戏会用明文兜底：

```
已载入 1 个委托数据包：pack_a
```

不会显示生硬的翻译键。模组自带一份默认翻译，所以通常不需要你自己写。

---

## 4. 字段说明

### kill.json（击杀目标）

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 是 | 条目键，覆盖内置时用它；自定义条目名字随便起，建议小写下划线 |
| `type` | 是 | 固定 `"kill"`。写别的值会被跳过并记日志 |
| `target` | 是 | 实体注册 id，如 `minecraft:zombie` |
| `weight` | 否 | 权重，默认 `1`。越高越常见 |
| `min` | 是 | 最少击杀数量，≥ 1 |
| `max` | 是 | 最多击杀数量，必须 ≥ `min` |

### collect.json（收集目标）

与 `kill.json` 相同，但 `type` 固定 `"collect"`，`target` 填**物品**注册 id。

### rewards.json（奖励）

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | 条目键 |
| `item` | 是 | 物品注册 id（注意字段名是 `item`，不是 `target`） |
| `min` | 是 | 最少数量，≥ 1 |
| `max` | 是 | 最多数量，必须 ≥ `min` |
| `weight` | 否 | 权重，默认 `1` |

`rewards.json` 不需要 `type` 字段。

---

## 5. 示例

### kill.json

```json
{
  "entries": [
    {
      "id": "zombie",
      "type": "kill",
      "target": "minecraft:zombie",
      "weight": 6,
      "min": 10,
      "max": 25
    },
    {
      "id": "hoglin_custom",
      "type": "kill",
      "target": "minecraft:hoglin",
      "weight": 3,
      "min": 3,
      "max": 8
    }
  ]
}
```

- `zombie` 与内置同名 → **覆盖**（原本权重 10、数量 5~15）
- `hoglin_custom` 是内置没有的 id → **追加**

### collect.json

```json
{
  "entries": [
    {
      "id": "shaved_ice_custom",
      "type": "collect",
      "target": "friliensmagic:shaved_ice",
      "weight": 4,
      "min": 2,
      "max": 6
    }
  ]
}
```

`target` 也可以填本模组的物品，比如让玩家去收集刨冰。

### rewards.json

```json
{
  "entries": [
    {
      "id": "diamond",
      "item": "minecraft:diamond",
      "min": 2,
      "max": 5,
      "weight": 20
    },
    {
      "id": "netherite_ingot_custom",
      "item": "minecraft:netherite_ingot",
      "min": 1,
      "max": 1,
      "weight": 1
    }
  ]
}
```

---

## 6. 想让某个内置委托消失

把它的数量设成玩家不可能达到的数字：

```json
{ "id": "warden", "type": "kill", "target": "minecraft:warden",
  "weight": 1, "min": 1000, "max": 1000 }
```

（目前没有「删除条目」语法，这是最简单的等效做法。）

---

## 7. 内置条目 id 一览

覆盖时用这些键。

<details>
<summary><b>击杀目标（19 条）</b></summary>

`zombie` `skeleton` `spider` `creeper` `husk` `stray` `cave_spider`
`enderman` `witch` `drowned` `pillager` `vindicator` `blaze` `magma_cube`
`wither_skeleton` `ghast` `evoker` `ravager` `warden`

</details>

<details>
<summary><b>收集目标（22 条）</b></summary>

`iron_ingot` `coal` `redstone` `copper_ingot` `leather` `feather` `string`
`bone` `gunpowder`
`gold_ingot` `lapis_lazuli` `quartz` `ender_pearl` `blaze_rod` `book` `glass`
`diamond` `emerald` `netherite_scrap` `ancient_debris` `shulker_shell` `totem_of_undying`

</details>

<details>
<summary><b>奖励（23 条）</b></summary>

药水：`potion_lv1` `potion_lv2` `potion_lv3` `potion_lv4` `potion_lv5`

矿物：`iron_ingot` `gold_ingot` `redstone` `diamond` `emerald` `lapis_lazuli`

卷轴：`scroll_zoltraak` `scroll_defense` `scroll_flight` `scroll_mana_strike`
`scroll_seal` `scroll_blast` `scroll_ice` `scroll_float` `scroll_clean`
`scroll_judradjim` `scroll_vollzanbel` `scroll_black_light`

</details>

---

## 8. 内置默认值

供对照，方便决定改什么。

### 击杀目标

| id | 实体 | 权重 | 数量 |
|---|---|---|---|
| `zombie` | 僵尸 | 10 | 5-15 |
| `skeleton` | 骷髅 | 10 | 5-12 |
| `spider` | 蜘蛛 | 8 | 5-10 |
| `creeper` | 苦力怕 | 8 | 3-8 |
| `husk` | 尸壳 | 6 | 5-12 |
| `stray` | 流浪者 | 6 | 5-10 |
| `cave_spider` | 洞穴蜘蛛 | 5 | 3-8 |
| `enderman` | 末影人 | 5 | 3-6 |
| `witch` | 女巫 | 5 | 2-4 |
| `drowned` | 溺尸 | 4 | 3-8 |
| `pillager` | 掠夺者 | 4 | 3-8 |
| `blaze` | 烈焰人 | 4 | 2-5 |
| `vindicator` | 卫道士 | 3 | 2-5 |
| `magma_cube` | 岩浆怪 | 3 | 3-8 |
| `wither_skeleton` | 凋灵骷髅 | 2 | 1-4 |
| `ghast` | 恶魂 | 2 | 1-3 |
| `evoker` | 唤魔者 | 1 | 1-2 |
| `ravager` | 劫掠兽 | 1 | 1-2 |
| `warden` | 监守者 | 1 | 1-1 |

### 收集目标

| id | 物品 | 权重 | 数量 |
|---|---|---|---|
| `iron_ingot` | 铁锭 | 10 | 8-32 |
| `coal` | 煤炭 | 10 | 16-48 |
| `redstone` | 红石 | 8 | 16-40 |
| `copper_ingot` | 铜锭 | 6 | 8-24 |
| `leather` | 皮革 | 6 | 8-20 |
| `feather` | 羽毛 | 5 | 8-16 |
| `string` | 线 | 5 | 8-24 |
| `bone` | 骨头 | 5 | 8-24 |
| `gold_ingot` | 金锭 | 5 | 4-16 |
| `gunpowder` | 火药 | 4 | 4-12 |
| `lapis_lazuli` | 青金石 | 4 | 8-24 |
| `quartz` | 下界石英 | 4 | 8-24 |
| `ender_pearl` | 末影珍珠 | 3 | 2-6 |
| `blaze_rod` | 烈焰棒 | 3 | 2-6 |
| `book` | 书 | 3 | 4-12 |
| `glass` | 玻璃 | 3 | 8-24 |
| `diamond` | 钻石 | 2 | 1-5 |
| `emerald` | 绿宝石 | 2 | 2-6 |
| `netherite_scrap` | 下界合金碎片 | 1 | 1-2 |
| `ancient_debris` | 远古残骸 | 1 | 1-2 |
| `shulker_shell` | 潜影壳 | 1 | 1-3 |
| `totem_of_undying` | 不死图腾 | 1 | 1-1 |

### 奖励

| id | 物品 | 权重 | 数量 |
|---|---|---|---|
| `potion_lv1` | 初级魔力药水 | 30 | 2-5 |
| `potion_lv2` | 中级魔力药水 | 25 | 2-5 |
| `potion_lv3` | 高级魔力药水 | 15 | 1-3 |
| `iron_ingot` | 铁锭 | 15 | 4-12 |
| `gold_ingot` | 金锭 | 12 | 2-6 |
| `redstone` | 红石 | 10 | 8-16 |
| `diamond` | 钻石 | 8 | 1-3 |
| `emerald` | 绿宝石 | 8 | 2-5 |
| `lapis_lazuli` | 青金石 | 8 | 4-12 |
| `potion_lv4` | 超级魔力药水 | 6 | 1-3 |
| `potion_lv5` | 终极魔力药水 | 3 | 1-2 |
| `scroll_zoltraak` | 法术卷轴·攻击 | 4 | 1 |
| `scroll_defense` | 法术卷轴·防御 | 4 | 1 |
| `scroll_flight` | 法术卷轴·飞行 | 4 | 1 |
| `scroll_mana_strike` | 法术卷轴·魔力挤压 | 3 | 1 |
| `scroll_seal` | 法术卷轴·封印 | 3 | 1 |
| `scroll_blast` | 法术卷轴·爆破 | 3 | 1 |
| `scroll_ice` | 法术卷轴·刨冰 | 3 | 1 |
| `scroll_float` | 法术卷轴·漂浮 | 3 | 1 |
| `scroll_clean` | 法术卷轴·清洁 | 3 | 1 |
| `scroll_judradjim` | 法术卷轴·破灭之雷 | 1 | 1 |
| `scroll_vollzanbel` | 法术卷轴·地狱业火 | 1 | 1 |
| `scroll_black_light` | 法术卷轴·破坏黑光 | 1 | 1 |

**权重是相对值，不是百分比。** 奖励池总权重约 200，所以 `diamond` 权重 8 大约是 4% 的概率。

---

## 9. 容错行为

设计取向是**宁可跑默认内容，也不让委托系统瘫痪**。任何加载问题都不会导致功能不可用，最多是某些自定义没生效。

| 情况 | 结果 |
|---|---|
| 单条 JSON 写错（字段缺失、类型不对、`min > max`） | **只跳过该条** + 日志警告，同文件其它条目照常生效 |
| 整个文件读不出来（JSON 语法错） | 该文件贡献 0 条，其它文件照常 |
| 顶层不是对象 / 缺 `entries` 数组 | 跳过该文件 + 警告 |
| `type` 与所在文件不符（如在 `kill.json` 里写 `collect`） | 跳过该条 + 警告 |
| 某个池最终为空 | **自动回退模组内置默认值** + 警告 |
| 物品或实体 id 不存在 | 启动自检在日志里报出**具体是哪一条** |
| 同一条目 id 重复 | 后者覆盖前者 + 警告 |

排查时看日志里的这几条：

```
已加载委托池: 击杀 N 条, 收集 N 条, 奖励 N 条      ← 每次 reload 都打印
委托池条目解析失败 <文件>[索引]: <原因>             ← 单条失败
委托池 <名字> 中 id "xxx" 重复，后者覆盖了前一条
委托池存在空池，已回退内置默认值
已载入 N 个委托数据包: [命名空间]                   ← 通报
Integrity check passed (... )                      ← 启动自检，会报数据包里写错的 id
```

---

## 10. 性能

自定义数据包不会拖慢游戏：

- 池子在数据包重载时构建成**不可变快照**，并预计算权重前缀和
- 运行时按权重选取用**二分查找**，零内存分配，读路径无锁
- 只在服务器启动、`/reload`、数据包变动时解析，不在每 tick 路径上

---

## 11. 快速上手

**方式一：直接用现成的**

把 `examples/dist/friliensmagic_quest_pools_example.zip` 丢进
`saves/<世界>/datapacks/` 或服务器 `world/datapacks/`，然后游戏里 `/reload`。

**方式二：改成自己的**

1. 复制本文件夹作为起点
2. 改 `pack.mcmeta` 里的 `description`
3. 把 `data/friliensmagic_example/` 和 `assets/friliensmagic_example/` 重命名成你的命名空间
4. 改三个 json 的内容，删掉 `_comment` 字段
5. 放进 datapacks 目录，`/reload`
6. 看聊天栏是否出现通报，日志里确认 `已加载委托池: ...`

**重新打包成 zip**（在项目根目录）：

```powershell
python tools\pack_examples.py
```

产物在 `examples/dist/`。

> `_comment` 是 JSON 没有注释语法的变通写法，Minecraft 会忽略它，但它**不是**必需字段。
> 保留当自己的备注也可以。
