package com.jj.zds.data.dto.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CurUserDtoTest {

    @Test
    fun userCarriesSkillListForQualificationCheck() {
        val user = CurUserDto(
            id = "USER-01",
            userName = "张三",
            skiList = listOf(
                CurUserSkillDto(
                    skillId = "45596243824660480",
                    skillName = "检查",
                ),
            ),
        )

        assertEquals("45596243824660480", user.skiList?.first()?.skillId)
        assertEquals("检查", user.skiList?.first()?.skillName)
    }
}
