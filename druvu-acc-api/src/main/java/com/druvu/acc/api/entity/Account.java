package com.druvu.acc.api.entity;

import com.druvu.acc.api.AccStore;
import java.util.Optional;

/**
 * Account data entity - pure data holder without business logic.
 *
 * <p>Use {@link AccStore} for lookups, navigation (parent/children), and computed values like qualified names.
 *
 * @param id unique ID
 * @param name account name (simple name, not qualified)
 * @param type account type
 * @param code optional account code/number
 * @param description optional description
 * @param commodity account commodity (currency)
 * @param parentId ID of a parent account, empty for root accounts
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
        Optional<String> parentId) {

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
        return new Account(id, name, type, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * @param code the account code/number
     * @return a copy carrying that code
     */
    public Account withCode(String code) {
        return new Account(id, name, type, Optional.of(code), description, commodity, parentId);
    }

    /**
     * @param description the account description
     * @return a copy carrying that description
     */
    public Account withDescription(String description) {
        return new Account(id, name, type, code, Optional.of(description), commodity, parentId);
    }

    /**
     * @param commodity the commodity the account is denominated in
     * @return a copy carrying that commodity
     */
    public Account withCommodity(CommodityId commodity) {
        return new Account(id, name, type, code, description, Optional.of(commodity), parentId);
    }

    /**
     * @param parentId ID of the parent account
     * @return a copy sitting under that parent
     */
    public Account withParent(String parentId) {
        return new Account(id, name, type, code, description, commodity, Optional.of(parentId));
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
        builder.append(']');
        return builder.toString();
    }
}
