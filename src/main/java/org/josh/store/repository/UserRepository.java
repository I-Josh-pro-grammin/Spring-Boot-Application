package org.josh.store.repository;

import org.josh.store.Dtos.UserDto;
import org.josh.store.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface UserRepository extends JpaRepository<User, UUID> {

    public User findByEmail(String email);
}
