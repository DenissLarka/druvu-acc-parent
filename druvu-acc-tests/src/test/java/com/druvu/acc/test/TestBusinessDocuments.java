package com.druvu.acc.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.BillLine;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Entry;
import com.druvu.acc.api.entity.EntryTax;
import com.druvu.acc.api.entity.Invoice;
import com.druvu.acc.api.entity.InvoiceLine;
import com.druvu.acc.api.entity.Job;
import com.druvu.acc.api.entity.Order;
import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.api.entity.OwnerType;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Document tests against {@code business.gnucash}: a job-owned posted invoice with one entry - the exact chain issue
 * #29 asks to resolve.
 */
public class TestBusinessDocuments {

    private static final String CUSTOMER_ID = "eac642afcba14bc8a237aa0d9e15418a";
    private static final String VENDOR_ID = "5243362923b74c9c9b2c3fb0cf1ed034";
    private static final String JOB_ID = "b771b53fdcc44533b84071cc39749560";
    private static final String INVOICE_ID = "5aba7c49258c4229a74055d98d71c755";
    private static final String ENTRY_ID = "243b6819b8e04248a2dea00f821fb954";
    private static final String BILL_TERM_ID = "a58f650d5e7a4b4fb38ce9660ce57e10";
    private static final String TAX_TABLE_ID = "3bb0afed41f9425baa446df588099ecd";
    private static final String FROZEN_TAX_TABLE_ID = "b940561e709141468a7a368756fc3257";

    /** The posting transaction of invoice 33 - what a reporting caller starts from. */
    private static final String POSTING_TX_ID = "e51b9b2caefa49168be9b2837e311963";

    /** The income account of the fixture's entry. */
    private static final String INCOME_ACCOUNT_ID = "95b86bf991504acd8548eea71cdacc10";

