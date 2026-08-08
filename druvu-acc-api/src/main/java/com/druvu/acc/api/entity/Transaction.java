package com.druvu.acc.api.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Transaction data entity - pure data holder without business logic.
 *
 * @param id unique ID
 * @param currency transaction currency
 * @param number optional transaction number
 * @param datePosted date the transaction was posted
 * @param description transaction description
 * @param splits splits in this transaction
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
public record Transaction(
        String id,
        CommodityId currency,
        Optional<String> number,
        LocalDate datePosted,
        String description,
        List<Split> splits)
        implements Comparable<Transaction> {

    /**
     * Canonical constructor - defensively copies the splits so the record cannot be mutated through the list a caller
     * passed in, nor through the one {@link #splits()} hands back.
     *
     * @throws NullPointerException if the split list or any split in it is {@code null}
     */
    public Transaction {
        splits = List.copyOf(splits);
    }

    /**
     * A transaction without a transaction number, which most books do not use.
     *
     * @param id unique ID - see {@link com.druvu.acc.api.WritableAccStore#newId()}
     * @param currency transaction currency
     * @param datePosted date the transaction was posted
     * @param description transaction description
     * @param splits splits in this transaction
     * @return the transaction
     */
    public static Transaction of(
            String id, CommodityId currency, LocalDate datePosted, String description, List<Split> splits) {
        return new Transaction(id, currency, Optional.empty(), datePosted, description, splits);
    }

    /**
     * @param number the transaction number
     * @return a copy carrying that number
     */
    public Transaction withNumber(String number) {
        return new Transaction(id, currency, Optional.of(number), datePosted, description, splits);
    }

    @Override
    public int compareTo(Transaction other) {
        return datePosted().compareTo(other.datePosted());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("tx[");
        builder.append(currency());
        builder.append(' ');
        builder.append(datePosted());
        for (Split split : splits) {
            builder.append(' ');
            builder.append(split);
        }
        builder.append(']');
        return builder.toString();
    }
}
