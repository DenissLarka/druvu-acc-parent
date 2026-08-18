package com.druvu.acc.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Address;
import com.druvu.acc.api.entity.BillTerm;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Employee;
import com.druvu.acc.api.entity.TaxIncluded;
import com.druvu.acc.api.entity.TaxTable;
import com.druvu.acc.api.entity.TaxTableEntry;
import com.druvu.acc.api.entity.TaxTablePolicy;
import com.druvu.acc.api.entity.Vendor;
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
 * Business-party tests against {@code business.gnucash}, a book written by real GnuCash with a customer, a vendor, a
 * posted invoice and a versioned tax table.
 */
public class TestBusinessParties {

    /** {@code cust:guid} of "Cust 1" in the fixture. */
    private static final String CUSTOMER_ID = "eac642afcba14bc8a237aa0d9e15418a";

    /** {@code vendor:guid} of "Vendy" in the fixture. */
    private static final String VENDOR_ID = "5243362923b74c9c9b2c3fb0cf1ed034";

    /** {@code billterm:guid} of "MONTH1", referenced by the vendor. */
    private static final String BILL_TERM_ID = "a58f650d5e7a4b4fb38ce9660ce57e10";

    /** The visible "VAT" tax table, referenced by the vendor; its invisible frozen version is below. */
    private static final String TAX_TABLE_ID = "3bb0afed41f9425baa446df588099ecd";

    /** The invisible frozen "VAT" version - the posted invoice's entry references it. */
    private static final String FROZEN_TAX_TABLE_ID = "b940561e709141468a7a368756fc3257";

    /** The tax-collection account of both VAT versions. */
    private static final String TAX_ACCOUNT_ID = "48fba9b70e944997b5076f23534ee17c";

