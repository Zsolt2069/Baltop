package hu.msizsolt.baltop.model;

import java.util.UUID;

public record BalanceEntry(UUID uuid, String name, double balance, int rank) {
}
