package com.soscall.repository;

import com.soscall.model.Binding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BindingRepository extends JpaRepository<Binding, String> {

    /** 查找两个用户之间的绑定关系 */
    @Query("SELECT b FROM Binding b WHERE " +
           "(b.userId = :user1 AND b.boundUserId = :user2) OR " +
           "(b.userId = :user2 AND b.boundUserId = :user1)")
    Optional<Binding> findBindingBetween(@Param("user1") String user1, @Param("user2") String user2);

    /** 查找用户相关的所有绑定 */
    @Query("SELECT b FROM Binding b WHERE b.userId = :userId OR b.boundUserId = :userId")
    List<Binding> findBindingsForUser(@Param("userId") String userId);

    /** 查找用户已接受的绑定 */
    @Query("SELECT b FROM Binding b WHERE (b.userId = :userId OR b.boundUserId = :userId) AND b.status = 'accepted'")
    List<Binding> findAcceptedBindingsForUser(@Param("userId") String userId);

    /** 查找待用户处理的绑定请求（别人发给该用户的） */
    List<Binding> findByBoundUserIdAndStatus(String boundUserId, String status);
}
