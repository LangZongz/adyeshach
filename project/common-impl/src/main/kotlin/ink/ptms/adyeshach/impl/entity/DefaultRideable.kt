package ink.ptms.adyeshach.impl.entity

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.Rideable
import ink.ptms.adyeshach.core.entity.StandardTags
import ink.ptms.adyeshach.core.event.AdyeshachEntityVehicleEnterEvent
import ink.ptms.adyeshach.core.event.AdyeshachEntityVehicleLeaveEvent
import ink.ptms.adyeshach.core.util.errorBy
import org.bukkit.entity.Player

/**
 * Adyeshach
 * ink.ptms.adyeshach.impl.entity.DefaultRideable
 *
 * @author 坏黑
 * @since 2022/6/20 00:32
 */
@Suppress("DuplicatedCode")
interface DefaultRideable : Rideable {

    override fun isVehicle(): Boolean {
        return getPassengers().isNotEmpty()
    }

    override fun hasVehicle(): Boolean {
        return getVehicle() != null
    }

    override fun getVehicle(): EntityInstance? {
        this as EntityInstance
        return manager?.getEntity { it.getPassengers().any { p -> p.uniqueId == uniqueId } }
    }

    override fun getPassengers(): List<EntityInstance> {
        this as DefaultEntityInstance
        return passengers.mapNotNull { manager?.getEntityByUniqueId(it) }
    }

    override fun addPassenger(vararg entity: EntityInstance) {
        this as DefaultEntityInstance
        // 单位管理器必须有效
        if (manager == null || entity.any { it.manager == null }) {
            errorBy("error-entity-manager-is-null")
        }
        // 单位管理器必须相同
        if (entity.any { it.manager != manager }) {
            errorBy("error-entity-manager-not-match")
        }
        entity.filter { it != this }.filterIsInstance<DefaultEntityInstance>().forEach { target ->
            // 避免循环骑乘
            target.passengers.remove(uniqueId)
            // 从载具中离开
            target.getVehicle()?.removePassenger(target)
            // 事件
            if (AdyeshachEntityVehicleEnterEvent(target, this).call()) {
                passengers.add(target.uniqueId)
                // 设置标签
                target.setPersistentTag(StandardTags.IS_IN_VEHICLE, "true")
            }
        }
        refreshPassenger()
    }

    override fun removePassenger(vararg entity: EntityInstance) {
        this as DefaultEntityInstance
        // 单位管理器必须有效
        if (manager == null || entity.any { it.manager == null }) {
            errorBy("error-entity-manager-is-null")
        }
        // 单位管理器必须相同
        if (entity.any { it.manager != manager }) {
            errorBy("error-entity-manager-not-match")
        }
        entity.filter { it != this }.filterIsInstance<DefaultEntityInstance>().forEach { target ->
            // 进行二次判断是否为乘客
            if (passengers.contains(target.uniqueId)) {
                // 事件
                if (AdyeshachEntityVehicleLeaveEvent(target, this).call()) {
                    passengers.remove(target.uniqueId)
                    // 移除标签
                    target.removePersistentTag(StandardTags.IS_IN_VEHICLE)
                    // 校准位置
                    manager?.getEntityByUniqueId(target.uniqueId)?.refreshPosition()
                }
            }
        }
        refreshPassenger()
    }

    override fun removePassenger(vararg id: String) {
        this as DefaultEntityInstance
        removePassenger(*getPassengers().filter { it.id in id }.toTypedArray())
    }

    override fun clearPassengers() {
        this as DefaultEntityInstance
        removePassenger(*getPassengers().toTypedArray())
    }

    override fun refreshPassenger(viewer: Player) {
        this as DefaultEntityInstance
        // 刷新自己
        Adyeshach.api().getMinecraftAPI().getEntityOperator().updatePassengers(viewer, index, *getPassengers().map { e -> e.index }.toIntArray())
        // 刷新坐骑
        getVehicle()?.refreshPassenger(viewer)
    }

    override fun refreshPassenger() {
        this as DefaultEntityInstance
        forViewers { refreshPassenger(it) }
    }
}