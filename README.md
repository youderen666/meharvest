# ME 农收机（ME Harvest Machine）

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange)
![AE2](https://img.shields.io/badge/AE2-19.2.x-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

> 这是 **1.21.1 / NeoForge** 分支。1.20.1 / Forge 版本在 `main` 分支。

一个 **Applied Energistics 2（AE2）** 附属模组：在机器周围自动收割成熟作物并原地补种，产物直接送进 ME 网络。

- Minecraft **1.21.1**
- NeoForge **21.1.169+**
- AE2 **19.2.20+**（NeoForge 版）

---

## 功能

### 自动收割

- 以机器为中心、半径 3~16 格（水平）× ±3 格（垂直）扫描作物
- 只收成熟的，收完**原地补种**；种子优先从掉落物里取，不够再从 ME 网络抽
- 产物直接 `insert` 进 ME 网络；网络满了会先放进 9 格内部缓冲，再慢慢重试
- 自适应检查间隔：连续没收获就逐步拉长间隔，避免空转扫区块
- 滚动扫描索引：每 tick 检查一小片区域，新种下的作物几秒内就能被发现

### 作物兼容

| 类型 | 说明 |
| --- | --- |
| 右键收获类 | **优先尝试**用假玩家右键收获（触发方块 `use()` 和 NeoForge 的 `PlayerInteractEvent.RightClickBlock`），成功就采用它的产出、不破坏方块。把作物改成「可重复收获」的 mod 基本都挂在这条路上 |
| 多格同 ID 作物 | 森罗物语水稻（`location` 0/1/2），只认最下面一格，从顶部整列拆除后补回底部 |
| 多格不同 ID 作物 | 农夫乐事水稻（`rice` + `rice_panicles`），上半部分按正常掉落结算 |
| 藤蔓类作物 | 葡萄藤一类：成熟看 `grape` 属性，收完把属性归零，不需要种子 |
| 只摘果的作物 | 甜浆果丛、洞穴藤蔓：**不破坏本体**，只把生长状态回退（浆果丛 age 3→1，藤蔓 `berries`→`false`） |
| 瓜类的茎 | 西瓜/南瓜茎本身不产东西，拆掉反而毁掉结果能力，直接跳过 |

### 能量

- **AE**：接入 ME 网络即可供电，机器会持续把电充进**内置 10 kAE 缓存**，收割时从自己的缓存扣（每株 6 AE）
- **FE**：机器也暴露 `IEnergyStorage`，任何 FE 线缆/管道都能直接给它充电
- 待机功耗 0.2 AE/t，需要 1 个频道

### 悬浮提示（Jade / WTHIT / TOP）

和 AE2 充能器同款，悬停可看到「设备在线 / 设备离线 / 缺少频道」和「已存储：X / Y AE」。
本模组通过 AE2 的 `ServiceLoader` 钩子（`META-INF/services/appeng.api.integrations.igtooltip.TooltipProvider`）把 AE2 自带的两个 provider 登记到自己的方块实体上。

### 机器界面

右键打开，可调范围半径、检查间隔、每轮最多收割数量，并带一个**运行开关**（关掉后停止收割，但仍会充电、仍会把缓冲区送进网络）。界面下方显示电量条、状态、已索引作物数量和缓冲占用。

---

## 安装

1. Minecraft 1.21.1 + **NeoForge 21.1.169 或更高**
2. [Applied Energistics 2](https://modrinth.com/mod/ae2) **19.2.20+**（NeoForge 版）
3. [GuideME](https://modrinth.com/mod/guideme)（AE2 的前置，版本 21.1.x）
4. 把 `meharvest-1.21.1-*.jar` 丢进 `mods/`
5. 可选：Jade / WTHIT / TOP 之一，才能看到悬浮提示

## 从源码构建

需要 **JDK 21**。

```bash
# 1. 把 AE2 的 jar 放进 libs/（AE2 自己的 maven 在部分网络下不可达）
#    https://modrinth.com/mod/ae2/versions?g=1.21.1&l=neoforge
#    文件名：appliedenergistics2-19.2.20.jar

# 2. 构建
./gradlew build
```

产物在 `build/libs/meharvest-1.21.1-*.jar`。

> 缺少 `libs/appliedenergistics2-*.jar` 时编译会直接给出提示，不会报一堆找不到符号。
> `gradle.properties` 里的代理配置默认是注释掉的，需要时改成自己的端口。

## 开发备注

1.21.1 与 1.20.1 的 API 差异（移植时踩过的坑）：

| 1.20.1 (Forge) | 1.21.1 (NeoForge) |
| --- | --- |
| `AENetworkPowerBlockEntity` | `AENetworkedPoweredBlockEntity` |
| `BlockEntity#getCapability` + `LazyOptional` | `RegisterCapabilitiesEvent` 集中登记 |
| `Capabilities.IN_WORLD_GRID_NODE_HOST` | `AECapabilities.IN_WORLD_GRID_NODE_HOST`（`BlockCapability<IInWorldGridNodeHost, Void>`）——**不登记的话线缆一根都连不上** |
| FE 走 `getCapability` | `Capabilities.EnergyStorage.BLOCK` + `be.getEnergyStorage(side)` |
| `SimpleChannel` | `CustomPacketPayload` + `StreamCodec` + `RegisterPayloadHandlersEvent`，发送用 `PacketDistributor.sendToServer` |
| `RegistryObject` + `ForgeRegistries` | `DeferredHolder` / `DeferredBlock` + `Registries` |
| `IForgeMenuType` | `IMenuTypeExtension` |
| `FMLClientSetupEvent` 注册 Screen | `RegisterMenuScreensEvent` |
| `loadTag(CompoundTag)` | `loadTag(CompoundTag, HolderLookup.Provider)` |
| `ItemStack.of(tag)` / `stack.save(tag)` | `ItemStack.parseOptional(provider, tag)` / `stack.save(provider)` |
| `Block#use(...)` | `useWithoutItem(...)` + `codec()` |
| `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` |
| `data/.../loot_tables`、`recipes`、`tags/blocks` | `loot_table`、`recipe`、`tags/block` |
| 配方 `result.item` | `result.id` |

另外，`AEBaseBlockEntity` 里的 `AENetworkedPoweredBlockEntity` 构造时已经做了
`setInWorldNode(true)` / `addService(IAEPowerStorage)` / `setTagName`，所以子类只需要补
`setFlags` 和 `setIdlePowerUsage`。

## 协议

MIT，见 [LICENSE](LICENSE)。
