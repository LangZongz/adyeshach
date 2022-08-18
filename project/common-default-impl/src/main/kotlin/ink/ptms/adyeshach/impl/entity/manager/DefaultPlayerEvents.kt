package ink.ptms.adyeshach.impl.entity.manager

import ink.ptms.adyeshach.api.AdyeshachSettings
import ink.ptms.adyeshach.api.event.AdyeshachPlayerJoinEvent
import ink.ptms.adyeshach.common.api.Adyeshach
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.nms.PacketReceiveEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Adyeshach
 * ink.ptms.adyeshach.impl.entity.manager.DefaultPlayerEvents
 *
 * @author 坏黑
 * @since 2022/8/18 10:41
 */
internal object DefaultPlayerEvents {

    val onlinePlayerMap = CopyOnWriteArrayList<String>()

    /**
     * 进入游戏初始化管理器
     */
    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        if (AdyeshachSettings.spawnTrigger == AdyeshachSettings.SpawnTrigger.JOIN) {
            submit(delay = 20) { Adyeshach.api().setupEntityManager(e.player) }
        }
    }

    /**
     * 进入游戏初始化管理器（延迟）
     */
    @SubscribeEvent
    fun onLateJoin(e: AdyeshachPlayerJoinEvent) {
        if (AdyeshachSettings.spawnTrigger == AdyeshachSettings.SpawnTrigger.KEEP_ALIVE) {
            Adyeshach.api().setupEntityManager(e.player)
        }
    }

    /**
     * 离开游戏释放管理器
     */
    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        Adyeshach.api().releaseEntityManager(e.player)
    }

    /**
     * 延迟进入检查器
     */
    @SubscribeEvent
    fun onReceive(e: PacketReceiveEvent) {
        if (e.packet.name == "PacketPlayInPosition" && e.player.name !in onlinePlayerMap) {
            onlinePlayerMap.add(e.player.name)
            AdyeshachPlayerJoinEvent(e.player).call()
        }
    }
}