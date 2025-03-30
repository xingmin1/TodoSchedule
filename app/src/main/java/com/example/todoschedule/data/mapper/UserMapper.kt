package com.example.todoschedule.data.mapper

import com.example.todoschedule.data.database.entity.UserEntity
import com.example.todoschedule.domain.model.User

/** 将用户实体映射为用户领域模型 */
fun UserEntity.toUser(): User {
    return User(
        id = id,
        username = username,
        phoneNumber = phoneNumber,
        email = email,
        avatar = avatar,
        createdAt = createdAt,
        passwordHash = passwordHash,
        lastOpen = lastOpen,
        token = token
    )
}

/** 将用户领域模型映射为用户实体 */
fun User.toUserEntity(): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        phoneNumber = phoneNumber,
        email = email,
        avatar = avatar,
        createdAt = createdAt,
        passwordHash = passwordHash,
        lastOpen = lastOpen,
        token = token
    )
}
