package com.example.todoschedule.domain.use_case.auth

import org.mindrot.jbcrypt.BCrypt
import javax.inject.Inject

/**
 * 验证明文密码和哈希是否匹配 Use Case
 *
 * 注意：该 Use Case 仅用于本地操作（如本地用户验证），
 * 对于远程认证，密码验证由服务器端完成
 */
class VerifyPasswordUseCase @Inject constructor() {
    operator fun invoke(password: String, storedHash: String?): Boolean {
        if (storedHash == null) {
            return false // 如果没有存储哈希，无法验证
        }
        return try {
            BCrypt.checkpw(password, storedHash)
        } catch (e: Exception) {
            // 处理潜在的 jBCrypt 异常，例如哈希格式无效
            // 在实际应用中可能需要更详细的日志记录
            false
        }
    }
} 