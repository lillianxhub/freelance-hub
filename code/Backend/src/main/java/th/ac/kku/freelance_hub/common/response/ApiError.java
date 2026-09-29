package th.ac.kku.freelance_hub.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Machine-readable error information for an API response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String code;
    private Object details;
}
