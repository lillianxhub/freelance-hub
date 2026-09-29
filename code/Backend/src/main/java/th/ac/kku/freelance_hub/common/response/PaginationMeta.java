package th.ac.kku.freelance_hub.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pagination information, present only for paginated responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMeta {
    private int page;
    private int limit;
    private long total;
    private int totalPages;
}
