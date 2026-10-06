package com.folkify.entitlement.repository;

import com.folkify.entitlement.entity.UserAiUsage;
import com.folkify.entitlement.entity.UserAiUsageId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAiUsageRepository extends JpaRepository<UserAiUsage, UserAiUsageId> {

    /** Tạo row đếm cho kỳ nếu chưa có — an toàn khi nhiều request đồng thời. */
    @Modifying
    @Query(value = "INSERT INTO user_ai_usage (user_id, period, used_count, updated_at) "
            + "VALUES (:userId, :period, 0, NOW()) ON CONFLICT (user_id, period) DO NOTHING",
            nativeQuery = true)
    void insertIfAbsent(@Param("userId") UUID userId, @Param("period") String period);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAiUsage u WHERE u.id = :id")
    Optional<UserAiUsage> findForUpdate(@Param("id") UserAiUsageId id);
}
