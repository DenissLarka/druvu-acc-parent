package com.druvu.acc.gnucash.mapper;

import java.util.Optional;

import com.druvu.acc.api.AccPrice;
import com.druvu.acc.api.CommodityId;
import com.druvu.acc.gnucash.generated.Price;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;
import com.druvu.acc.gnucash.impl.GnucashAccPrice;

/**
 * Maps GnuCash XML Price entity to AccPrice business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
public final class PriceMapper {

	private PriceMapper() {
	}

	public static AccPrice map(Price peer) {
		var commodity = peer.getPriceCommodity();
		var currency = peer.getPriceCurrency();

		return new GnucashAccPrice(
				peer.getPriceId().getValue(),
				new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()),
				new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId()),
				DateTimeUtils.parseTimestamp(peer.getPriceTime().getTsDate()),
				peer.getPriceSource(),
				Optional.ofNullable(peer.getPriceType()),
				Fractions.parse(peer.getPriceValue())
		);
	}
}
