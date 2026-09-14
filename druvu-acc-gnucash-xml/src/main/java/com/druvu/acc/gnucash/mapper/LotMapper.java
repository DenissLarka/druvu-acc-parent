package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Lot;
import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.gnucash.generated.GncAccount;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps a GnuCash lot to the {@link Lot} business object and back.
 *
 * <p>On the wire a lot is nothing but an ID and slots: {@code title} and {@code notes} are the lot's own fields and
 * become the record's; the {@code gncInvoice} and {@code gncOwner} frames are GnuCash's bookkeeping of what the lot
 * settles and are read here for {@code customerForTransaction} but never surfaced or rewritten - a
 * {@link #applyTo(GncAccount.ActLots.GncLot, Lot) merge} leaves them exactly as found.
 *
 * @author Deniss Larka <br>
 *     on 14 Sep 2026
 */
@UtilityClass
public final class LotMapper {

    private static final String VERSION = "2.0.0";

    private static final String KEY_TITLE = "title";
    private static final String KEY_NOTES = "notes";

    /** {@code gncInvoice/invoice-guid}: the document this lot tracks, attached by GnuCash on posting. */
    private static final String FRAME_INVOICE = "gncInvoice";

    private static final String KEY_INVOICE_GUID = "invoice-guid";

    /** {@code gncOwner/owner-type,owner-guid}: the owner of a payment lot that settles no document yet. */
    private static final String FRAME_OWNER = "gncOwner";

    private static final String KEY_OWNER_TYPE = "owner-type";
    private static final String KEY_OWNER_GUID = "owner-guid";

    /**
     * @param peer the GnuCash lot
     * @param accountId the account the lot was found under - the wire format keeps the lot inside the account and names
     *     it nowhere else
     * @return the business object
     */
    public static Lot map(GncAccount.ActLots.GncLot peer, String accountId) {
        return new Lot(
                peer.getLotId().getValue(),
                accountId,
                SlotMapper.string(peer.getLotSlots(), KEY_TITLE),
                SlotMapper.string(peer.getLotSlots(), KEY_NOTES));
    }

    /**
     * Builds a fresh GnuCash element for a new lot.
     *
     * @param lot the lot to map
     * @return the GnuCash XML representation
     */
    public static GncAccount.ActLots.GncLot toGnc(Lot lot) {
        GncAccount.ActLots.GncLot peer = new GncAccount.ActLots.GncLot();
        peer.setVersion(VERSION);

        GncAccount.ActLots.GncLot.LotId id = new GncAccount.ActLots.GncLot.LotId();
        id.setType(GncConstants.GUID);
        id.setValue(lot.id());
        peer.setLotId(id);

        applyTo(peer, lot);
        return peer;
    }

    /**
     * Writes a lot's title and notes onto an existing element, leaving every other slot - the document or owner GnuCash
     * attached - as it was.
     *
     * @param peer the element to update
     * @param lot the lot whose fields to write
     */
    public static void applyTo(GncAccount.ActLots.GncLot peer, Lot lot) {
        peer.setLotSlots(SlotMapper.setString(peer.getLotSlots(), KEY_TITLE, lot.title()));
        peer.setLotSlots(SlotMapper.setString(peer.getLotSlots(), KEY_NOTES, lot.notes()));
    }

    /**
     * @param peer the GnuCash lot
     * @return whether the element carries any slot at all - the schema requires at least one, so a lot without any
     *     cannot be written
     */
    public static boolean hasSlots(GncAccount.ActLots.GncLot peer) {
        return peer.getLotSlots() != null && !peer.getLotSlots().getSlot().isEmpty();
    }

    /**
     * @param peer the GnuCash lot
     * @return the ID of the document whose posting created this lot, if GnuCash attached one
     */
    public static Optional<String> invoiceId(GncAccount.ActLots.GncLot peer) {
        return SlotMapper.frameString(peer.getLotSlots(), FRAME_INVOICE, KEY_INVOICE_GUID);
    }

    /**
     * @param peer the GnuCash lot
     * @return the owner GnuCash attached to this lot directly - a payment lot that settles no document, or an unposted
     *     one - if the frame is present and names a real owner type
     */
    public static Optional<Owner> owner(GncAccount.ActLots.GncLot peer) {
        Optional<String> guid = SlotMapper.frameString(peer.getLotSlots(), FRAME_OWNER, KEY_OWNER_GUID);
        if (guid.isEmpty()) {
            return Optional.empty();
        }
        return SlotMapper.frameString(peer.getLotSlots(), FRAME_OWNER, KEY_OWNER_TYPE)
                .flatMap(LotMapper::parseLong)
                .flatMap(OwnerCodes::fromLotCode)
                .map(type -> new Owner(type, guid.get()));
    }

    private static Optional<Long> parseLong(String text) {
        try {
            return Optional.of(Long.parseLong(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
