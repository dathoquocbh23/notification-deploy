package com.demo.notify.repo;

import com.demo.notify.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public interface TokenRepo extends JpaRepository<UserToken, Long> {
    List<UserToken> findByUserId(Long userId);
    Optional<UserToken> findByToken(String token);
    @Transactional void deleteByToken(String token);
}
