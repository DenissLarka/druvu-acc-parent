package com.druvu.acc.gnucash.mapper;

import java.util.Optional;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.generated.GncAccount;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncAccount entity to AccAccount business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
@UtilityClass
public final class AccountMapper {

	public static Account map(GncAccount peer) {
		var commodity = peer.getActCommodity();
		Optional<CommodityId> commodityId = commodity != null
				? Optional.of(new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()))
				: Optional.empty();

		var parent = peer.getActParent();
		Optional<String> parentId = parent != null
				? Optional.of(parent.getValue())
				: Optional.empty();

		AccountType type;
		try {
			type = AccountType.valueOf(peer.getActType());
		}
		catch (IllegalArgumentException _) {
			type = AccountType.ASSET;
		}

		return new Account(
				peer.getActId().getValue(),
				peer.getActName(),
				type,
				Optional.ofNullable(peer.getActCode()),
				Optional.ofNullable(peer.getActDescription()),
				commodityId,
				parentId
		);
	}
}
