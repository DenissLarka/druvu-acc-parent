# The house owner's books

You own a house. A salary arrives every month, the rent for the granny flat goes out, groceries
and electricity nibble at the rest — and at some point you want the answer to the only question
that matters: **what did this month actually cost me?**

This page builds that answer from nothing: an account tree, one month of money movements, and the
totals — all in one small Java program.

## Before the code: one idea from bookkeeping

Double-entry bookkeeping rests on a single idea: **money never appears or disappears, it moves
between accounts**. When your salary arrives, it *leaves* the account `Salary` and *reaches* the
account `Bank`. When you pay rent, it leaves `Bank` and reaches `Rent`. Every movement is recorded
on both ends, and the two ends always add up to zero.

That is the whole trick, and it is exactly what the code below writes down.

## Start with an empty book

A book begins with one line — name its currency (this story uses CHF, substitute yours) and you
get an empty book: a lone *Root Account* that every account we create will hang beneath, and
nothing else:

```java
var store = AccStore.newBook(CommodityId.CHF);
```

No file, no other program involved. (An existing book — yours, or one kept in GnuCash — is opened
with `AccStore.loadWritable(path)` instead; everything below works the same.)

## The account tree

Add the accounts: one for the bank, one for the salary, and a family of expense accounts under a
common parent:

```java
var rootId = store.rootAccounts().getFirst().id();
var chf = CommodityId.CHF;

var bankId = store.newId();
store.addAccount(
  Account.of(bankId, "Bank", AccountType.BANK)
    .withCommodity(chf)
    .withParent(rootId)
);

var salaryId = store.newId();
store.addAccount(
  Account.of(salaryId, "Salary", AccountType.INCOME)
    .withCommodity(chf)
    .withParent(rootId)
);

var expensesId = store.newId();
store.addAccount(
  Account.of(expensesId, "Household", AccountType.EXPENSE)
      .withCommodity(chf)
      .withParent(rootId)
      .withPlaceholder(true)
);

var rentId = store.newId();
store.addAccount(
  Account.of(rentId, "Rent", AccountType.EXPENSE)
      .withCommodity(chf)
      .withParent(expensesId)
);

var groceriesId = store.newId();
store.addAccount(
  Account.of(groceriesId, "Groceries", AccountType.EXPENSE)
        .withCommodity(chf)
        .withParent(expensesId)
);

var electricityId = store.newId();
store.addAccount(
  Account.of(electricityId, "Electricity", AccountType.EXPENSE)
        .withCommodity(chf)
        .withParent(expensesId)
);
```

Two things to notice:

- `store.newId()` mints IDs in the format the file needs — you never invent one yourself.
- `Household` is marked as a **placeholder**: a grouping account. GnuCash shows it as a folder and
  refuses to book money directly onto it — money goes into `Rent`, `Groceries` or `Electricity`,
  and `Household` exists to be their sum.

## A month of money movements

Now the movements. Each one is a `Transaction` with two `Split`s — the two ends of the move. The
account the money **left** gets the negative amount, the account it **reached** gets the positive
one; together they sum to zero, which is double-entry's whole promise:

```java
move(salaryId, bankId, "Salary August", "5200.00", 25);
move(bankId, rentId, "Rent September", "1800.00", 28);
move(bankId, groceriesId, "Weekly groceries", "160.45", 2);
move(bankId, groceriesId, "Weekly groceries", "142.80", 9);
move(bankId, electricityId, "Electricity July", "87.30", 12);

store.save(Path.of("household-august.gnucash"));
```

Five lines that read like the cash book itself. Each is a call to this little helper — written
once, used forever:

```java
/** One money movement: it leaves {@code from} and reaches {@code to}. */
private void move(String from, String to, String what, String amount, int day) {
    var txId = store.newId();
    var date = LocalDate.of(2026, 8, day);
    var value = new BigDecimal(amount);
    store.addTransaction(
      Transaction.of(txId, chf, date, what, List.of(
            Split.of(store.newId(), txId, from, date, value.negate()),
            Split.of(store.newId(), txId, to, date, value)))
    );
}
```

The store holds you to double entry as you go: a movement whose two ends do not cancel out is
refused on the spot. And `save` writes to a **new** file, checking the book's structure first —
if something is wrong (a split pointing at an account that does not exist, say), it refuses and
tells you what, rather than writing a broken book.

## The payoff

```java
var service = AccountService.create(store);
IO.println("Household spending: " + service.totalAmount(expensesId));
IO.println("  of which rent:    " + service.balance(rentId));
IO.println("  groceries:        " + service.balance(groceriesId));
IO.println("  electricity:      " + service.balance(electricityId));
IO.println("Bank balance:       " + service.balance(bankId));
```

which prints:

```
Household spending: 2190.55 CHF
  of which rent:    1800.00 CHF
  groceries:        303.25 CHF
  electricity:      87.30 CHF
Bank balance:       3009.45 CHF
```

`balance` is one account's own figure; `totalAmount` rolls a whole subtree up — it is the
**Total** column of GnuCash's account page, and here it answers the opening question: August cost
2190.55. Note that every figure carries its currency: the library never hands you a bare number
that could be mistaken for the wrong one.

> **A word on signs.** The books keep the raw double-entry sign, so if you print
> `service.balance(salaryId)` you get **−5200.00 CHF** — income is money that *left* its source
> account. Accountants will recognise the credit balance; GnuCash's *display* flips the sign for
> income and liability accounts to read more naturally, but the file — and this library — store
> the real one.

## See it in GnuCash

Open `household-august.gnucash` in GnuCash. There is your tree — `Household` as a folder with its
three children — the five transactions in the `Bank` register, and GnuCash's own Total column
agreeing with `totalAmount` to the rappen.

**Next story:** money that comes from other people — [the contractor sends a
bill](contractor-bill.md).
