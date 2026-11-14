package br.com.cps.hae.domain.shared;

import java.time.LocalDate;
import java.time.Month;

/**
 * Semestre letivo: 1º (jan–jun) ou 2º (jul–dez) de um ano.
 */
public record Semester(int year, int half) implements Comparable<Semester> {

    public Semester {
        if (half != 1 && half != 2) {
            throw new IllegalArgumentException("Semestre deve ser 1 ou 2.");
        }
    }

    public static Semester of(LocalDate date) {
        return new Semester(date.getYear(), date.getMonthValue() <= Month.JUNE.getValue() ? 1 : 2);
    }

    public LocalDate firstDay() {
        return half == 1 ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 7, 1);
    }

    public LocalDate lastDay() {
        return half == 1 ? LocalDate.of(year, 6, 30) : LocalDate.of(year, 12, 31);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(firstDay()) && !date.isAfter(lastDay());
    }

    @Override
    public int compareTo(Semester other) {
        int byYear = Integer.compare(year, other.year);
        return byYear != 0 ? byYear : Integer.compare(half, other.half);
    }

    @Override
    public String toString() {
        return year + "/" + half;
    }
}
