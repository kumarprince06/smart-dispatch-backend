package com.smartdispatch.auth.repository;

import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}
