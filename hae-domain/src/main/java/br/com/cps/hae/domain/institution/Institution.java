package br.com.cps.hae.domain.institution;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.com.cps.hae.domain.shared.Guard;

/**
 * Unidade (Fatec). Define o limite de horas semanais de HAE que podem ser alocadas por semestre.
 */
public class Institution {

    public static final int MAX_WEEKLY_HOURS_LIMIT = 10_000;

    private final UUID id;
    private int code;
    private String name;
    private String address;
    private int weeklyHoursLimit;
    private boolean active;
    private final Instant createdAt;
    private final Long version;

    private Institution(UUID id, int code, String name, String address, int weeklyHoursLimit, boolean active,
            Instant createdAt, Long version) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = createdAt;
        this.version = version;
        this.active = active;
        update(code, name, address, weeklyHoursLimit);
    }

    public static Institution create(int code, String name, String address, int weeklyHoursLimit, Instant now) {
        return new Institution(UUID.randomUUID(), code, name, address, weeklyHoursLimit, true, now, null);
    }

    public static Institution restore(UUID id, int code, String name, String address, int weeklyHoursLimit,
            boolean active, Instant createdAt, Long version) {
        return new Institution(id, code, name, address, weeklyHoursLimit, active, createdAt, version);
    }

    public void update(int newCode, String newName, String newAddress, int newWeeklyHoursLimit) {
        this.code = Guard.range(newCode, 1, 999_999, "código");
        this.name = Guard.notBlank(newName, "nome", 150);
        this.address = Guard.notBlank(newAddress, "endereço", 255);
        changeWeeklyHoursLimit(newWeeklyHoursLimit);
    }

    public void changeWeeklyHoursLimit(int newLimit) {
        this.weeklyHoursLimit = Guard.range(newLimit, 0, MAX_WEEKLY_HOURS_LIMIT, "limite de horas");
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public UUID id() {
        return id;
    }

    public int code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    public int weeklyHoursLimit() {
        return weeklyHoursLimit;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Long version() {
        return version;
    }
}
