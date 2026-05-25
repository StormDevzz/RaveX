package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.client.OptionInstance
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object NoBob : Module("NoBob", "Removes walking view and hand bobbing", Category.RENDER) {
    private var originalBob = true

    @Suppress("UNCHECKED_CAST")
    private val bobOption: OptionInstance<Boolean>? by lazy {
        try {
            val field = net.minecraft.client.Options::class.java.getDeclaredField("bobView")
            field.isAccessible = true
            field.get(Minecraft.getInstance().options) as? OptionInstance<Boolean>
        } catch (e: Exception) {
            null
        }
    }

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        val opt = bobOption ?: return
        if (opt.get()) {
            opt.set(false)
        }
    }

    override fun onEnable() {
        val opt = bobOption ?: return
        originalBob = opt.get()
        opt.set(false)
    }

    override fun onDisable() {
        val opt = bobOption ?: return
        opt.set(originalBob)
    }
}
