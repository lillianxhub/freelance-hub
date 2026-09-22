package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

/** Filters and pagination options for listing the current user's clients. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientFilterRequest {

    /** Null includes both active and archived clients. */
    private ClientStatus status;

    /** Optional text filter; blank input should be treated as no search. */
    @Size(max = 150, message = "Search must not exceed 150 characters")
    private String search;

    @Builder.Default
    @Min(value = 0, message = "Page must be zero or greater")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private int size = 20;

    /** Allow only known Client properties as sort keys. */
    @Builder.Default
    @NotBlank(message = "Sort field is required")
    @Pattern(
        regexp = "name|companyName|email|createdAt|updatedAt",
        message = "Sort field is not supported"
    )
    private String sortBy = "name";

    @Builder.Default
    @NotNull(message = "Sort direction is required")
    private Sort.Direction direction = Sort.Direction.ASC;
}
