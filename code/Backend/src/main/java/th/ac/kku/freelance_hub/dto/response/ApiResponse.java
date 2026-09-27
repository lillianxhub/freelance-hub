package th.ac.kku.freelance_hub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Common response envelope used by API controllers. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private PaginationMeta meta;
    private Object error;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .meta(null)
            .error(null)
            .build();
    }

    public static <T> ApiResponse<T> success(String message, T data, PaginationMeta meta) {
        ApiResponse<T> response = success(message, data);
        response.setMeta(meta);
        return response;
    }
}
