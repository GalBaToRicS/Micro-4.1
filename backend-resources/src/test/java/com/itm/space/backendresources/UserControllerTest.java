package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerTest extends BaseIntegrationTest {

    @MockBean
    UserService userService;

    private final UserRequest validUserRequest = new UserRequest(
            "Test username",
            "email@test.test",
            "test",
            "Test firstname",
            "Test lastname");

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void getByIdSuccess() throws Exception {
        UserResponse user = new UserResponse(
                "test",
                "test",
                "test@test.test",
                List.of("default-roles-itm"),
                List.of("Moderators"));

        when(userService.getUserById(UUID.fromString("f37b9ce3-523c-408c-b8cf-121f506b4985")))
                .thenReturn(user);

        mvc.perform(get("/api/users/{id}", UUID.fromString("f37b9ce3-523c-408c-b8cf-121f506b4985"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("test"))
                .andExpect(jsonPath("$.lastName").value("test"))
                .andExpect(jsonPath("$.email").value("test@test.test"))
                .andExpect(jsonPath("$.roles[0]").value("default-roles-itm"))
                .andExpect(jsonPath("$.groups[0]").value("Moderators"));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void getByIdUserNotFound() throws Exception {
        when(userService.getUserById(any())).thenThrow(new BackendResourcesException("Exception", HttpStatus.INTERNAL_SERVER_ERROR));

        mvc.perform(get("/api/users/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getByIdNotAuthorized() throws Exception {
        mvc.perform(get("/api/users/{id}", UUID.fromString("f37b9ce3-523c-408c-b8cf-121f506b4985"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "other")
    public void getByIdIncorrectRoles() throws Exception {
        mvc.perform(get("/api/users/{id}", UUID.fromString("f37b9ce3-523c-408c-b8cf-121f506b4985"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createSuccess() throws Exception {
        mvc.perform(requestWithContent(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON), validUserRequest))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createException() throws Exception {
        doThrow(new BackendResourcesException("WebApplicationException", HttpStatus.CONFLICT))
                .when(userService).createUser(any());

        mvc.perform(requestWithContent(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON), validUserRequest))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createNotValidUsername() throws Exception {
        UserRequest invalidUserRequest = new UserRequest(
                "", //ошибка
                "email@test.test",
                "test",
                "Test firstname",
                "Test lastname");

        mvc.perform(requestWithContent(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON), invalidUserRequest))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "other")
    public void createIncorrectRoles() throws Exception {
        mvc.perform(requestWithContent(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON), validUserRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createNotAuthorized() throws Exception {
        mvc.perform(requestWithContent(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON), validUserRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void helloSuccess() throws Exception {
        mvc.perform(get("/api/users/hello")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));
    }

    @Test //валидация прав
    @WithMockUser(roles = "other")
    public void helloIncorrectRoles() throws Exception {
        mvc.perform(get("/api/users/hello")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void helloNotAuthorized() throws Exception {
        mvc.perform(get("/api/users/hello")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}