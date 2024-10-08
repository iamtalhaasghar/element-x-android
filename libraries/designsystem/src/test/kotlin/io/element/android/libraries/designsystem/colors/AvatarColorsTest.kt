/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.colors

import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.avatarColorsDark
import io.element.android.compound.theme.avatarColorsLight
import org.junit.Test

class AvatarColorsTest {
    @Test
    fun `ensure the size of the avatar color are equal for light and dark theme`() {
        assertThat(avatarColorsDark.size).isEqualTo(avatarColorsLight.size)
    }

    @Test
    fun `compute string hash`() {
        assertThat("@alice:domain.org".toHash()).isEqualTo(6)
        assertThat("@bob:domain.org".toHash()).isEqualTo(3)
        assertThat("@charlie:domain.org".toHash()).isEqualTo(0)
    }

    @Test
    fun `compute string hash reverse`() {
        assertThat("0".toHash()).isEqualTo(0)
        assertThat("1".toHash()).isEqualTo(1)
        assertThat("2".toHash()).isEqualTo(2)
        assertThat("3".toHash()).isEqualTo(3)
        assertThat("4".toHash()).isEqualTo(4)
        assertThat("5".toHash()).isEqualTo(5)
        assertThat("6".toHash()).isEqualTo(6)
        assertThat("7".toHash()).isEqualTo(7)
    }
}
