package th.ac.kku.freelance_hub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import th.ac.kku.freelance_hub.domain.entity.Client;
import th.ac.kku.freelance_hub.domain.entity.Project;
import th.ac.kku.freelance_hub.domain.entity.User;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import th.ac.kku.freelance_hub.dto.request.ChangeProjectStatusRequest;
import th.ac.kku.freelance_hub.dto.request.CreateProjectRequest;
import th.ac.kku.freelance_hub.exception.ClientNotFoundException;
import th.ac.kku.freelance_hub.exception.ProjectNotFoundException;
import th.ac.kku.freelance_hub.mapper.ProjectMapper;
import th.ac.kku.freelance_hub.repository.ClientRepository;
import th.ac.kku.freelance_hub.repository.ProjectRepository;
import th.ac.kku.freelance_hub.repository.UserRepository;
import th.ac.kku.freelance_hub.service.impl.ProjectServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    private ProjectServiceImpl service;
    private User owner;
    private Client client;

    @BeforeEach
    void setUp() {
        service = new ProjectServiceImpl(
                projectRepository,
                clientRepository,
                userRepository,
                new ProjectMapper()
        );

        owner = User.builder().id(OWNER_ID).build();
        client = new Client(owner, "Acme");
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
    }

    @Test
    void createsProjectOnlyWithOwnedClient() {
        when(userRepository.findById(OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(clientRepository.findByIdAndOwnerId(CLIENT_ID, OWNER_ID))
                .thenReturn(Optional.of(client));
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateProjectRequest request = CreateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Website")
                .targetMinutes(120)
                .build();

        var response = service.create(OWNER_ID, request);

        assertThat(response.getName()).isEqualTo("Website");
        assertThat(response.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PLANNED);

        verify(projectRepository).save(argThat(project ->
                project.getOwner() == owner
                        && project.getClient() == client
                        && project.getTargetMinutes() == 120
        ));
    }

    @Test
    void rejectsClientNotOwnedByUser() {
        when(userRepository.findById(OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(clientRepository.findByIdAndOwnerId(CLIENT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        CreateProjectRequest request = CreateProjectRequest.builder()
                .clientId(CLIENT_ID)
                .name("Website")
                .build();

        assertThatThrownBy(() -> service.create(OWNER_ID, request))
                .isInstanceOf(ClientNotFoundException.class);

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void doesNotReturnProjectOwnedByAnotherUser() {
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(OWNER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectRepository)
                .findByIdAndOwnerId(PROJECT_ID, OWNER_ID);
    }

    @Test
    void rejectsInvalidStatusTransitionWithoutSaving() {
        Project project = new Project(owner, client, "Website");
        when(projectRepository.findByIdAndOwnerId(PROJECT_ID, OWNER_ID))
                .thenReturn(Optional.of(project));

        ChangeProjectStatusRequest request =
                ChangeProjectStatusRequest.builder()
                        .status(ProjectStatus.COMPLETED)
                        .build();

        assertThatThrownBy(() ->
                service.changeStatus(OWNER_ID, PROJECT_ID, request)
        ).isInstanceOf(IllegalStateException.class);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        verify(projectRepository, never()).save(any(Project.class));
    }
}

