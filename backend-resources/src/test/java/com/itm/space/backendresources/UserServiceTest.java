package com.itm.space.backendresources;

import com.itm.space.backendresources.BackendResourcesApplication;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

//    @Autowired
//    private Keycloak keycloak;

    @MockBean
    private Keycloak keycloak;

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

    UserRequest userRequest = new UserRequest(
            "userTest",
            "userTest@test.test",
            "userTest",
            "userTest",
            "userTest"
    );

    @BeforeEach
    void setup() {
        when(keycloak.realm(anyString())).thenReturn(realmResource);
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

    @Test //проверка, что юзер создался корректно
    //@WithMockUser(roles = "MODERATOR")
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
    //@WithMockUser(roles = "MODERATOR")
    public void createUserException() {
        userService.createUser(userRequest);

        UsersResource users = keycloak.realm("ITM").users();
        String userID = users.search(userRequest.getUsername()).get(0).getId();

        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.createUser(userRequest));

        users.get(userID).remove();
    }

    @Test
    //@WithMockUser(roles = "MODERATOR")
    public void getUserByIdUserNotFound() {
        Assertions.assertThrows(
                BackendResourcesException.class, () -> userService.getUserById(UUID.randomUUID()));
    }

}