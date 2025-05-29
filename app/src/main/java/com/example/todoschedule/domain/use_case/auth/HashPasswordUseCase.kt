package com.example.todoschedule.domain.use_case.auth

import org.mindrot.jbcrypt.BCrypt
import javax.inject.Inject

/**
 * 将明文密码哈希化 Use Case
 *
 * 注意：该 Use Case 仅用于本地操作（如本地用户验证），
 * 对于远程 API 调用，密码以明文形式发送，由服务器端负责哈希处理
 */
class HashPasswordUseCase @Inject constructor() {
    operator fun invoke(password: String): String {
        // 使用 jBCrypt 生成盐并哈希密码
        // gensalt 的参数 log_rounds 控制计算强度，默认是 10，可以根据需要调整
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
} 