    private Path source;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        source = Paths.get(
                TestBusinessParties.class.getResource("/business.gnucash").toURI());
    }

    // ========== Reading the real book ==========

    @Test
    public void readsCustomerFromRealBook() {
        AccStore store = AccStore.load(source);
        Customer customer = store.customerById(CUSTOMER_ID).orElseThrow();

        assertEquals(customer.number(), "CUST1");
        assertEquals(customer.name(), "Cust 1");
        assertEquals(customer.address().name(), Optional.of("Custy 1"));
        assertEquals(customer.address().lines(), List.of("Cust 1 Addrr 222"));
        // The fixture's shipping address element is present but empty - that is "no shipping address".
        assertEquals(customer.shippingAddress(), Optional.empty());
        assertEquals(customer.taxIncluded(), TaxIncluded.USE_GLOBAL);
        assertEquals(customer.taxTable(), TaxTablePolicy.useDefault());
        assertEquals(customer.termsId(), Optional.empty());
        assertEquals(customer.discount().compareTo(BigDecimal.ZERO), 0);
        assertEquals(customer.creditLimit().compareTo(BigDecimal.ZERO), 0);
        assertEquals(customer.currency().id(), "CHF");
        assertTrue(customer.active());
    }

    @Test
    public void readsVendorFromRealBook() {
        AccStore store = AccStore.load(source);
        Vendor vendor = store.vendorById(VENDOR_ID).orElseThrow();

        assertEquals(vendor.number(), "VENDOR1");
        assertEquals(vendor.name(), "Vendy");
        assertEquals(vendor.address().lines(), List.of("Vendy Addr 123"));
        assertEquals(vendor.termsId(), Optional.of(BILL_TERM_ID));
        assertEquals(vendor.taxIncluded(), TaxIncluded.INCLUDED);
        assertEquals(vendor.taxTable(), TaxTablePolicy.table(TAX_TABLE_ID));
        assertEquals(vendor.currency().id(), "CHF");
    }

    @Test
    public void readsBillTermFromRealBook() {
        AccStore store = AccStore.load(source);
        BillTerm term = store.billTermById(BILL_TERM_ID).orElseThrow();

        assertEquals(term.name(), "MONTH1");
        assertEquals(term.description(), Optional.of("MONTH1"));
        BillTerm.Schedule.Days days = (BillTerm.Schedule.Days) term.schedule();
        assertEquals(days.dueDays(), 30);
        assertEquals(days.discountDays(), 0);
        assertEquals(days.discount().compareTo(BigDecimal.TWO), 0);
    }

    @Test
    public void readsTaxTablesIncludingFrozenVersions() {
        AccStore store = AccStore.load(source);
        List<TaxTable> tables = store.taxTables();

        // GnuCash froze an invisible copy of "VAT" when it was edited in use; the posted invoice's
        // entry references the frozen version, so both must be reachable by ID.
        assertEquals(tables.size(), 2);
        TaxTable visible = store.taxTableById(TAX_TABLE_ID).orElseThrow();
        assertEquals(visible.name(), "VAT");
        assertEquals(visible.entries().size(), 1);
        TaxTableEntry entry = visible.entries().get(0);
        assertEquals(entry.accountId(), TAX_ACCOUNT_ID);
        assertEquals(entry.kind(), TaxTableEntry.Kind.PERCENT);
        assertEquals(entry.amount().compareTo(new BigDecimal("8.1")), 0);
        assertTrue(store.taxTableById(FROZEN_TAX_TABLE_ID).isPresent(), "frozen version reachable by ID");
    }

    // ========== Round trips ==========

    @Test
    public void customerRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();

        store.addCustomer(Customer.of(
                        id,
                        "C-100",
                        "ACME AG",
                        store.customerById(CUSTOMER_ID).orElseThrow().currency())
                .withAddress(Address.of("Bahnhofstrasse 1", "8001 Zurich").withEmail("billing@acme.example"))
                .withShippingAddress(Address.of("Ramp 4", "8005 Zurich"))
                .withNotes("prefers e-bill")
                .withTerms(BILL_TERM_ID)
                .withTaxTable(TaxTablePolicy.table(TAX_TABLE_ID))
                .withDiscount(new BigDecimal("1.5"))
                .withCreditLimit(new BigDecimal("5000")));

        Customer reloaded = saveAndReload(store).customerById(id).orElseThrow();
        assertEquals(reloaded.number(), "C-100");
        assertEquals(reloaded.name(), "ACME AG");
        assertEquals(reloaded.address().lines(), List.of("Bahnhofstrasse 1", "8001 Zurich"));
        assertEquals(reloaded.address().email(), Optional.of("billing@acme.example"));
        assertEquals(reloaded.shippingAddress().orElseThrow().lines(), List.of("Ramp 4", "8005 Zurich"));
        assertEquals(reloaded.notes(), Optional.of("prefers e-bill"));
        assertEquals(reloaded.termsId(), Optional.of(BILL_TERM_ID));
        assertEquals(reloaded.taxTable(), TaxTablePolicy.table(TAX_TABLE_ID));
        assertEquals(reloaded.discount().compareTo(new BigDecimal("1.5")), 0);
        assertEquals(reloaded.creditLimit().compareTo(new BigDecimal("5000")), 0);
        assertTrue(reloaded.active());
    }

    @Test
    public void addingACustomerMaintainsCountsAndRefcounts() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addCustomer(Customer.of(store.newId(), "C-101", "Count Me", currencyOf(store))
                .withTerms(BILL_TERM_ID)
                .withTaxTable(TaxTablePolicy.table(TAX_TABLE_ID)));

        String xml = saveRaw(store);
        assertTrue(xml.contains("<gnc:count-data cd:type=\"gnc:GncCustomer\">2</gnc:count-data>"), "customer count");
        // The fixture ships both at refcount 1 (the vendor); our customer is the second holder.
        assertTrue(xml.contains("<billterm:refcount>2</billterm:refcount>"), "billing term refcount");
        assertTrue(xml.contains("<taxtable:refcount>2</taxtable:refcount>"), "tax table refcount");
    }

    @Test
    public void vendorRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();

        store.addVendor(Vendor.of(id, "V-100", "Paper AG", currencyOf(store))
                .withAddress(Address.of("Werkstrasse 9").withPhone("+41 44 000 00 00"))
                .withTaxIncluded(TaxIncluded.EXCLUDED)
                .withTaxTable(TaxTablePolicy.none()));

        Vendor reloaded = saveAndReload(store).vendorById(id).orElseThrow();
        assertEquals(reloaded.number(), "V-100");
        assertEquals(reloaded.name(), "Paper AG");
        assertEquals(reloaded.address().phone(), Optional.of("+41 44 000 00 00"));
        assertEquals(reloaded.taxIncluded(), TaxIncluded.EXCLUDED);
        assertEquals(reloaded.taxTable(), TaxTablePolicy.none());
    }

    @Test
    public void employeeRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();

        store.addEmployee(Employee.of(id, "E-1", "dlarka", currencyOf(store))
                .withAddress(Address.of("Home Office 1"))
                .withLanguage("de")
                .withWorkdayAndRate(new BigDecimal("8"), new BigDecimal("120")));

        AccStore reloaded = saveAndReload(store);
        Employee employee = reloaded.employeeById(id).orElseThrow();
        assertEquals(employee.number(), "E-1");
        assertEquals(employee.username(), "dlarka");
        assertEquals(employee.language(), Optional.of("de"));
        assertEquals(employee.workday().compareTo(new BigDecimal("8")), 0);
        assertEquals(employee.rate().compareTo(new BigDecimal("120")), 0);

        // The fixture has no employees, so this count line did not exist before.
        String xml = saveRaw(store);
        assertTrue(xml.contains("<gnc:count-data cd:type=\"gnc:GncEmployee\">1</gnc:count-data>"), "employee count");
    }

    @Test
    public void taxTableRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();

        store.addTaxTable(TaxTable.of(id, "VAT 2.6", TaxTableEntry.percent(TAX_ACCOUNT_ID, new BigDecimal("2.6"))));

        TaxTable reloaded = saveAndReload(store).taxTableById(id).orElseThrow();
        assertEquals(reloaded.name(), "VAT 2.6");
        assertEquals(reloaded.entries().size(), 1);
        assertEquals(reloaded.entries().get(0).kind(), TaxTableEntry.Kind.PERCENT);
        assertEquals(reloaded.entries().get(0).amount().compareTo(new BigDecimal("2.6")), 0);
    }

    @Test
    public void billTermRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String daysId = store.newId();
        String proximoId = store.newId();

        store.addBillTerm(BillTerm.netDays(daysId, "Net 30", 30).withDescription("payable within a month"));
        store.addBillTerm(new BillTerm(
                proximoId, "On the 15th", Optional.empty(), new BillTerm.Schedule.Proximo(15, 5, 25, BigDecimal.TWO)));

        AccStore reloaded = saveAndReload(store);
        BillTerm days = reloaded.billTermById(daysId).orElseThrow();
        assertEquals(days.description(), Optional.of("payable within a month"));
        assertEquals(((BillTerm.Schedule.Days) days.schedule()).dueDays(), 30);

        BillTerm proximo = reloaded.billTermById(proximoId).orElseThrow();
        BillTerm.Schedule.Proximo schedule = (BillTerm.Schedule.Proximo) proximo.schedule();
        assertEquals(schedule.dueDay(), 15);
        assertEquals(schedule.discountDay(), 5);
        assertEquals(schedule.cutoffDay(), 25);
        assertEquals(schedule.discount().compareTo(BigDecimal.TWO), 0);
    }

    // ========== Updates ==========

    @Test
    public void updateMovesRefcountsWithTheReference() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        Customer customer = store.customerById(CUSTOMER_ID).orElseThrow();

        // The fixture customer has no terms; giving it some makes this store the second holder.
        store.updateCustomer(customer.withTerms(BILL_TERM_ID));
        assertTrue(saveRaw(store).contains("<billterm:refcount>2</billterm:refcount>"), "term gains a holder");

        // Dropping them again hands the count back.
        store.updateCustomer(customer);
        assertTrue(saveRaw(store).contains("<billterm:refcount>1</billterm:refcount>"), "term back to one holder");
    }

    @Test
    public void switchingToDefaultTaxKeepsTheLatentTableReference() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        Vendor vendor = store.vendorById(VENDOR_ID).orElseThrow();
        assertEquals(vendor.taxTable(), TaxTablePolicy.table(TAX_TABLE_ID));

        // GnuCash keeps the chosen table stored even while the override is off, so flipping the
        // override back on finds it again. Our update must not destroy that.
        store.updateVendor(vendor.withTaxTable(TaxTablePolicy.useDefault()));

        String xml = saveRaw(store);
        assertTrue(
                xml.contains("<vendor:taxtable type=\"guid\">" + TAX_TABLE_ID + "</vendor:taxtable>"),
                "latent table reference preserved");
        assertTrue(xml.contains("<vendor:use-tt>0</vendor:use-tt>"), "override off");
        assertTrue(xml.contains("<taxtable:refcount>1</taxtable:refcount>"), "still counted as a holder");

        Vendor reloaded = saveAndReload(store).vendorById(VENDOR_ID).orElseThrow();
        assertEquals(reloaded.taxTable(), TaxTablePolicy.useDefault());
    }

    // ========== Removal guards ==========

    @Test
    public void removeBillTermRefusedWhileReferenced() {
        WritableAccStore store = AccStore.loadWritable(source);
        IllegalStateException refusal =
                org.testng.Assert.expectThrows(IllegalStateException.class, () -> store.removeBillTerm(BILL_TERM_ID));
        assertTrue(refusal.getMessage().contains("Vendy"), "refusal names the holder: " + refusal.getMessage());
    }

    @Test
    public void removeTaxTableRefusedWhileReferenced() {
        WritableAccStore store = AccStore.loadWritable(source);
        // The visible version is held by the vendor and by its frozen child's parent link.
        assertThrows(IllegalStateException.class, () -> store.removeTaxTable(TAX_TABLE_ID));
        // The frozen version is held by the posted invoice's entry.
        assertThrows(IllegalStateException.class, () -> store.removeTaxTable(FROZEN_TAX_TABLE_ID));
    }

    @Test
    public void removeCustomerHandsBackCountsAndRefcounts() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        // The fixture customer is owner of a job and cannot go; a fresh, unreferenced one can.
        String id = store.newId();
        store.addCustomer(
                Customer.of(id, "C-DEL", "Short-lived", currencyOf(store)).withTerms(BILL_TERM_ID));
        store.removeCustomer(id);

        AccStore reloaded = saveAndReload(store);
        assertTrue(reloaded.customerById(id).isEmpty());
        String xml = saveRaw(store);
        assertTrue(xml.contains("<gnc:count-data cd:type=\"gnc:GncCustomer\">1</gnc:count-data>"), "count back");
        assertTrue(xml.contains("<billterm:refcount>1</billterm:refcount>"), "terms refcount handed back");
    }

    // ========== Strict-write guards ==========

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addCustomerRefusesUnknownTerms() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addCustomer(Customer.of(store.newId(), "C-1", "X", currencyOf(store))
                .withTerms("00000000000000000000000000000000"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addCustomerRefusesUnknownTaxTable() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addCustomer(Customer.of(store.newId(), "C-1", "X", currencyOf(store))
                .withTaxTable(TaxTablePolicy.table("00000000000000000000000000000000")));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addTaxTableRefusesUnknownAccount() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addTaxTable(TaxTable.of(
                store.newId(), "Broken", TaxTableEntry.percent("00000000000000000000000000000000", BigDecimal.ONE)));
    }

    @Test
    public void validateCatchesUndefinedPartyCurrency() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addCustomer(Customer.of(store.newId(), "C-1", "Tokyo Branch", com.druvu.acc.api.entity.CommodityId.JPY));

        List<String> problems = store.validate();
        assertTrue(
                problems.stream()
                        .anyMatch(p -> p.contains("Customer") && p.contains("commodity the book does not define")),
                "expected an undefined-currency problem, got: " + problems);
        assertThrows(
                IllegalStateException.class,
                () -> store.save(Paths.get(System.getProperty("java.io.tmpdir"), "never-written-business.gnucash")));
    }

    // ========== Helpers ==========

    private com.druvu.acc.api.entity.CommodityId currencyOf(AccStore store) {
        return store.customerById(CUSTOMER_ID).orElseThrow().currency();
    }

    private AccStore saveAndReload(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-business", ".gnucash");
        try {
            store.save(out);
            return AccStore.load(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }

    /** Saves uncompressed and returns the raw XML - counts and refcounts are not exposed by the API, by design. */
    private String saveRaw(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-business", ".xml");
        try {
            store.save(out);
            return Files.readString(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
