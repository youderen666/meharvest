# ME 农收机（ME Harvest Machine）

一个 **Applied Energistics 2（AE2）** 附属模组：在机器周围自动收割成熟作物并原地补种，产物直接送进 ME 网络。

- Minecraft **1.20.1**
- Forge **47.x**
- AE2 **15.x**（开发时基于 15.4.12）

---

## 功能

### 自动收割

- 以机器为中心、半径 3~16 格（水平）× ±3 格（垂直）扫描作物
- 只收成熟的，收完**原地补种**；种子优先从掉落物里取，不够再从 ME 网络抽
- 产物直接 `insert` 进 ME 网络；网络满了会先放进 9 格内部缓冲，再慢慢重试
- 自适应检查间隔：连续没收获就逐步拉长间隔，避免空转扫区块
- 内置位置索引，每 1200 tick 重建一次，作物被破坏后自动从索引里剔除

### 作物兼容

除了原版作物，额外处理了这些特殊作物：

| 类型 | 说明 |
| --- | --- |
| 多格同 ID 作物 | 森罗物语水稻（`location` 0/1/2），只认最下面一格，从顶部整列拆除后补回底部 |
| 多格不同 ID 作物 | 农夫乐事水稻（`rice` + `rice_panicles`），上半部分按正常掉落结算 |
| 藤蔓类作物 | 葡萄藤一类：成熟看 `grape` 属性，收完把属性归零，不需要种子 |

### 能量

- **AE**：接入 ME 网络即可供电，机器会持续把电充进**内置 10 kAE 缓存**，收割时从自己的缓存扣（每株 6 AE）
- **FE**：机器也暴露 Forge Energy 接口，任何 FE 线缆/管道都能直接给它充电，充进去的电同样进那个 10 kAE 缓存
- 待机功耗 0.2 AE/t，需要 1 个频道

### 悬浮提示（Jade / WTHIT / TOP）

和 AE2 充能器同款：把鼠标悬停在机器上就能看到

```
ME 农收机
已存储：1.2 kAE / 10 kAE
设备在线
```

设备在线 / 设备离线 / 设备缺少频道 这三种状态由 AE2 自带的 `GridNodeStateDataProvider` 提供，
电量行由 `PowerStorageDataProvider` 提供 —— 本模组通过 AE2 的 `ServiceLoader` 钩子
（`META-INF/services/appeng.api.integrations.igtooltip.TooltipProvider`）把它们登记到自己的方块实体上。

### 机器界面

右键打开，可以调：

- 范围半径（3~16）
- 检查间隔（20~400 tick）
- 每轮最多收割数量（1~16）
- **运行开关**：关掉后不再收割，但仍然会充电、仍然会把缓冲区里的产物送进网络

界面下方显示电量条、当前状态、已索引作物数量、缓冲区占用。

---

## 安装

1. 安装 Minecraft 1.20.1 + Forge 47.x
2. 安装 [Applied Energistics 2](https://modrinth.com/mod/ae2) 15.x
3. 把 `meharvest-*.jar` 丢进 `mods/` 目录
4. 可选：装 Jade / WTHIT / TOP 之一，才能看到悬浮提示

## 从源码构建

需要 JDK 17。

```bash
./gradlew build
```

产物在 `build/libs/meharvest-1.0.0.jar`。

> `gradle.properties` 里的代理配置默认是注释掉的。如果你的网络需要代理才能拉依赖，
> 把 `systemProp.http(s).proxyHost/Port` 四行取消注释并改成自己的端口。

## 开发环境

| 项 | 值 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.22 |
| Mappings | official |
| AE2 | 15.4.12 |

核心实现都在 `HarvestFlagBlockEntity`：

- 继承 AE2 的 `AENetworkPowerBlockEntity` —— 它自带**世界内网格节点**（`setInWorldNode(true)`）、
  `IAEPowerStorage` 内置电量、以及 Forge Energy 适配器。自己手写 `IManagedGridNode` 时
  千万别漏了 `setInWorldNode(true)`，否则节点是普通 `GridNode` 而不是 `InWorldGridNode`，
  `getGridNode(side)` 永远返回 null，**线缆一根都连不上**。
- 收割 / 补种 / 索引逻辑在 `logic/` 下：`HarvestRules`、`ReplantHelper`、`CropCompat`

## 协议

MIT，见 [LICENSE](LICENSE)。