    private Path source;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        source = Paths.get(
                TestBusinessDocuments.class.getResource("/business.gnucash").toURI());
    }

    // ========== Reading the real book ==========

    @Test
    public void readsJobFromRealBook() {
        Job job = AccStore.load(source).jobById(JOB_ID).orElseThrow();

        assertEquals(job.number(), "12");
        assertEquals(job.name(), "JobName1");
        assertEquals(job.reference(), Optional.of("22"));
        assertEquals(job.owner(), Owner.customer(CUSTOMER_ID));
        assertTrue(job.active());
    }

    @Test
    public void readsInvoiceFromRealBook() {
        Invoice invoice = AccStore.load(source).invoiceById(INVOICE_ID).orElseThrow();

        assertEquals(invoice.number(), "33");
        assertEquals(invoice.owner(), Owner.job(JOB_ID));
        assertEquals(invoice.opened(), LocalDateTime.of(2026, 8, 8, 10, 59, 0));
        assertEquals(invoice.billingId(), Optional.of("22"));
        assertEquals(invoice.notes(), Optional.of("inv notes"));
        assertEquals(invoice.termsId(), Optional.empty());
        assertEquals(invoice.currency().id(), "CHF");
        assertTrue(invoice.active());

        Invoice.Posting posting = invoice.posting().orElseThrow();
        assertEquals(posting.when(), LocalDateTime.of(2026, 8, 8, 10, 59, 0));
        assertEquals(posting.transactionId(), Optional.of(POSTING_TX_ID));
        assertEquals(posting.lotId(), Optional.of("24a0db2d77b243bb9c7e730f7ca5457e"));
        assertEquals(posting.accountId(), Optional.of("209cc002d4ed474090ba2281dbb286b5"));
    }

    @Test
    public void readsEntryFromRealBook() {
        Entry entry = AccStore.load(source).entryById(ENTRY_ID).orElseThrow();

        assertEquals(entry.date(), LocalDateTime.of(2026, 8, 8, 10, 0, 0));
        assertEquals(entry.action(), Optional.of("Material"));
        assertEquals(entry.quantity().orElseThrow().compareTo(BigDecimal.ONE), 0);
        assertTrue(entry.billLine().isEmpty(), "fixture entry has no bill side");

        InvoiceLine line = entry.invoiceLine().orElseThrow();
        assertEquals(line.invoiceId(), INVOICE_ID);
        assertEquals(line.accountId(), Optional.of(INCOME_ACCOUNT_ID));
        assertEquals(line.price().orElseThrow().compareTo(new BigDecimal("190")), 0);
        assertEquals(line.discount(), Optional.empty(), "disc-type/how are stored even with no amount");
        assertEquals(line.discountKind(), InvoiceLine.DiscountKind.PERCENT);
        assertEquals(line.discountHow(), InvoiceLine.DiscountHow.POSTTAX);
        assertTrue(line.tax().taxable());
        assertTrue(line.tax().taxIncluded());
        assertEquals(line.tax().taxTableId(), Optional.of(FROZEN_TAX_TABLE_ID));
    }

    // ========== Issue #29: the customer behind a transaction ==========

    @Test
    public void resolvesTheCustomerBehindAPostedInvoiceTransaction() {
        AccStore store = AccStore.load(source);

        // The full #29 chain in one call: transaction -> invoice -> job -> customer.
        Customer customer = store.customerForTransaction(POSTING_TX_ID).orElseThrow();
        assertEquals(customer.name(), "Cust 1");

        // And the individual hops, for callers that hold an invoice already.
        assertEquals(store.invoiceForTransaction(POSTING_TX_ID).orElseThrow().number(), "33");
        assertEquals(store.customerForInvoice(INVOICE_ID).orElseThrow().id(), CUSTOMER_ID);
        assertTrue(
                store.customerForTransaction("00000000000000000000000000000000").isEmpty());
    }

    @Test
    public void entriesForInvoiceFindsTheDocumentLines() {
        AccStore store = AccStore.load(source);
        assertEquals(store.entriesForInvoice(INVOICE_ID).size(), 1);
        assertEquals(store.entriesForInvoice(INVOICE_ID).get(0).id(), ENTRY_ID);
    }

    // ========== Round trips ==========

    @Test
    public void jobRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();

        store.addJob(Job.of(id, "J-1", "Kitchen renovation", Owner.customer(CUSTOMER_ID))
                .withReference("PO-7"));

        Job reloaded = saveAndReload(store).jobById(id).orElseThrow();
        assertEquals(reloaded.number(), "J-1");
        assertEquals(reloaded.name(), "Kitchen renovation");
        assertEquals(reloaded.reference(), Optional.of("PO-7"));
        assertEquals(reloaded.owner().type(), OwnerType.CUSTOMER);
        assertTrue(saveRaw(store).contains("<gnc:count-data cd:type=\"gnc:GncJob\">2</gnc:count-data>"));
    }

    @Test
    public void orderRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();
        LocalDateTime opened = LocalDateTime.of(2026, 8, 18, 9, 0, 0);

        store.addOrder(Order.of(id, "O-1", Owner.customer(CUSTOMER_ID), opened)
                .withNotes("summer batch")
                .withReference("REF-9")
                .withClosed(opened.plusDays(3)));

        Order reloaded = saveAndReload(store).orderById(id).orElseThrow();
        assertEquals(reloaded.number(), "O-1");
        assertEquals(reloaded.opened(), opened);
        assertEquals(reloaded.closed(), Optional.of(opened.plusDays(3)));
        assertEquals(reloaded.notes(), Optional.of("summer batch"));
        assertEquals(reloaded.reference(), Optional.of("REF-9"));
        // The fixture has no orders, so this count line is created from nothing.
        assertTrue(saveRaw(store).contains("<gnc:count-data cd:type=\"gnc:GncOrder\">1</gnc:count-data>"));
    }

    @Test
    public void invoiceRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();
        LocalDateTime opened = LocalDateTime.of(2026, 8, 18, 12, 0, 0);

        store.addInvoice(Invoice.of(
                        id,
                        "INV-100",
                        Owner.customer(CUSTOMER_ID),
                        opened,
                        store.invoiceById(INVOICE_ID).orElseThrow().currency())
                .withTerms(BILL_TERM_ID)
                .withBillingId("PO-77")
                .withNotes("first of the month"));

        Invoice reloaded = saveAndReload(store).invoiceById(id).orElseThrow();
        assertEquals(reloaded.number(), "INV-100");
        assertEquals(reloaded.owner(), Owner.customer(CUSTOMER_ID));
        assertEquals(reloaded.opened(), opened);
        assertEquals(reloaded.termsId(), Optional.of(BILL_TERM_ID));
        assertEquals(reloaded.billingId(), Optional.of("PO-77"));
        assertEquals(reloaded.posting(), Optional.empty(), "a fresh document is unposted");

        String xml = saveRaw(store);
        assertTrue(xml.contains("<gnc:count-data cd:type=\"gnc:GncInvoice\">2</gnc:count-data>"));
        assertTrue(xml.contains("<billterm:refcount>2</billterm:refcount>"), "terms gained a holder");
    }

    @Test
    public void entryRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String invoiceId = store.newId();
        String entryId = store.newId();
        LocalDateTime date = LocalDateTime.of(2026, 8, 18, 14, 0, 0);

        store.addInvoice(Invoice.of(
                invoiceId,
                "INV-101",
                Owner.customer(CUSTOMER_ID),
                date,
                store.invoiceById(INVOICE_ID).orElseThrow().currency()));
        store.addEntry(Entry.of(entryId, date, "Consulting", new BigDecimal("3"))
                .withAction("Hours")
                .withNotes("on site")
                .withInvoiceLine(InvoiceLine.of(invoiceId, INCOME_ACCOUNT_ID, new BigDecimal("150"))
                        .withDiscount(BigDecimal.TEN)
                        .withTax(EntryTax.table(TAX_TABLE_ID))));

        AccStore reloaded = saveAndReload(store);
        Entry entry = reloaded.entryById(entryId).orElseThrow();
        assertEquals(entry.description(), Optional.of("Consulting"));
        assertEquals(entry.action(), Optional.of("Hours"));
        assertEquals(entry.notes(), Optional.of("on site"));
        assertEquals(entry.quantity().orElseThrow().compareTo(new BigDecimal("3")), 0);

        InvoiceLine line = entry.invoiceLine().orElseThrow();
        assertEquals(line.invoiceId(), invoiceId);
        assertEquals(line.price().orElseThrow().compareTo(new BigDecimal("150")), 0);
        assertEquals(line.discount().orElseThrow().compareTo(BigDecimal.TEN), 0);
        assertEquals(line.tax().taxTableId(), Optional.of(TAX_TABLE_ID));
        assertTrue(line.tax().taxable());

        assertEquals(reloaded.entriesForInvoice(invoiceId).size(), 1);
        assertTrue(saveRaw(store).contains("<gnc:count-data cd:type=\"gnc:GncEntry\">2</gnc:count-data>"));
    }

    @Test
    public void chargebackEntryCarriesBothSides() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String billId = store.newId();
        String invoiceId = store.newId();
        String entryId = store.newId();
        LocalDateTime date = LocalDateTime.of(2026, 8, 18, 15, 0, 0);
        var currency = store.invoiceById(INVOICE_ID).orElseThrow().currency();

        // A vendor bill whose cost is chargeable onward, later pulled onto the customer's invoice -
        // GnuCash's chargeback flow leaves ONE entry on BOTH documents.
        store.addInvoice(Invoice.of(billId, "BILL-1", Owner.vendor(VENDOR_ID), date, currency));
        store.addInvoice(Invoice.of(invoiceId, "INV-104", Owner.customer(CUSTOMER_ID), date, currency));
        store.addEntry(Entry.of(entryId, date, "Freight", BigDecimal.ONE)
                .withBillLine(BillLine.of(billId, INCOME_ACCOUNT_ID, new BigDecimal("40"))
                        .chargeableTo(Owner.customer(CUSTOMER_ID))
                        .withPayment(BillLine.Payment.CARD))
                .withInvoiceLine(InvoiceLine.of(invoiceId, INCOME_ACCOUNT_ID, new BigDecimal("40"))));

        Entry entry = saveAndReload(store).entryById(entryId).orElseThrow();
        BillLine billLine = entry.billLine().orElseThrow();
        assertEquals(billLine.billId(), billId);
        assertTrue(billLine.billable());
        assertEquals(billLine.billTo(), Optional.of(Owner.customer(CUSTOMER_ID)));
        assertEquals(billLine.payment(), Optional.of(BillLine.Payment.CARD));
        assertEquals(entry.invoiceLine().orElseThrow().invoiceId(), invoiceId);
    }

    @Test
    public void updateInvoiceMovesTermsRefcount() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        Invoice invoice = store.invoiceById(INVOICE_ID).orElseThrow();
        assertEquals(invoice.termsId(), Optional.empty());

        store.updateInvoice(invoice.withTerms(BILL_TERM_ID));
        assertTrue(saveRaw(store).contains("<billterm:refcount>2</billterm:refcount>"), "terms gained a holder");

        store.updateInvoice(invoice);
        assertTrue(saveRaw(store).contains("<billterm:refcount>1</billterm:refcount>"), "back to one holder");
    }

    // ========== Removal guards ==========

    @Test
    public void removePostedInvoiceRefused() {
        WritableAccStore store = AccStore.loadWritable(source);
        IllegalStateException refusal =
                expectThrows(IllegalStateException.class, () -> store.removeInvoice(INVOICE_ID));
        assertTrue(refusal.getMessage().contains("unpost"), refusal.getMessage());
    }

    @Test
    public void removeInvoiceRefusedWhileEntriesReferenceIt() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String invoiceId = store.newId();
        String entryId = store.newId();
        LocalDateTime date = LocalDateTime.of(2026, 8, 18, 16, 0, 0);

        store.addInvoice(Invoice.of(
                invoiceId,
                "INV-102",
                Owner.customer(CUSTOMER_ID),
                date,
                store.invoiceById(INVOICE_ID).orElseThrow().currency()));
        store.addEntry(Entry.of(entryId, date, "Line", BigDecimal.ONE)
                .withInvoiceLine(InvoiceLine.of(invoiceId, INCOME_ACCOUNT_ID, BigDecimal.TEN)));

        assertThrows(IllegalStateException.class, () -> store.removeInvoice(invoiceId));

        // Removing the line first unblocks the document.
        store.removeEntry(entryId);
        store.removeInvoice(invoiceId);
        assertTrue(saveAndReload(store).invoiceById(invoiceId).isEmpty());
    }

    @Test
    public void removeJobRefusedWhileADocumentNamesIt() {
        WritableAccStore store = AccStore.loadWritable(source);
        IllegalStateException refusal = expectThrows(IllegalStateException.class, () -> store.removeJob(JOB_ID));
        assertTrue(refusal.getMessage().contains("invoice 33"), refusal.getMessage());
    }

    @Test
    public void removeCustomerRefusedWhileTheJobNamesIt() {
        WritableAccStore store = AccStore.loadWritable(source);
        IllegalStateException refusal =
                expectThrows(IllegalStateException.class, () -> store.removeCustomer(CUSTOMER_ID));
        assertTrue(refusal.getMessage().contains("JobName1"), refusal.getMessage());
    }

    @Test
    public void removeOrderRefusedWhileAnEntryReferencesIt() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String orderId = store.newId();
        String invoiceId = store.newId();
        LocalDateTime date = LocalDateTime.of(2026, 8, 18, 17, 0, 0);

        store.addOrder(Order.of(orderId, "O-2", Owner.customer(CUSTOMER_ID), date));
        store.addInvoice(Invoice.of(
                invoiceId,
                "INV-103",
                Owner.customer(CUSTOMER_ID),
                date,
                store.invoiceById(INVOICE_ID).orElseThrow().currency()));
        store.addEntry(Entry.of(store.newId(), date, "Ordered line", BigDecimal.ONE)
                .withOrder(orderId)
                .withInvoiceLine(InvoiceLine.of(invoiceId, INCOME_ACCOUNT_ID, BigDecimal.ONE)));

        assertThrows(IllegalStateException.class, () -> store.removeOrder(orderId));
    }

    // ========== Strict-write guards ==========

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addJobRefusesAJobOwner() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addJob(Job.of(store.newId(), "J-9", "Nested", Owner.job(JOB_ID)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addJobRefusesAnUnknownOwner() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addJob(Job.of(store.newId(), "J-9", "Orphan", Owner.customer("00000000000000000000000000000000")));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addEntryRefusesAnUnknownInvoice() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addEntry(Entry.of(store.newId(), LocalDateTime.of(2026, 8, 18, 18, 0, 0), "Lost", BigDecimal.ONE)
                .withInvoiceLine(
                        InvoiceLine.of("00000000000000000000000000000000", INCOME_ACCOUNT_ID, BigDecimal.ONE)));
    }

    @Test
    public void validateCatchesAFabricatedPosting() {
        WritableAccStore store = AccStore.loadWritable(source);
        Invoice invoice = store.invoiceById(INVOICE_ID).orElseThrow();
        Invoice.Posting fabricated = new Invoice.Posting(
                LocalDateTime.of(2026, 8, 18, 19, 0, 0),
                Optional.of("00000000000000000000000000000000"),
                Optional.empty(),
                Optional.empty());
        store.updateInvoice(new Invoice(
                invoice.id(),
                invoice.number(),
                invoice.owner(),
                invoice.opened(),
                invoice.billingId(),
                invoice.notes(),
                invoice.termsId(),
                invoice.currency(),
                invoice.billTo(),
                invoice.chargeAmount(),
                Optional.of(fabricated),
                invoice.active()));

        assertTrue(
                store.validate().stream().anyMatch(p -> p.contains("claims a posting transaction")),
                "expected a fabricated-posting problem: " + store.validate());
    }

    // ========== Helpers ==========

    private AccStore saveAndReload(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-documents", ".gnucash");
        try {
            store.save(out);
            return AccStore.load(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    /** Saves uncompressed and returns the raw XML - counts and refcounts are not exposed by the API, by design. */
    private String saveRaw(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-documents", ".xml");
        try {
            store.save(out);
            return Files.readString(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
