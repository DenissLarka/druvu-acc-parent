package com.druvu.acc.gnucash.mapper;

import java.util.Optional;

import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;

/**
 * Maps GnuCash XML Price entity to AccPrice business object.
 *
 * @author Deniss Larka
 * <br/>on 12 Jan 2026
 */
public final class PriceMapper {

	private PriceMapper() {
	}

	public static Price map(com.druvu.acc.gnucash.generated.Price peer) {
		var commodity = peer.getPriceCommodity();
		var currency = peer.getPriceCurrency();

		return new Price(
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
