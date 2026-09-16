# Whose money is this?

The ledger fills up. Posted invoices, rent, materials, the odd refund — and one day the question
arrives, usually from whoever does the year-end: *"the 4172.66 that came in on the 19th — which
customer is that?"*

In the file, the answer is buried three hops deep: the ledger transaction points at an invoice,
the invoice at a job, the job at a customer. Following that chain by hand means digging through
GnuCash's internals. In code it is **one call**:

```java
Optional<Customer> customer = store.customerForTransaction(transactionId);
```

The call walks the whole chain for you — including the job indirection, and including invoices
owned by a customer directly — and comes back empty for transactions that have no customer behind
them (the rent does not belong to anybody).

It answers for the *payment* too. When the customer's money lands on the bank account, GnuCash
files the receivable side of that transaction in the invoice's **lot** — its private ledger of
what was billed and what came in — and the call follows it there. Posting and payment resolve to
the same name.

If the library is missing *your* question, [say
so](https://github.com/DenissLarka/druvu-acc-parent/issues) — what gets built next is decided by
whoever turns up and asks.

## A statement with names on it

Here is the whole program — it opens a book read-only and prints the ledger with a name against
every transaction that has one:

```java
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Customer;
import java.nio.file.Path;

public class WhoseMoney {

    public static void main(String[] args) throws Exception {
        var store = AccStore.load(Path.of(args[0]));   // read-only: nothing can be changed

        for (var tx : store.transactions()) {
            var who = store.customerForTransaction(tx.id())
                    .map(Customer::name)
                    .orElse("-");
            IO.println("%s  %-28s %s".formatted(tx.datePosted(), tx.description(), who));
        }
    }
}
```

It is [WhoseMoney.java](WhoseMoney.java), and it takes the book as its argument. Run it against
the contractor's book from the [previous story](contractor-bill.md) once its invoice is posted —
or against the sample book below, a real one (fictitious data) whose invoices are settled by
payments:

```bash
curl -LO https://raw.githubusercontent.com/DenissLarka/druvu-acc-parent/main/druvu-acc-tests/src/test/resources/payments.gnucash
jbang WhoseMoney.java payments.gnucash
```

```
2026-07-01  Opening balance              -
2026-07-01  Opening balance              -
2026-08-12  Customer 1                   Customer 1
2026-08-12  Customer 2                   Customer 2
2026-08-22  Customer 1                   Customer 1
2026-08-22  Customer 2                   Customer 2
2026-09-11  Customer 1                   Customer 1
2026-09-11  Monthly service charge       -
2026-09-11  Customer 1                   Customer 1
2026-09-11  123 Attorney                 -
2026-09-11  Customer 1                   Customer 1
2026-09-11  123 Attorney                 -
2026-09-11  Customer 1                   Customer 1
```

The August lines are the postings, the September ones the payments: GnuCash names a payment after
the customer, and the library finds the customer behind it the same way it did for the invoice.
The attorney's bill and the bank's charge stay unnamed — no customer is behind them.

`AccStore.load` (without `Writable`) cannot modify anything — the right tool when you only want
answers, and the polite way to open a book somebody else maintains.

## Where this leads

From here it is a short walk to real reporting: group the resolved transactions by customer and
sum the amounts, and you have per-customer income for the year — the report GnuCash does not quite
offer, built from your own book in a dozen lines. The building blocks are all on `AccStore`:
`invoiceForTransaction`, `customerForInvoice`, `entriesForInvoice` give you each hop of the chain
individually when you need something the one-call version does not cover.

The lot is reachable too — `lotForSplit`, `splitsInLot` — and it answers the other question the
year-end brings: *is this invoice paid?* GnuCash keeps no flag for it. A lot is settled when its
splits sum to zero, so the posting and the payments cancel out; anything left is what is still
owed.

That is the arc of these three stories: your own money, money others owe you, and finally the
books answering questions — which is, after all, what they are kept for.
