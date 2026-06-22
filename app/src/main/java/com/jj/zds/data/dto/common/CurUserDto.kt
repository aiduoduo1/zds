package com.jj.zds.data.dto.common

data class CurUserDto(
    val id: String? = null,
    val userName: String? = null,
    val skills: List<String>? = null,
    val skiList: List<CurUserSkillDto>? = null,
)

data class CurUserSkillDto(
    val id: String? = null,
    val userId: String? = null,
    val skillId: String? = null,
    val skillLevel: String? = null,
    val activeAt: String? = null,
    val inactiveAt: String? = null,
    val confirmDate: String? = null,
    val confirmDepartment: String? = null,
    val status: Int? = null,
    val cmnFlg: String? = null,
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val skillName: String? = null,
    val distinct: Boolean? = null,
    val confirmDateLabel: String? = null,
    val inActiveAtLabel: String? = null,
    val activeAtLabel: String? = null,
)

data class UserSkillRequest(
    var account: String? = null,
    var status: Int = 1,
    var distinct: Boolean = true,
)
