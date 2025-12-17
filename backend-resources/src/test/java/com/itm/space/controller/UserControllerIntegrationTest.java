package com.itm.space.controller;

import com.itm.space.backendresources.BackendResourcesApplication;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.parent.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;


import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendResourcesApplication.class)
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloakClient;

    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private Response response;

    @BeforeEach
    void setup() {
        when(keycloakClient.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(response);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(fakeUser());
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(fakeRoleMappings());
        when(userResource.groups()).thenReturn(fakeGroups());
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/" + UUID.randomUUID()));
    }

    private UserRepresentation fakeUser() {
        UserRepresentation u = new UserRepresentation();
        u.setId(UUID.randomUUID().toString());
        u.setUsername("userTest");
        u.setEmail("userTest@test.test");
        u.setFirstName("userTest");
        u.setLastName("userTest");
        u.setEnabled(true);
        return u;
    }

    private List<GroupRepresentation> fakeGroups() {
        GroupRepresentation g = new GroupRepresentation();
        g.setName("test-group");
        return List.of(g);
    }

    private MappingsRepresentation fakeRoleMappings() {
        RoleRepresentation role = new RoleRepresentation("MODERATOR", null, false);
        MappingsRepresentation mappings = new MappingsRepresentation();
        mappings.setRealmMappings(List.of(role));
        return mappings;
    }


    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenCreateUserThenReturn200() throws Exception {
        UserRequest userRequest = new UserRequest(
                "userTest",
                "userTest@test.test",
                "userTest",
                "userTest",
                "userTest"
        );
        mvc.perform(requestWithContent(post("/api/users"), userRequest))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenGetUserByIdThenReturn200() throws Exception {
        UUID userId = UUID.fromString("845b9bdc-d288-49c9-9417-11c2ae6b5362");

        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenGetHelloThenReturn200() throws Exception {
        mvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void whenUserWithoutModeratorRoleThenReturn403() throws Exception {
        mvc.perform(get("/api/users/hello"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenInvalidUserRequestThenReturn400() throws Exception {
        UserRequest invalidRequest = new UserRequest(
                "",
                "invaliEmail",
                "",
                "",
                ""
        );
        mvc.perform(requestWithContent(post("/api/users"), invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenUserNotFoundThenReturn404() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(get("/api/users" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenInvalidUserUUIDThenReturn400() throws Exception {
        mvc.perform(get("/api/users/invalidUUID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void whenBackendResourcesExceptionThrownThenReturnExpectedStatus() throws Exception {
        when(usersResource.get(anyString())).thenThrow(
                new BackendResourcesException("Test exception", HttpStatus.BAD_REQUEST)
        );

        mvc.perform(get("/api/users/{id}", UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Test exception"));
    }
}