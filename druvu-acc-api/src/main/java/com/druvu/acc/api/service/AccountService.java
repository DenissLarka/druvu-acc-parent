package com.druvu.acc.api.service;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAsset;
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
     * @return the summed quantity of the account's own splits
     */
    public BigDecimal balance(String accountId) {
        return balance(accountId, null);
    }

    /**
     * Balance of this account alone as of a date, in the account's own commodity.
     *
     * @param accountId the account ID
     * @param toDate include splits posted on or before this date; {@code null} for all splits
     * @return the summed quantity of the account's own splits
     * @see #balance(String)
     */
    public BigDecimal balance(@NonNull String accountId, LocalDate toDate) {
        final List<Split> splits = store.splitsForAccount(accountId);
        return sumSplits(splits, toDate);
    }

    /**
     * Balance of this account and every account below it - the "Total" column of the GnuCash account tree.
     *
     * <p>A subtree may hold several commodities (a EUR account and a NASDAQ/AAPL account under one parent), so the
     * result is a {@link MultiAsset} carrying one figure per commodity. Nothing is converted: no exchange rate is
     * applied and no commodity is dropped. Callers needing a single number convert the parts themselves, typically via
     * {@link AccStore#prices()}.
     *
     * @param accountId the account ID at the root of the subtree
     * @return the summed quantities of this account and all its descendants, per commodity
     */
    public MultiAsset totalBalance(String accountId) {
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
    public MultiAsset totalBalance(@NonNull String accountId, LocalDate toDate) {
        MultiAsset total = MultiAsset.empty();
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
            total = total.plus(ownAsset(currentId, toDate));
            store.fetchChildIds(currentId).forEach(pending::push);
        }
        return total;
    }

    private MultiAsset ownAsset(String accountId, LocalDate toDate) {
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
            return MultiAsset.empty();
        }
        return MultiAsset.of(commodity.get(), sumSplits(splits, toDate));
    }

    private static BigDecimal sumSplits(List<Split> splits, LocalDate toDate) {
        final Predicate<Split> datePredicate =
                toDate != null ? split -> split.datePosted().isBefore(toDate.plusDays(1)) : _ -> true;
        return splits.stream().filter(datePredicate).map(Split::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
