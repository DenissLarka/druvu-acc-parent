package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccCommodity;
import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.gnucash.generated.GncV2;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * GnuCash XML implementation of AccCommodity.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@RequiredArgsConstructor
public class GnucashAccCommodity implements AccCommodity {

	private final GncV2.GncBook.GncCommodity peer;

	@Override
	public CommodityId commodityId() {
		return new CommodityId(peer.getCmdtySpace(), peer.getCmdtyId());
	}

	@Override
	public Optional<String> name() {
		return Optional.ofNullable(peer.getCmdtyName());
	}

	@Override
	public Optional<String> exchangeCode() {
		return Optional.ofNullable(peer.getCmdtyXcode());
	}

	@Override
	public int fraction() {
		Integer fraction = peer.getCmdtyFraction();
		return fraction != null ? fraction : 100;
	}

	@Override
	public boolean quotesEnabled() {
		return peer.getCmdtyGetQuotes() != null;
	}

	@Override
	public Optional<String> quoteSource() {
		return Optional.ofNullable(peer.getCmdtyQuoteSource());
	}

	@Override
	public Optional<String> quoteTimezone() {
		return Optional.ofNullable(peer.getCmdtyQuoteTz());
	}
}
