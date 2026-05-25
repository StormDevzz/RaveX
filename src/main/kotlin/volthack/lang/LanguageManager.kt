package volthack.lang

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import volthack.VoltHack
import java.io.InputStreamReader
import java.nio.file.Path

object LanguageManager {
    private val gson = Gson()
    private val entries = mutableMapOf<String, String>()
    private var currentLang = "en_us"

    private val customDir: Path by lazy {
        FabricLoader.getInstance().gameDir.resolve("VoltHack/lang")
    }

    fun initialize() {
        load("en_us")
        customDir.toFile().mkdirs()
        VoltHack.LOGGER.info("Language manager ready: ${entries.size} entries loaded")
    }

    fun load(lang: String) {
        val path = "assets/volthack/lang/$lang.json"
        val stream = javaClass.classLoader.getResourceAsStream(path)
        if (stream == null) {
            VoltHack.LOGGER.warn("Language file not found: $lang")
            return
        }

        val type = object : TypeToken<Map<String, String>>() {}.type
        val map: Map<String, String> = gson.fromJson(InputStreamReader(stream), type)
        entries.putAll(map)
    }

    fun get(key: String): String = entries[key] ?: key

    fun get(key: String, vararg args: Any?): String {
        val template = entries[key] ?: return key
        return template.format(*args)
    }

    fun setLanguage(lang: String) {
        currentLang = lang
        entries.clear()
        load(lang)
    }
}
