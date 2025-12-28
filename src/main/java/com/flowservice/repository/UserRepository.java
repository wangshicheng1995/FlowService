package com.flowservice.repository;

import com.flowservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository
 * 提供用户数据的 CRUD 操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据 Apple ID 查询用户
     *
     * @param appleId Apple Sign In 的用户标识
     * @return 用户对象（Optional）
     */
    Optional<User> findByAppleId(String appleId);

    /**
     * 检查 Apple ID 是否已存在
     *
     * @param appleId Apple Sign In 的用户标识
     * @return 是否存在
     */
    boolean existsByAppleId(String appleId);
}
