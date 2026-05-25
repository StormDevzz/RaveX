package volthack.setting

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class Setting<T>(
    val name: String,
    val description: String = "",
    defaultValue: T
) : ReadWriteProperty<Any?, T> {
    var onChanged: ((T) -> Unit)? = null

    var value: T = defaultValue
        set(newValue) {
            val changed = field != newValue
            field = newValue
            if (changed) {
                onChanged?.invoke(newValue)
            }
        }

    private var condition: () -> kotlin.Boolean = { true }

    fun showIf(cond: () -> kotlin.Boolean) { condition = cond }
    fun isVisible(): kotlin.Boolean = condition()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }

    class Boolean(
        name: String,
        description: String = "",
        defaultValue: kotlin.Boolean = false
    ) : Setting<kotlin.Boolean>(name, description, defaultValue)

    class Float(
        name: String,
        description: String = "",
        defaultValue: kotlin.Float = 0f,
        val min: kotlin.Float = 0f,
        val max: kotlin.Float = 1f,
        val step: kotlin.Float = 0.1f
    ) : Setting<kotlin.Float>(name, description, defaultValue)

    class Int(
        name: String,
        description: String = "",
        defaultValue: kotlin.Int = 0,
        val min: kotlin.Int = 0,
        val max: kotlin.Int = 10
    ) : Setting<kotlin.Int>(name, description, defaultValue)

    class Mode(
        name: String,
        description: String = "",
        defaultValue: String = "",
        val modes: List<String>
    ) : Setting<String>(name, description, defaultValue)

    class Color(
        name: String,
        description: String = "",
        defaultValue: kotlin.Int = 0xFF6C63FF.toInt()
    ) : Setting<kotlin.Int>(name, description, defaultValue)

    class StringSetting(
        name: String,
        description: String = "",
        defaultValue: kotlin.String = ""
    ) : Setting<kotlin.String>(name, description, defaultValue)
}
