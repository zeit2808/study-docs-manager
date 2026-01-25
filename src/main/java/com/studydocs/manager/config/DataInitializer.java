package com.studydocs.manager.config;

import com.studydocs.manager.entity.Role;
import com.studydocs.manager.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Tạo các role mặc định nếu chưa tồn tại
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role("ADMIN", "System Administrator");
            roleRepository.save(adminRole);
            System.out.println("Created ADMIN role");
        }

        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role("USER", "Default User");
            roleRepository.save(userRole);
            System.out.println("Created USER role");
        }
    }
}