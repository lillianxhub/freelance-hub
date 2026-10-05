package th.ac.kku.freelance_hub.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

/** Machine-readable error information for an API response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String code;
    private Object details;
    private Integer status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant timestamp;
    private Map<String, String> fieldErrors;
    private String traceId;
}
