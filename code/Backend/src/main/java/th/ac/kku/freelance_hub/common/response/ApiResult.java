package th.ac.kku.freelance_hub.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Shared response envelope for API endpoints that return a body. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private String message;
    private T data;
    private PaginationMeta meta;
    private ApiError error;

    public static <T> ApiResult<T> success(String message, T data) {
        return success(message, data, null);
    }

    public static <T> ApiResult<T> success(String message, T data, PaginationMeta meta) {
        return ApiResult.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(meta)
                .error(null)
                .build();
    }

    public static ApiResult<Void> error(String message, String code, Object details) {
        return ApiResult.<Void>builder()
                .success(false)
                .message(message)
                .data(null)
                .meta(null)
                .error(new ApiError(code, details))
                .build();
    }
}
