package me.jangsalt.sampling.feature.user.controller;

import lombok.RequiredArgsConstructor;
import me.jangsalt.sampling.feature.user.dto.UserDTO;
import me.jangsalt.sampling.feature.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO.Response> getUsers(@ModelAttribute UserDTO.Search search) {
        return userService.search(search);
    }

    @GetMapping("/{id}")
    public UserDTO.Response getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    public UserDTO.Response createUser(@RequestBody UserDTO.Request request) {
        return userService.create(request);
    }
}
