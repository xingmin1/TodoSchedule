package com.example.todoschedule.domain.use_case.auth

import org.mindrot.jbcrypt.BCrypt
import javax.inject.Inject

/**
 * 将明文密码哈希化 Use Case
 */
class HashPasswordUseCase @Inject constructor() {
    operator fun invoke(password: String): String {
        // 使用 jBCrypt 生成盐并哈希密码
        // gensalt 的参数 log_rounds 控制计算强度，默认是 10，可以根据需要调整
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
} 