package volthack.setting

enum class AutoEnable {
    NORMAL,
    ONCE,
    ALWAYS
}

abstract class Module(
    val name: String,
    val description: String = "",
    val category: Category,
    val autoEnable: AutoEnable = AutoEnable.NORMAL,
    val toggleable: Boolean = true
) {
    var enabled = false
    var bindKey: Int = 0

    val settings = mutableListOf<Setting<*>>()

    var onStateChanged: ((Module) -> Unit)? = null

    fun toggle() {
        if (!toggleable) return
        if (enabled) {
            if (autoEnable == AutoEnable.ALWAYS) return
            disable()
        } else {
            enable()
        }
        onStateChanged?.invoke(this)
    }

    fun enable() {
        if (!toggleable) return
        if (enabled) return
        enabled = true
        onEnable()
        onStateChanged?.invoke(this)
        if (this.name != "Notifications" && volthack.modules.render.Notifications.enabled && volthack.modules.render.Notifications.notifyModules) {
            volthack.util.render.NotificationManager.add(
                "Module Enabled",
                "${this.name} has been enabled",
                volthack.util.render.NotificationType.SUCCESS,
                2000L
            )
        }
    }

    fun disable() {
        if (!enabled) return
        enabled = false
        onDisable()
        onStateChanged?.invoke(this)
        if (this.name != "Notifications" && volthack.modules.render.Notifications.enabled && volthack.modules.render.Notifications.notifyModules) {
            volthack.util.render.NotificationManager.add(
                "Module Disabled",
                "${this.name} has been disabled",
                volthack.util.render.NotificationType.ERROR,
                2000L
            )
        }
    }

    protected open fun onEnable() {}
    protected open fun onDisable() {}

    protected fun boolean(
        name: String,
        default: kotlin.Boolean = false,
        description: String = "",
        onChanged: ((kotlin.Boolean) -> Unit)? = null
    ) = Setting.Boolean(name, description, default).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }

    protected fun float(
        name: String,
        default: kotlin.Float = 0f,
        min: kotlin.Float = 0f,
        max: kotlin.Float = 1f,
        step: kotlin.Float = 0.1f,
        description: String = "",
        onChanged: ((kotlin.Float) -> Unit)? = null
    ) = Setting.Float(name, description, default, min, max, step).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }

    protected fun int(
        name: String,
        default: kotlin.Int = 0,
        min: kotlin.Int = 0,
        max: kotlin.Int = 10,
        description: String = "",
        onChanged: ((kotlin.Int) -> Unit)? = null
    ) = Setting.Int(name, description, default, min, max).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }

    protected fun mode(
        name: String,
        modes: List<String>,
        default: String = modes.firstOrNull() ?: "",
        description: String = "",
        onChanged: ((String) -> Unit)? = null
    ) = Setting.Mode(name, description, default, modes).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }

    protected fun color(
        name: String,
        default: kotlin.Int = 0xFFFFFFFF.toInt(),
        description: String = "",
        onChanged: ((kotlin.Int) -> Unit)? = null
    ) = Setting.Color(name, description, default).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }

    protected fun text(
        name: String,
        default: kotlin.String = "",
        description: String = "",
        onChanged: ((kotlin.String) -> Unit)? = null
    ) = Setting.StringSetting(name, description, default).also { 
        if (onChanged != null) it.onChanged = onChanged
        settings.add(it) 
    }
}

enum class Category(val displayName: String) {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    WORLD("World"),
    CONFIGS("Configs")
}
