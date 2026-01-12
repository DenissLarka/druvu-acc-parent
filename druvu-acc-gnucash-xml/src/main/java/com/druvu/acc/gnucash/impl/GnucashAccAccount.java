package com.druvu.acc.gnucash.impl;

import java.util.Optional;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccountType;
import com.druvu.acc.api.CommodityId;

/**
 * GnuCash implementation of AccAccount as an immutable record.
 * <p>
 * Pure data holder - computed values like qualified name and child IDs
 * are available via {@link com.druvu.acc.api.AccStore} methods.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public record GnucashAccAccount(
		String id,
		String name,
		AccountType type,
		Optional<String> code,
		Optional<String> description,
		Optional<CommodityId> commodity,
		Optional<String> parentId
) implements AccAccount {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("acc[");
		builder.append(type);
		builder.append(' ');
		if (commodity.isPresent()) {
			builder.append(commodity.get());
		}
		builder.append(' ');
		builder.append(name);
		builder.append(']');
		return builder.toString();
	}
}
