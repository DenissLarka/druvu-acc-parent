package com.druvu.acc.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Amount;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.service.AccountService;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import org.testng.annotations.Test;

/**
 * A placeholder account that holds entries of its own: the write is accepted, its balance and the subtree total count
 * them, and both the flag and the entry survive a save.
 */
public class TestPlaceholderWithEntries {

    private static final String PLACEHOLDER = "Root Account:Actif";
    private static final String COUNTERPARTY = "Root Account:Dépenses:Abonnements";

    @Test
    public void entriesOnAPlaceholderAreCountedAndPreserved() throws IOException, URISyntaxException {
        Path source = Paths.get(
                TestPlaceholderWithEntries.class.getResource("/slots.gnucash").toURI());
        WritableAccStore store = AccStore.loadWritable(source);
        AccountService service = AccountService.create(store);

        Account holder = store.accountByName(PLACEHOLDER).orElseThrow();
        Account other = store.accountByName(COUNTERPARTY).orElseThrow();
        assertThat(holder.placeholder())
                .as("GnuCash flagged this account as a placeholder")
                .isTrue();
        assertThat(other.placeholder()).isFalse();
        assertThat(other.commodity()).isEqualTo(holder.commodity());

        CommodityId currency = holder.commodity().orElseThrow();
        Amount booked = new Amount(new BigDecimal("12.50"), currency);
        Amount totalBefore = service.totalAmount(holder.id());
        assertThat(service.balance(holder.id()).isZero())
                .as("the fixture books nothing onto the heading")
                .isTrue();

        String txId = store.newId();
        LocalDate date = LocalDate.of(2026, 9, 27);
        Transaction onto = Transaction.of(
                txId,
                currency,
                date,
                "Booked straight onto the placeholder",
                List.of(
                        Split.of(store.newId(), txId, holder.id(), date, booked.value()),
                        Split.of(
                                store.newId(),
                                txId,
                                other.id(),
                                date,
                                booked.value().negate())));

        assertThatCode(() -> store.addTransaction(onto)).doesNotThrowAnyException();
        assertThat(store.validate()).isEmpty();
        assertThat(service.balance(holder.id())).isEqualTo(booked);
        assertThat(service.totalAmount(holder.id())).isEqualTo(totalBefore.plus(booked));

        Path saved = Files.createTempFile("placeholder-entries", ".gnucash");
        try {
            store.save(saved);
            AccStore reloaded = AccStore.load(saved);
            AccountService again = AccountService.create(reloaded);

            assertThat(reloaded.accountById(holder.id()).orElseThrow().placeholder())
                    .isTrue();
            assertThat(reloaded.splitsForAccount(holder.id())).hasSize(1);
            assertThat(again.balance(holder.id())).isEqualTo(booked);
            assertThat(again.totalAmount(holder.id())).isEqualTo(totalBefore.plus(booked));
        } finally {
            Files.deleteIfExists(saved);
        }
    }
}
