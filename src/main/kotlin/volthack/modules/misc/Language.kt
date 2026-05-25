package volthack.modules.misc

import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.lang.LanguageManager

object Language : Module("Language", "Switches client translation language", Category.MISC, toggleable = false) {
    private val languageSetting by mode(
        "Language",
        listOf("English", "Russian"),
        "English",
        "Allows switching translation language",
        onChanged = { value ->
            val langCode = if (value == "Russian") "ru_ru" else "en_us"
            LanguageManager.setLanguage(langCode)
        }
    )
}
