package com.druvu.acc.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Lot;
import com.druvu.acc.api.entity.Split;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Lot tests against {@code payments.gnucash}: a real book, contributed by the library's first external user, whose
 * invoices are settled by payments - the case the #29 resolution could not cover before lots existed. GnuCash placed
 * every payment split directly into the invoice's own lot, so each settled lot holds exactly two splits.
 */
public class TestLots {

    private static final String RECEIVABLE = "65f8b60c4bdb480f9a98ac84247ad490";
    private static final String BANK = "7467002200d943978c106213a12c72dc";

    private static final String INVOICE_1 = "66a6d04b0bfd4c669bc09b7c6efaf19e";
    private static final String INVOICE_1_LOT = "d11931bd811244cc88ef79f0411bfc1a";
    private static final String INVOICE_1_POSTING_TX = "2176328036764b9abee22c3993a7796c";
    private static final String INVOICE_1_POSTING_SPLIT = "f84cbd3345834d5a8e61138f8e99bf2a";

    private static final String PAYMENT_1_TX = "b18d43ae64f54717ae41ef9329df3967";
    private static final String PAYMENT_1_RECEIVABLE_SPLIT = "57612d50598648c2b407cd2d02ddde15";
    private static final String PAYMENT_2_TX = "30ccb4c430414345b7e1150e95aa7a2d";
    private static final String PAYMENT_5_TX = "fa8d14b815fd4c0fafb9fac43f252156";
    private static final String PAYMENT_6_TX = "c856bba622a445fa9ce0ab08cf3bc650";
    private static final String VENDOR_PAYMENT_TX = "1688e594ea3542ecb80d0cf23bf51f25";

    private static final String BANK_CHARGE_TX = "2b4c11a002cf417b93df96e3bbe3afa1";
    private static final String BANK_CHARGE_BANK_SPLIT = "cec962079ddd4a719ea897b983501944";
    private static final String BANK_CHARGE_EXPENSE_SPLIT = "46c7879983b34cfcac38170a239f95f7";

    /** {@code business.gnucash}: invoice 33's lot holds only its posting split - the invoice is unpaid. */
    private static final String OPEN_LOT = "24a0db2d77b243bb9c7e730f7ca5457e";

    private static final String UNKNOWN = "00000000000000000000000000000000";

