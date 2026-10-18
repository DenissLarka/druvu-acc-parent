# Ready-made scripts for a GnuCash book

Each file here is a complete program that you run with one command: no Java project, no build, no programming. The
scripts read the book with [druvu-acc](../README.md) and write spreadsheets with
[druvu-lib-excel](https://github.com/DenissLarka/druvu-lib-excel).

| Script | What it gives you |
|---|---|
| [MonthlyExpensesExcel.java](MonthlyExpensesExcel.java) | `monthly-expenses.xlsx`: what each month cost, category by category, with totals |
| [AccountRegisterExcel.java](AccountRegisterExcel.java) | `register.xlsx`: every entry of one account, with its total |
| [WhoseMoneyExcel.java](WhoseMoneyExcel.java) | `whose-money.xlsx`: every transaction with the customer behind it, customers' money highlighted |
| [HouseholdSampleBook.java](HouseholdSampleBook.java) | `household-2026.gnucash`: a made-up household's half year, to try the others on |

The three Excel scripts open your book **read-only**: nothing in it can change.

## Run one

The scripts run with [JBang](https://www.jbang.dev), which fetches Java and the libraries from Maven Central by
itself. Install it once:

```bash
# macOS / Linux
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

```powershell
# Windows (PowerShell)
iex "& { $(iwr -useb https://ps.jbang.dev) } app setup"
```

Then open a terminal in the folder that holds your book and run a script straight from GitHub:

```bash
jbang https://github.com/DenissLarka/druvu-acc-parent/blob/main/scripts/MonthlyExpensesExcel.java mybook.gnucash
jbang https://github.com/DenissLarka/druvu-acc-parent/blob/main/scripts/AccountRegisterExcel.java mybook.gnucash "Groceries"
jbang https://github.com/DenissLarka/druvu-acc-parent/blob/main/scripts/WhoseMoneyExcel.java mybook.gnucash
jbang https://github.com/DenissLarka/druvu-acc-parent/blob/main/scripts/HouseholdSampleBook.java
```

The result is written to the folder you run from, and the script names the file when it is done. An account name
with spaces goes in quotes: `"Phone & Internet"`.

The first time, JBang asks whether you trust the address. Choose **Trust limited url in future** to allow this
project's scripts from now on, or **Trust once** for this run only. Type the option's number and press Enter within
30 seconds: no answer cancels the run. The first run also downloads Java, which can take a few minutes.

A downloaded copy runs by its file name, with no question asked: `jbang MonthlyExpensesExcel.java mybook.gnucash`.

## Something else?

Each script is short: change a copy. Or give your AI assistant [the brief for assistants](../docs/ai-skill.md) and
say in plain words what you want from your book.
