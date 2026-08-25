package com.hendrik.javaledgerapi.bootstrap;

import com.hendrik.javaledgerapi.config.DevAdminProperties;
import com.hendrik.javaledgerapi.model.User;
import com.hendrik.javaledgerapi.model.enums.Role;
import com.hendrik.javaledgerapi.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "ledger.dev-admin", name = "enabled", havingValue = "true")
public class DevAdminBootstrap implements ApplicationRunner {

    private final DevAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminBootstrap(DevAdminProperties properties,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(properties.email())
                .ifPresentOrElse(this::verifyExistingUser, this::createAdmin);
    }

    private void verifyExistingUser(User existingUser) {
        if (existingUser.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Dev admin email is already assigned to a non-admin user");
        }
    }

    private void createAdmin() {
        User admin = new User(null, properties.email(), passwordEncoder.encode(properties.password()),
                Role.ADMIN, null, null);

        try {
            userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException exception) {
            User existingUser = userRepository.findByEmail(properties.email())
                    .orElseThrow(() -> exception);
            verifyExistingUser(existingUser);
        }
    }
}
