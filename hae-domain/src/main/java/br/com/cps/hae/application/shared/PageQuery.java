package br.com.cps.hae.application.shared;

/** Paginação solicitada (página baseada em zero). */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }
}
