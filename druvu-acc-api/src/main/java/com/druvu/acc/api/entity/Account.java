package com.druvu.acc.api.entity;

import com.druvu.acc.api.AccStore;
import java.util.Optional;

/**
 * Account data entity - pure data holder without business logic.
 *
 * <p>Use {@link AccStore} for lookups, navigation (parent/children), and computed values like qualified names.
 *
 * <p>Most accounts need only the first few fields; build one with {@link #of(String, String, AccountType)} and add the
 * rest through the {@code with...} methods rather than calling the canonical constructor.
 *
 * <p>How a storage format records {@link #placeholder()} and the other flags is the backend's business - a caller never
 * sees the underlying representation.
 *
 * @param id unique ID
 * @param name account name (simple name, not qualified)
 * @param type account type
 * @param code optional account code/number
 * @param description optional description
 * @param commodity account commodity (currency)
 * @param parentId ID of a parent account, empty for root accounts
 * @param placeholder a grouping account that transactions may not be posted to
 * @param hidden hidden from the account tree
 * @param taxRelated relevant to tax reports
 * @param notes free-text notes, kept separately from {@link #description()}
 * @param color the account's display colour in the host application's own notation, e.g. {@code "rgb(237,236,235)"};
 *     purely presentational and not interpreted here
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
public record Account(
        String id,
        String name,
        AccountType type,
        Optional<String> code,
        Optional<String> description,
        Optional<CommodityId> commodity,
        Optional<String> parentId,
        boolean placeholder,
        boolean hidden,
        boolean taxRelated,
        Optional<String> notes,
        Optional<String> color) {

    /**
     * A new account with only the fields every account must have; everything optional is left unset and added with the
     * {@code with...} methods.
     *
     * @param id unique ID - see {@link com.druvu.acc.api.WritableAccStore#newId()}
     * @param name account name (simple name, not qualified)
     * @param type account type
     * @return the account
     */
    public static Account of(String id, String name, AccountType type) {
        return new Account(
                id,
                name,
                type,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                false,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * @param code the account code/number
     * @return a copy carrying that code
     */
    public Account withCode(String code) {
        return new Account(
                id,
                name,
                type,
                Optional.of(code),
                description,
                commodity,
                parentId,
                placeholder,
                hidden,
                taxRelated,
                notes,
                color);
    }

    /**
     * @param description the account description
     * @return a copy carrying that description
     */
    public Account withDescription(String description) {
        return new Account(
                id,
                name,
                type,
                code,
                Optional.of(description),
                commodity,
                parentId,
                placeholder,
                hidden,
                taxRelated,
                notes,
                color);
    }

    /**
     * @param commodity the commodity the account is denominated in
     * @return a copy carrying that commodity
     */
    public Account withCommodity(CommodityId commodity) {
        return new Account(
                id,
                name,
                type,
                code,
                description,
                Optional.of(commodity),
                parentId,
                placeholder,
                hidden,
                taxRelated,
                notes,
                color);
    }

    /**
     * @param parentId ID of the parent account
     * @return a copy sitting under that parent
     */
    public Account withParent(String parentId) {
        return new Account(
                id,
                name,
                type,
                code,
                description,
                commodity,
                Optional.of(parentId),
                placeholder,
                hidden,
                taxRelated,
                notes,
                color);
    }

    /**
     * A placeholder account groups its children and holds no transactions of its own; GnuCash and comparable
     * applications refuse to post to one.
     *
     * @param placeholder whether the account is a placeholder
     * @return a copy with that flag
     */
    public Account withPlaceholder(boolean placeholder) {
        return new Account(
                id, name, type, code, description, commodity, parentId, placeholder, hidden, taxRelated, notes, color);
    }

    /**
     * @param hidden whether the account is hidden from the account tree
     * @return a copy with that flag
     */
    public Account withHidden(boolean hidden) {
        return new Account(
                id, name, type, code, description, commodity, parentId, placeholder, hidden, taxRelated, notes, color);
    }

    /**
     * @param taxRelated whether the account is relevant to tax reports
     * @return a copy with that flag
     */
    public Account withTaxRelated(boolean taxRelated) {
        return new Account(
                id, name, type, code, description, commodity, parentId, placeholder, hidden, taxRelated, notes, color);
    }

    /**
     * @param notes the notes text
     * @return a copy carrying those notes
     */
    public Account withNotes(String notes) {
        return new Account(
                id,
                name,
                type,
                code,
                description,
                commodity,
                parentId,
                placeholder,
                hidden,
                taxRelated,
                Optional.of(notes),
                color);
    }

    /**
     * @param color the display colour, in the host application's own notation
     * @return a copy carrying that colour
     */
    public Account withColor(String color) {
        return new Account(
                id,
                name,
                type,
                code,
                description,
                commodity,
                parentId,
                placeholder,
                hidden,
                taxRelated,
                notes,
                Optional.of(color));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("acc[");
        builder.append(type);
        builder.append(' ');
        commodity.ifPresent(builder::append);
        builder.append(' ');
        builder.append(name);
        if (placeholder) {
            builder.append(" placeholder");
        }
        if (hidden) {
            builder.append(" hidden");
        }
        builder.append(']');
        return builder.toString();
    }
}
