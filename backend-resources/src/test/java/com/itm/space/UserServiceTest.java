package com.itm.space;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

public class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private Keycloak keycloak;

    private final UserRequest userRequest = new UserRequest(
            "Test1",
            "test2@test.test",
            "test3",
            "Test4",
            "Test5");

    @Test //проверка, что юзер создался корректно
    @WithMockUser(roles = "MODERATOR")
    public void createUserSuccess() {
        userService.createUser(userRequest);

        UsersResource users = keycloak.realm("ITM").users();
        String userID = users.search(userRequest.getUsername()).get(0).getId();

        //тест getUserById()
        UserResponse userResponse = userService.getUserById(UUID.fromString(userID));

        Assertions.assertEquals(userRequest.getFirstName(), userResponse.getFirstName());
        Assertions.assertEquals(userRequest.getLastName(), userResponse.getLastName());
        Assertions.assertEquals(userRequest.getEmail(), userResponse.getEmail());

        users.get(userID).remove();
    }

    @Test //попытка создать такого же юзера ещё раз должна бросать исключение BackendResourcesException
    @WithMockUser(roles = "MODERATOR")
    public void createUserException() {
        userService.createUser(userRequest);

        UsersResource users = keycloak.realm("ITM").users();
        String userID = users.search(userRequest.getUsername()).get(0).getId();

        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.createUser(userRequest));

        users.get(userID).remove();
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void getUserByIdUserNotFound() {
        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.getUserById(UUID.randomUUID()));
    }

}