    private Path payments;
    private Path business;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        payments = Paths.get(TestLots.class.getResource("/payments.gnucash").toURI());
        business = Paths.get(TestLots.class.getResource("/business.gnucash").toURI());
    }

    // ========== Reading ==========

    @Test
    public void readsTheLotsOfTheReceivableAccount() {
        List<Lot> lots = AccStore.load(payments).lots(RECEIVABLE);

        assertThat(lots)
                .extracting(Lot::title)
                .containsExactlyInAnyOrder(
                        Optional.of("Invoice 000001"),
                        Optional.of("Invoice 000002"),
                        Optional.of("Invoice 000005"),
                        Optional.of("Invoice 000006"));
        assertThat(lots).allSatisfy(lot -> {
            assertThat(lot.accountId()).isEqualTo(RECEIVABLE);
            assertThat(lot.notes()).isEmpty();
        });
    }

    @Test
    public void anAccountWithoutLotsHasNone() {
        AccStore store = AccStore.load(payments);

        assertThat(store.lots(BANK)).isEmpty();
        assertThat(store.lots(UNKNOWN)).isEmpty();
        assertThat(store.lotById(UNKNOWN)).isEmpty();
        assertThat(store.splitsInLot(UNKNOWN)).isEmpty();
    }

    @Test
    public void lotByIdMatchesWhatTheInvoicePostingNames() {
        AccStore store = AccStore.load(payments);

        Optional<String> posted = store.invoiceById(INVOICE_1)
                .orElseThrow()
                .posting()
                .orElseThrow()
                .lotId();
        assertThat(posted).contains(INVOICE_1_LOT);
        assertThat(store.lotById(INVOICE_1_LOT))
                .contains(new Lot(INVOICE_1_LOT, RECEIVABLE, Optional.of("Invoice 000001"), Optional.empty()));
    }

    @Test
    public void lotForSplitFindsTheLotOfPostingAndPaymentAlike() {
        AccStore store = AccStore.load(payments);

        assertThat(store.lotForSplit(INVOICE_1_POSTING_SPLIT)).map(Lot::id).contains(INVOICE_1_LOT);
        assertThat(store.lotForSplit(PAYMENT_1_RECEIVABLE_SPLIT)).map(Lot::id).contains(INVOICE_1_LOT);
        assertThat(store.lotForSplit(BANK_CHARGE_BANK_SPLIT)).isEmpty();
        assertThat(store.lotForSplit(UNKNOWN)).isEmpty();
    }

    @Test
    public void aSettledLotHoldsThePostingAndThePaymentAndSumsToZero() {
        List<Split> members = AccStore.load(payments).splitsInLot(INVOICE_1_LOT);

        assertThat(members)
                .extracting(Split::id)
                .containsExactlyInAnyOrder(INVOICE_1_POSTING_SPLIT, PAYMENT_1_RECEIVABLE_SPLIT);
        assertThat(members)
                .extracting(Split::transactionId)
                .containsExactlyInAnyOrder(INVOICE_1_POSTING_TX, PAYMENT_1_TX);
        assertThat(sum(members)).isEqualByComparingTo("0");
    }

    @Test
    public void anOpenLotHoldsOnlyThePostingAndDoesNotSumToZero() {
        List<Split> members = AccStore.load(business).splitsInLot(OPEN_LOT);

        assertThat(members).hasSize(1);
        assertThat(sum(members)).isEqualByComparingTo("190");
    }

    // ========== The payment side of #29 ==========

    @Test
    public void resolvesTheCustomerBehindAPayment() {
        AccStore store = AccStore.load(payments);

        assertThat(store.customerForTransaction(PAYMENT_1_TX))
                .map(Customer::name)
                .contains("Customer 1");
        assertThat(store.customerForTransaction(PAYMENT_2_TX))
                .map(Customer::name)
                .contains("Customer 2");
        assertThat(store.customerForTransaction(PAYMENT_5_TX))
                .map(Customer::name)
                .contains("Customer 1");
        assertThat(store.customerForTransaction(PAYMENT_6_TX))
                .map(Customer::name)
                .contains("Customer 1");
    }

    @Test
    public void leavesTransactionsWithoutACustomerUnresolved() {
        AccStore store = AccStore.load(payments);

        // A vendor's bill being paid: the lot resolves to a vendor, which is not a customer.
        assertThat(store.customerForTransaction(VENDOR_PAYMENT_TX)).isEmpty();
        // No receivable split at all.
        assertThat(store.customerForTransaction(BANK_CHARGE_TX)).isEmpty();
        assertThat(store.customerForTransaction(UNKNOWN)).isEmpty();
        // And a payment is not a document: the posting-only query stays honest.
        assertThat(store.invoiceForTransaction(PAYMENT_1_TX)).isEmpty();
    }

    @Test
    public void stillResolvesTheCustomerBehindAPostedInvoice() {
        assertThat(AccStore.load(payments).customerForTransaction(INVOICE_1_POSTING_TX))
                .map(Customer::name)
                .contains("Customer 1");
    }

    // ========== Writing ==========

    @Test
    public void addsALotAndAssignsASplitToItAcrossASave() throws IOException {
        WritableAccStore store = AccStore.loadWritable(payments);
        String lotId = store.newId();
        store.addLot(Lot.of(lotId, BANK, "Fees 2026").withNotes("what the bank charged"));
        store.assignSplitToLot(BANK_CHARGE_BANK_SPLIT, lotId);

        AccStore reloaded = saveAndReload(store);

        assertThat(reloaded.lotById(lotId))
                .contains(new Lot(lotId, BANK, Optional.of("Fees 2026"), Optional.of("what the bank charged")));
        assertThat(reloaded.lots(BANK)).extracting(Lot::id).containsExactly(lotId);
        assertThat(reloaded.lotForSplit(BANK_CHARGE_BANK_SPLIT)).map(Lot::id).contains(lotId);
        assertThat(reloaded.splitsInLot(lotId)).extracting(Split::id).containsExactly(BANK_CHARGE_BANK_SPLIT);
        assertThat(sum(reloaded.splitsInLot(lotId))).isEqualByComparingTo("-2.99");
    }

    @Test
    public void detachesTheSplitsAndRemovesTheLot() throws IOException {
        WritableAccStore store = AccStore.loadWritable(payments);
        String lotId = store.newId();
        store.addLot(Lot.of(lotId, BANK, "Fees 2026"));
        store.assignSplitToLot(BANK_CHARGE_BANK_SPLIT, lotId);

        assertThatThrownBy(() -> store.removeLot(lotId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("detach them first");

        store.detachSplitFromLot(BANK_CHARGE_BANK_SPLIT);
        store.detachSplitFromLot(BANK_CHARGE_EXPENSE_SPLIT); // was in no lot: a no-op
        store.removeLot(lotId);

        AccStore reloaded = saveAndReload(store);
        assertThat(reloaded.lots(BANK)).isEmpty();
        assertThat(reloaded.lotForSplit(BANK_CHARGE_BANK_SPLIT)).isEmpty();
        assertThat(reloaded.validate()).isEmpty();
    }

    @Test
    public void updatingALotKeepsWhatGnuCashAttachedToIt() throws IOException {
        WritableAccStore store = AccStore.loadWritable(payments);
        Lot lot = store.lotById(INVOICE_1_LOT).orElseThrow();
        store.updateLot(lot.withTitle("Invoice 000001 (renamed)").withNotes("settled in August"));

        AccStore reloaded = saveAndReload(store);

        assertThat(reloaded.lotById(INVOICE_1_LOT))
                .contains(new Lot(
                        INVOICE_1_LOT,
                        RECEIVABLE,
                        Optional.of("Invoice 000001 (renamed)"),
                        Optional.of("settled in August")));
        // The gncInvoice frame the lot carries is not modelled, yet it must survive: it is what resolves the payment.
        assertThat(reloaded.customerForTransaction(PAYMENT_1_TX))
                .map(Customer::name)
                .contains("Customer 1");
        assertThat(reloaded.splitsInLot(INVOICE_1_LOT)).hasSize(2);
    }

    @Test
    public void refusesWhatTheFileFormatOrGnuCashWouldNot() {
        WritableAccStore store = AccStore.loadWritable(payments);
        String lotId = store.newId();

        assertThatThrownBy(() -> store.addLot(new Lot(lotId, BANK, Optional.empty(), Optional.empty())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neither a title nor notes");
        assertThatThrownBy(() -> store.addLot(Lot.of(lotId, UNKNOWN, "orphan")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No account");
        assertThatThrownBy(() -> store.addLot(Lot.of(INVOICE_1_LOT, RECEIVABLE, "duplicate")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        assertThatThrownBy(() -> store.updateLot(Lot.of(INVOICE_1_LOT, BANK, "moved")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be moved");
        assertThatThrownBy(() -> store.updateLot(Lot.of(UNKNOWN, BANK, "ghost")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No lot");
        assertThatThrownBy(() -> store.removeLot(UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No lot");

        // A lot only holds splits of its own account - the rule GnuCash's engine enforces.
        assertThatThrownBy(() -> store.assignSplitToLot(BANK_CHARGE_EXPENSE_SPLIT, INVOICE_1_LOT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own account");
        assertThatThrownBy(() -> store.assignSplitToLot(UNKNOWN, INVOICE_1_LOT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No split");
        assertThatThrownBy(() -> store.assignSplitToLot(BANK_CHARGE_BANK_SPLIT, UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No lot");
        assertThatThrownBy(() -> store.detachSplitFromLot(UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No split");
    }

    @Test
    public void validateReportsSplitsLeftPointingAtLotsThatAreGone() {
        WritableAccStore store = AccStore.loadWritable(payments);
        // Removing the receivable account takes its lots with it; the payment splits still name them.
        store.removeAccount(RECEIVABLE);

        assertThat(store.validate())
                .anySatisfy(problem -> assertThat(problem).contains("in a lot that is not in the book"));
    }

    // ========== Helpers ==========

    private static BigDecimal sum(List<Split> splits) {
        return splits.stream().map(Split::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static AccStore saveAndReload(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-lots", ".gnucash");
        try {
            store.save(out);
            return AccStore.load(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
