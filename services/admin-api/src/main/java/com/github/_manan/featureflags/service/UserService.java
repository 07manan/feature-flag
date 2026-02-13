package com.github._manan.featureflags.service;

import com.github._manan.featureflags.dto.UpdateUserRequest;
import com.github._manan.featureflags.dto.UserDto;
import com.github._manan.featureflags.entity.User;
import com.github._manan.featureflags.exception.IllegalSelfOperationException;
import com.github._manan.featureflags.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request, UUID currentUserId) {
        if (request.getEnabled() != null && !request.getEnabled() && id.equals(currentUserId)) {
            throw new IllegalSelfOperationException("Cannot disable your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        user = userRepository.save(user);
        return UserDto.from(user);
    }

    @Transactional
    public void deleteUser(UUID id, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalSelfOperationException("Cannot delete your own account");
        }
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
