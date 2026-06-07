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

	/**
	 * Maps a {@link Price} business object to a GnuCash XML price element.
	 *
	 * @param price the price to map
	 * @return the GnuCash XML representation
	 */
	public static com.druvu.acc.gnucash.generated.Price toGnc(Price price) {
		com.druvu.acc.gnucash.generated.Price peer = new com.druvu.acc.gnucash.generated.Price();

		com.druvu.acc.gnucash.generated.Price.PriceId id = new com.druvu.acc.gnucash.generated.Price.PriceId();
		id.setType(GncConstants.GUID);
		id.setValue(price.id());
		peer.setPriceId(id);

		com.druvu.acc.gnucash.generated.Price.PriceCommodity commodity =
				new com.druvu.acc.gnucash.generated.Price.PriceCommodity();
		commodity.setCmdtySpace(price.commodity().namespace());
		commodity.setCmdtyId(price.commodity().id());
		peer.setPriceCommodity(commodity);

		com.druvu.acc.gnucash.generated.Price.PriceCurrency currency =
				new com.druvu.acc.gnucash.generated.Price.PriceCurrency();
		currency.setCmdtySpace(price.currency().namespace());
		currency.setCmdtyId(price.currency().id());
		peer.setPriceCurrency(currency);

		com.druvu.acc.gnucash.generated.Price.PriceTime time =
				new com.druvu.acc.gnucash.generated.Price.PriceTime();
		time.setTsDate(DateTimeUtils.formatTimestamp(price.time()));
		peer.setPriceTime(time);

		peer.setPriceSource(price.source());
		price.type().ifPresent(peer::setPriceType);
		peer.setPriceValue(Fractions.format(price.value()));

		return peer;
	}
}
