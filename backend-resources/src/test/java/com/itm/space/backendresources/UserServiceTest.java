package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

public class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private Keycloak keycloak;

    private final UserRequest userRequest = new UserRequest(
            "Test username",
            "email@test.test",
            "test",
            "Test firstname",
            "Test lastname");

    @Test
    @Order(1)
    @WithMockUser(roles = "MODERATOR")
    public void createUserSuccess() {
        userService.createUser(userRequest);

        UsersResource users = keycloak.realm("ITM").users();
        String userID = users.search(userRequest.getUsername()).get(0).getId();

        UserResponse userResponse = userService.getUserById(UUID.fromString(userID));

        Assertions.assertEquals(userRequest.getFirstName(), userResponse.getFirstName());
        Assertions.assertEquals(userRequest.getLastName(), userResponse.getLastName());
        Assertions.assertEquals(userRequest.getEmail(), userResponse.getEmail());
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "MODERATOR")
    public void createUserException() {
        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.createUser(userRequest));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "MODERATOR")
    public void deleteUserSuccess() {
        UsersResource users = keycloak.realm("ITM").users();
        String userId = users.search(userRequest.getUsername()).get(0).getId();
        users.get(userId).remove();

        Assertions.assertThrows(BackendResourcesException.class, () ->
            userService.getUserById(UUID.fromString(userId)));
    }


    @Test
    @Order(4)
    @WithMockUser(roles = "MODERATOR")
    public void getUserByIdUserNotFound() {
        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.getUserById(UUID.randomUUID()));
    }

}