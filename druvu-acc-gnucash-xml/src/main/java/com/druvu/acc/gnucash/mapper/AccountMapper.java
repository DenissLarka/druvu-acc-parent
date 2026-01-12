package com.druvu.acc.gnucash.mapper;

import java.util.Optional;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccountType;
import com.druvu.acc.api.CommodityId;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.impl.GnucashAccAccount;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncAccount entity to AccAccount business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
@UtilityClass
public final class AccountMapper {

	public static AccAccount map(GncAccount peer) {
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

		return new GnucashAccAccount(
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
