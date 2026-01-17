package com.druvu.acc.api.entity;

import java.util.Optional;

import com.druvu.acc.api.AccStore;

/**
 * Account data entity - pure data holder without business logic.
 * <p>
 * Use {@link AccStore} for lookups, navigation (parent/children), and computed values
 * like qualified names.
 *
 * @param id          unique ID
 * @param name        account name (simple name, not qualified)
 * @param type        account type
 * @param code        optional account code/number
 * @param description optional description
 * @param commodity   account commodity (currency)
 * @param parentId    ID of a parent account, empty for root accounts
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
 */
public record Account(
		String id,
		String name,
		AccountType type,
		Optional<String> code,
		Optional<String> description,
		Optional<CommodityId> commodity,
		Optional<String> parentId
) {

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
