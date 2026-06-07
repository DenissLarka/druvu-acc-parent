package com.druvu.acc.gnucash.mapper;

import java.util.Optional;

import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.generated.GncV2;

import lombok.experimental.UtilityClass;

/**
 * Maps between {@link Commodity} business objects and GnuCash XML {@code gnc:commodity} elements.
 *
 * @author Deniss Larka
 *         <br/>on 07 Jun 2026
 */
@UtilityClass
public final class CommodityMapper {

	/**
	 * Maps a GnuCash XML commodity element to a {@link Commodity} business object.
	 *
	 * @param peer the GnuCash XML commodity
	 * @return the business object
	 */
	public static Commodity map(GncV2.GncBook.GncCommodity peer) {
		int fraction = peer.getCmdtyFraction() != null ? peer.getCmdtyFraction() : Commodity.CURRENCY_FRACTION;
		return new Commodity(
				new CommodityId(peer.getCmdtySpace(), peer.getCmdtyId()),
				Optional.ofNullable(peer.getCmdtyName()),
				fraction);
	}

	/**
	 * Maps a {@link Commodity} business object to a GnuCash XML {@code gnc:commodity} element.
	 *
	 * @param commodity the commodity to map
	 * @return the GnuCash XML representation
	 */
	public static GncV2.GncBook.GncCommodity toGnc(Commodity commodity) {
		GncV2.GncBook.GncCommodity peer = new GncV2.GncBook.GncCommodity();
		peer.setVersion(GncConstants.VERSION);
		peer.setCmdtySpace(commodity.id().namespace());
		peer.setCmdtyId(commodity.id().id());
		commodity.name().ifPresent(peer::setCmdtyName);
		peer.setCmdtyFraction(commodity.fraction());
		return peer;
	}
}
