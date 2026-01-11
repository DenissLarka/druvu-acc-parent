package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccPrice;
import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.auxiliary.Fractions;
import com.druvu.acc.gnucash.generated.Price;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * GnuCash XML implementation of AccPrice.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@RequiredArgsConstructor
public class GnucashAccPrice implements AccPrice {

	private final Price peer;

	private BigDecimal cachedValue;

	@Override
	public String id() {
		return peer.getPriceId().getValue();
	}

	@Override
	public CommodityId commodity() {
		var commodity = peer.getPriceCommodity();
		return new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId());
	}

	@Override
	public CommodityId currency() {
		var currency = peer.getPriceCurrency();
		return new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId());
	}

	@Override
	public ZonedDateTime time() {
		String tsDate = peer.getPriceTime().getTsDate();
		return DateTimeUtils.parseTimestamp(tsDate);
	}

	@Override
	public String source() {
		return peer.getPriceSource();
	}

	@Override
	public Optional<String> type() {
		return Optional.ofNullable(peer.getPriceType());
	}

	@Override
	public BigDecimal value() {
		if (cachedValue == null) {
			cachedValue = Fractions.parse(peer.getPriceValue());
		}
		return cachedValue;
	}
}
