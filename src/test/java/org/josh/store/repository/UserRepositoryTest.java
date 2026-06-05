package org.josh.store.repository;

import org.josh.store.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByEmail() {
        // Arrange
        User user = new User("test@example.com", "testuser", "encoded_password", new Date());

        // Act
        User savedUser = userRepository.save(user);
        User foundUser = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFindByEmail_NotFound() {
        // Act
        User foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(foundUser).isNull();
    }
}
