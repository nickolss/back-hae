package br.com.cps.hae.web.api;

import java.util.List;

import br.com.cps.hae.application.shared.PageResult;

/** Página de resultados. */
record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    static <T> PageResponse<T> of(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.size(), result.totalElements(),
                result.totalPages());
    }
}
