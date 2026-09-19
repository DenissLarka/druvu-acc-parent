///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1

// The contractor sends a bill - see contractor-bill.md.
// Run: jbang ContractorBill.java                    (starts from an empty book)
//      jbang ContractorBill.java my-book.gnucash     (adds the bill to a copy of your own book)

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.*;
import com.druvu.acc.api.entity.Customer.*;
import com.druvu.acc.api.entity.Entry.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ContractorBill {

    public static void main(String[] args) throws Exception {
        var store = args.length > 0
                ? AccStore.loadWritable(Path.of(args[0]))
                : AccStore.newBook(CommodityId.CHF);
        var rootId = store.rootAccounts().getFirst().id();
        var chf = CommodityId.CHF;

        var salesId = store.newId();
        store.addAccount(
          Account.of(salesId, "Sales", AccountType.INCOME)
                .withCommodity(chf)
                .withParent(rootId)
        );

        var vatAccountId = store.newId();
        store.addAccount(
          Account.of(vatAccountId, "VAT payable", AccountType.LIABILITY)
                .withCommodity(chf)
                .withParent(rootId)
        );

        var net30Id = store.newId();
        store.addBillTerm(BillTerm.netDays(net30Id, "Net 30", 30));

        var vatId = store.newId();
        store.addTaxTable(
          TaxTable.of(vatId, "VAT 8.1%",
                TaxTableEntry.percent(vatAccountId, new BigDecimal("8.1")))
        );

        var customerId = store.newId();
        store.addCustomer(
          Customer.of(customerId, "C-001", "Familie Keller", chf)
                .withAddress(
                  Address.of("Familie Keller", "Seestrasse 12", "8810 Horgen")
                        .withEmail("keller@example.ch"))
                .withTerms(net30Id)
                .withTaxTable(TaxTablePolicy.table(vatId))
        );

        var jobId = store.newId();
        store.addJob(
          Job.of(jobId, "J-2026-14", "Kitchen renovation", Owner.customer(customerId))
        );

        var invoiceId = store.newId();
        var today = LocalDateTime.of(2026, 8, 20, 9, 0, 0);
        store.addInvoice(Invoice.of(invoiceId, "2026-041", Owner.job(jobId), today, chf)
                .withTerms(net30Id)
                .withNotes("Kitchen renovation - completed 18 August")
        );

        store.addEntry(Entry.of(store.newId(), today, "Labour", new BigDecimal("16"))
                .withAction("Hours")
                .withInvoiceLine(InvoiceLine.of(invoiceId, salesId, new BigDecimal("95.00"))
                        .withTax(EntryTax.table(vatId)))
        );

        store.addEntry(
          Entry.of(store.newId(), today, "Materials: worktop and fittings", BigDecimal.ONE)
                .withAction("Material")
                .withInvoiceLine(
                  InvoiceLine.of(invoiceId, salesId, new BigDecimal("2340.00"))
                        .withTax(EntryTax.table(vatId))
                )
        );

        store.save(Path.of("contractor-billed.gnucash"));

        IO.println("Invoice 2026-041 for %s: %d lines, saved to contractor-billed.gnucash"
                .formatted(store.customerById(customerId).orElseThrow().name(),
                           store.entriesForInvoice(invoiceId).size()));
        IO.println("Open it in GnuCash: Business > Customer > Find Invoice, then post it.");
    }
}
