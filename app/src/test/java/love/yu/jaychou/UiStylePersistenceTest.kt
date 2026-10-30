package love.yu.jaychou

import love.yu.jaychou.data.UiStyle
import love.yu.jaychou.data.appearanceKey
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStylePersistenceTest {
    @Test fun existingStoredNamesAndKeysStayCompatible() {
        assertEquals(UiStyle.MIUIX, UiStyle.fromStored(null))
        assertEquals(UiStyle.MIUIX, UiStyle.fromStored("unknown"))
        assertEquals(UiStyle.MATERIAL3, UiStyle.fromStored("MATERIAL3"))
        assertEquals("theme", appearanceKey(UiStyle.MIUIX, "theme"))
        assertEquals("material3_theme", appearanceKey(UiStyle.MATERIAL3, "theme"))
    }

    @Test fun couiHasItsOwnPersistentAppearanceProfile() {
        assertEquals(listOf(UiStyle.MIUIX, UiStyle.COUI, UiStyle.MATERIAL3), UiStyle.entries.toList())
        assertEquals(UiStyle.COUI, UiStyle.fromStored("COUI"))
        for (name in listOf("theme", "monet", "custom_color", "album_layout")) {
            assertEquals(3, UiStyle.entries.map { appearanceKey(it, name) }.toSet().size)
            assertEquals("coui_$name", appearanceKey(UiStyle.COUI, name))
        }
    }
}
