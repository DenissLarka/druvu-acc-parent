# The contractor sends a bill

You renovated the Kellers' kitchen: sixteen hours of labour, a worktop with fittings, finished on
the 18th. Now the money has to be asked for — an invoice with VAT on top, payable within 30 days.

In GnuCash this is a click-path through the Business menus. Here it is code — which means next
month's invoice, and the fifty after it, can build themselves.

## Two accounts the bill will touch

An invoice is not yet money in the bank, but it already has an accounting meaning: it will create
**income** (your work, earned) and it collects **VAT** (the state's cut — money that passes
through your hands but was never yours, which is why it is a *liability*, not income):

```java
var store = AccStore.loadWritable(Path.of("contractor.gnucash"));
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
```

## Things you define once and reuse forever

Payment terms and tax rates belong to the business, not to one bill. *Net 30* means the client has
30 days to pay; the tax table says which rate applies and which account collects it:

```java
var net30Id = store.newId();
store.addBillTerm(BillTerm.netDays(net30Id, "Net 30", 30));

var vatId = store.newId();
store.addTaxTable(
  TaxTable.of(vatId, "VAT 8.1%",
        TaxTableEntry.percent(vatAccountId, new BigDecimal("8.1")))
);
```

## The client, and the job

The customer record carries everything you'd otherwise retype on every invoice — address, terms,
tax treatment. The **job** sits between customer and invoices: when next spring the Kellers want
the bathroom done too, its invoices get their own job under the same customer, and each project's
paperwork stays together:

```java
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
```

## The bill itself

An invoice is a header plus lines. The header names its owner — here the job, which leads to the
customer — and each line (an *entry*) says what was done, how much of it, at what price, taxed by
which table, and into which income account it belongs:

```java
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
```

16 hours at 95.00 plus 2340.00 of materials, 8.1% VAT on both — GnuCash will show 3860.00 net,
312.66 VAT, **4172.66 CHF** to be paid by 19 September.

The store checks references as you go: an invoice for a customer that does not exist, an entry
pointing at a missing invoice, a tax table over a missing account — each is refused on the spot
with a message naming the problem, not discovered later as a broken book.

## Posting: the step that stays in GnuCash

Open `contractor-billed.gnucash` in GnuCash: **Business → Customer → Find Invoice** finds
2026-041 with both lines and the VAT ready. What the code built is the *document*; making it count
in the books — **posting** — is GnuCash's move: posting turns the invoice into a ledger
transaction (the Kellers now owe you 4172.66, which is an *asset* — Accounts Receivable), and
recording their payment later clears it.

This division is deliberate. The library builds and reads the paperwork; the posting mechanics —
receivable lots, payment matching — belong to GnuCash and stay there.

**Next story:** once invoices are posted, the ledger fills with transactions — and someone will
ask [whose money each one is](whose-money.md).
