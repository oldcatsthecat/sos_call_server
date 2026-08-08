package com.soscall.repository;

import com.soscall.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% AND u.id NOT IN :excludeIds")
    List<User> searchByUsername(@Param("keyword") String keyword, @Param("excludeIds") List<String> excludeIds);
}
