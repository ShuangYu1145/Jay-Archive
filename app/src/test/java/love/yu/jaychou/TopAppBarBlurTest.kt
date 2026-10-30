package love.yu.jaychou

import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.TopAppBarBlur
import org.junit.Assert.assertEquals
import org.junit.Test

class TopAppBarBlurTest {
    @Test fun absentAndUnknownPreferencesKeepGaussianDefault() {
        assertEquals(TopAppBarBlur.GAUSSIAN, AppSettings().topAppBarBlur)
        assertEquals(TopAppBarBlur.GAUSSIAN, TopAppBarBlur.fromStored(null))
        assertEquals(TopAppBarBlur.GAUSSIAN, TopAppBarBlur.fromStored("unknown"))
    }

    @Test fun storedModesRoundTrip() {
        for (mode in TopAppBarBlur.entries) assertEquals(mode, TopAppBarBlur.fromStored(mode.name))
    }
}
