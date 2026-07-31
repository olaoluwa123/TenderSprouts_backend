package com.example.backend.security;

import com.example.backend.entity.Admin;
import com.example.backend.entity.Parent;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        ParentRepository parentRepository,
        AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("User inactive");
        }
        Long profileId = resolveProfileId(user);
        return new SecurityUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole(), profileId);
    }

    public SecurityUser loadSecurityUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new SecurityUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole(), resolveProfileId(user));
    }

    private Long resolveProfileId(User user) {
        return switch (user.getRole()) {
            case TEACHER -> teacherRepository.findByUserId(user.getId())
                .map(Teacher::getId)
                .orElse(null);
            case PARENT -> parentRepository.findByUserId(user.getId())
                .map(Parent::getId)
                .orElse(null);
            case ADMIN -> adminRepository.findByUserId(user.getId())
                .map(Admin::getId)
                .orElse(null);
        };
    }
}
