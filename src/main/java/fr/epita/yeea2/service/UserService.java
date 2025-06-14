package fr.epita.yeea2.service;

import fr.epita.yeea2.entity.AppUser;
import fr.epita.yeea2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public AppUser register(AppUser user) {
        return userRepository.save(user);
    }

    public AppUser findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}


