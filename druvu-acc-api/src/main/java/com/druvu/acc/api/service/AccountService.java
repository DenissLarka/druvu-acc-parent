package com.druvu.acc.api.service;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Amount;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAmount;
import com.druvu.acc.api.entity.Split;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Business logic for account operations.
 *
 * @author : Deniss Larka on 14 Jan 2026
 */
@AllArgsConstructor
public class AccountService {

    private final AccStore store;
    private final String rootAccountName;

    public static AccountService create(AccStore store) {
        return create(store, null);
    }

    public static AccountService create(AccStore store, String rootAccountName) {
        return new AccountService(store, rootAccountName);
    }

    public Account accountByName(String accountName) {
        final Optional<Account> accAccountOpt = rootAccountName == null
                ? store.accountByName(accountName)
                : store.accountByName(rootAccountName + ':' + accountName);
        return accAccountOpt.orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountName));
    }

    /**
     * Balance of this account alone, in the account's own commodity.
     *
     * <p>Sub-accounts are <em>not</em> included - this is the "Balance" column of the GnuCash account tree. For the
     * "Total" column, which rolls sub-accounts up, use {@link #totalBalance(String)}.
     *
     * @param accountId the account ID
     * @return the summed quantity of the account's own splits, carrying its commodity
     */
    public Amount balance(String accountId) {
        return balance(accountId, null);
    }

    /**
     * Balance of this account alone as of a date, in the account's own commodity.
     *
     * @param accountId the account ID
     * @param toDate include splits posted on or before this date; {@code null} for all splits
     * @return the summed quantity of the account's own splits, carrying its commodity
     * @throws IllegalArgumentException if no account has that ID
     * @throws IllegalStateException if the account declares no commodity, so the quantity cannot be named
     * @see #balance(String)
     */
    public Amount balance(@NonNull String accountId, LocalDate toDate) {
        final Account account = store.accountById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        final CommodityId commodity = account.commodity()
                .orElseThrow(() -> new IllegalStateException(
                        "Account declares no commodity, its balance cannot be named: " + accountId));
        return new Amount(sumSplits(store.splitsForAccount(accountId), toDate), commodity);
    }

    /**
     * Subtree total for a book that uses a single commodity - which most books do.
     *
     * <p>The comfortable form of {@link #totalBalance(String)}: it hands back an {@link Amount} directly instead of
     * making every caller unwrap a {@link MultiAmount} they know holds one thing. If the subtree turns out to mix
     * commodities the assumption was wrong, and this says so rather than quietly reporting one of them.
     *
     * @param accountId the account ID at the root of the subtree
     * @return the subtree total, in the single commodity it is held in
     * @throws IllegalStateException if the subtree holds no commodity or more than one
     */
    public Amount totalAmount(String accountId) {
        return totalAmount(accountId, null);
    }

    /**
     * Subtree total as of a date, for a book that uses a single commodity.
     *
     * @param accountId the account ID at the root of the subtree
     * @param toDate include splits posted on or before this date; {@code null} for all splits
     * @return the subtree total, in the single commodity it is held in
     * @throws IllegalStateException if the subtree holds no commodity or more than one
     * @see #totalAmount(String)
     */
    public Amount totalAmount(String accountId, LocalDate toDate) {
        final MultiAmount total = totalBalance(accountId, toDate);
        return total.single()
                .orElseThrow(() -> new IllegalStateException("Subtree of account " + accountId + " holds "
                        + (total.isEmpty() ? "no commodity" : total.toString())
                        + "; use totalBalance(...) for a subtree that mixes commodities"));
    }

    /**
     * Balance of this account and every account below it - the "Total" column of the GnuCash account tree.
     *
     * <p>A subtree may hold several commodities (a EUR account and a NASDAQ/AAPL account under one parent), so the
     * result is a {@link MultiAmount} carrying one figure per commodity. Nothing is converted: no exchange rate is
     * applied and no commodity is dropped. Callers needing a single number convert the parts themselves, typically via
     * {@link AccStore#prices()}.
     *
     * @param accountId the account ID at the root of the subtree
     * @return the summed quantities of this account and all its descendants, per commodity
     */
    public MultiAmount totalBalance(String accountId) {
        return totalBalance(accountId, null);
    }

    /**
     * Balance of this account and every account below it, as of a date.
     *
     * @param accountId the account ID at the root of the subtree
     * @param toDate include splits posted on or before this date; {@code null} for all splits
     * @return the summed quantities of this account and all its descendants, per commodity
     * @throws IllegalStateException if an account carrying splits declares no commodity, which would mean silently
     *     dropping its quantities from the total
     * @see #totalBalance(String)
     */
    public MultiAmount totalBalance(@NonNull String accountId, LocalDate toDate) {
        MultiAmount total = MultiAmount.empty();
        // Iterative walk with a visited guard: a malformed book can contain a parent cycle, and a
        // recursive walk would blow the stack instead of reporting anything useful.
        final Set<String> visited = new HashSet<>();
        final Deque<String> pending = new ArrayDeque<>();
        pending.push(accountId);

        while (!pending.isEmpty()) {
            final String currentId = pending.pop();
            if (!visited.add(currentId)) {
                continue;
            }
            total = ownAmount(currentId, toDate).map(total::plus).orElse(total);
            store.fetchChildIds(currentId).forEach(pending::push);
        }
        return total;
    }

    /**
     * One account's own balance. A single account holds exactly one commodity, so the result is an {@link Amount}, not
     * a {@link MultiAmount} - only a subtree can span several. Empty when the account declares no commodity at all,
     * which a root legitimately may.
     */
    private Optional<Amount> ownAmount(String accountId, LocalDate toDate) {
        final List<Split> splits = store.splitsForAccount(accountId);
        final Optional<CommodityId> commodity = store.accountById(accountId).flatMap(Account::commodity);

        if (commodity.isEmpty()) {
            // Root and placeholder-like accounts legitimately have no commodity, but then they carry
            // no splits either. Splits without a commodity are a broken book - say so rather than
            // dropping money out of the total.
            if (!splits.isEmpty()) {
                throw new IllegalStateException(
                        "Account has splits but declares no commodity, cannot be totalled: " + accountId);
            }
            return Optional.empty();
        }
        return Optional.of(new Amount(sumSplits(splits, toDate), commodity.get()));
    }

    private static BigDecimal sumSplits(List<Split> splits, LocalDate toDate) {
        final Predicate<Split> datePredicate =
                toDate != null ? split -> split.datePosted().isBefore(toDate.plusDays(1)) : _ -> true;
        return splits.stream().filter(datePredicate).map(Split::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
