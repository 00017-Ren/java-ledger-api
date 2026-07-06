package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static com.hendrik.javaledgerapi.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    UserRepository userRepository;

    @Test
    void findByEmail_returnsUser_whenEmailExists() {

        User testUser = new User();

        testUser.setEmail("test@mail.com");
        testUser.setPasswordHash("000000");
        testUser.setRole(USER);

        userRepository.save(testUser);
        Optional<User> foundUser = userRepository.findByEmail("test@mail.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());

    }



}


