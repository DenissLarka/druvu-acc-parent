package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Order;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML orders to {@link Order} business objects. Note {@code order:active} is a string "1"/"0" on the wire,
 * unlike every other active flag.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class OrderMapper {

    public static Order map(GncV2.GncBook.GncGncOrder peer) {
        return new Order(
                peer.getOrderGuid().getValue(),
                peer.getOrderId(),
                OwnerCodes.map(
                        peer.getOrderOwner().getOwnerType(),
                        peer.getOrderOwner().getOwnerId()),
                DateTimeUtils.parseTimestamp(peer.getOrderOpened().getTsDate()),
                Optional.ofNullable(peer.getOrderClosed())
                        .map(closed -> DateTimeUtils.parseTimestamp(closed.getTsDate())),
                Optional.ofNullable(peer.getOrderNotes()).filter(notes -> !notes.isBlank()),
                Optional.ofNullable(peer.getOrderReference()).filter(reference -> !reference.isBlank()),
                "1".equals(peer.getOrderActive()));
    }

    /** Builds a fresh GnuCash element for a new order. */
    public static GncV2.GncBook.GncGncOrder toGnc(Order order) {
        GncV2.GncBook.GncGncOrder peer = new GncV2.GncBook.GncGncOrder();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncOrder.OrderGuid guid = new GncV2.GncBook.GncGncOrder.OrderGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(order.id());
        peer.setOrderGuid(guid);

        applyTo(peer, order);
        return peer;
    }

    /** Writes the order's fields onto the element already in the book. */
    public static void applyTo(GncV2.GncBook.GncGncOrder peer, Order order) {
        peer.setOrderId(order.number());

        GncV2.GncBook.GncGncOrder.OrderOwner owner = new GncV2.GncBook.GncGncOrder.OrderOwner();
        owner.setVersion(GncConstants.VERSION);
        owner.setOwnerType(OwnerCodes.wireType(order.owner()));
        owner.setOwnerId(OwnerCodes.ownerId(order.owner()));
        peer.setOrderOwner(owner);

        GncV2.GncBook.GncGncOrder.OrderOpened opened = new GncV2.GncBook.GncGncOrder.OrderOpened();
        opened.setTsDate(DateTimeUtils.formatTimestamp(order.opened()));
        peer.setOrderOpened(opened);

        peer.setOrderClosed(order.closed()
                .map(closed -> {
                    GncV2.GncBook.GncGncOrder.OrderClosed element = new GncV2.GncBook.GncGncOrder.OrderClosed();
                    element.setTsDate(DateTimeUtils.formatTimestamp(closed));
                    return element;
                })
                .orElse(null));

        peer.setOrderNotes(order.notes().orElse(null));
        peer.setOrderReference(order.reference().orElse(null));
        peer.setOrderActive(order.active() ? "1" : "0");
    }
}
