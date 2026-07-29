# CatFight Forge 1.20.1

适用于 **Minecraft 1.20.1 / Forge 47.x** 的猫咪对哈模组。两只猫靠近时会弓背、面对彼此、哈气并播放老吴音频。

## 功能

- 两只猫的精确距离不超过 5 格时进入对峙；对峙中不会乱跑，会面对对方并保持弓背姿势。
- 已驯服、原本坐下的猫也会正常显示对峙与弓背。
- 对峙时会随机播放 3 段老吴音频；每段播放 10 或 15 秒后自动切换，并避免连续重复同一段。
- 对峙期间会屏蔽原版猫叫；任意一只猫死亡、离场或距离超过 5 格时，自定义音频会立即停止。
- 猫有概率互相造成少量伤害。
- 新增 3 个物品：
  - **训猫棒**：攻击已驯服的猫后，禁止它对哈 3 分钟。
  - **小鱼干**：喂给已驯服的猫，可随机获得奖励。
  - **超级小鱼干**：永久禁止已驯服的猫对哈，并让它坐下。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.x。
2. 将构建产物 `catfight-1.3.13-forge-1.20.1.jar` 放进 `.minecraft/mods`。
3. 联机时服务端和所有客户端应安装相同版本。

## 从源码构建

需要 Java 17。

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs/`。

## 相关项目

- [ATLCNND/catfight-mod](https://github.com/ATLCNND/catfight-mod)
- [Rogic460/Minecraft-laowu-meme](https://github.com/Rogic460/Minecraft-laowu-meme)

本仓库为 Forge 1.20.1 整理版本；请在再发布音频、素材或上游代码前确认其对应授权。
模组图标来自 `Rogic460/Minecraft-laowu-meme`，其 MIT 许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
