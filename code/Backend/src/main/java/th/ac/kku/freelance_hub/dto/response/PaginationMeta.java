package th.ac.kku.freelance_hub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pagination metadata shared by API responses that return a collection. */
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
