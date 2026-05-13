package thang.bida.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import thang.bida.model.ERole;
import thang.bida.model.Role;
import thang.bida.repository.RoleRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName(ERole.ADMIN).isEmpty()) {
                roleRepository.save(new Role(ERole.ADMIN));
            }
            if (roleRepository.findByName(ERole.STAFF).isEmpty()) {
                roleRepository.save(new Role(ERole.STAFF));
            }
            if (roleRepository.findByName(ERole.CUSTOMER).isEmpty()) {
                roleRepository.save(new Role(ERole.CUSTOMER));
            }
        };
    }
}
