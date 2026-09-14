package com.druvu.acc.gnucash;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.acc.api.entity.Lot;
import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.generated.Slot;
import com.druvu.acc.gnucash.generated.SlotValue;
import com.druvu.acc.gnucash.generated.SlotsType;
import com.druvu.acc.gnucash.mapper.LotMapper;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

/**
 * The wire shapes GnuCash writes on a lot, built by hand where no fixture has them: the {@code gncOwner} frame is
 * written on payment lots that settle no document yet, which no test book contains.
 */
public class TestLotMapper {

    private static final String LOT_ID = "82d2eb7f44d44553a0fc7399aa082717";
    private static final String OWNER_ID = "899e73371d3143a097095f48b963f21d";
    private static final String INVOICE_ID = "61848de22167417986c9d681e619eaa0";

    @Test
    public void readsTheOwnerFrameOfAPaymentLot() {
        GncAccount.ActLots.GncLot peer = lot(ownerFrame("2", OWNER_ID));

        assertThat(LotMapper.owner(peer)).contains(Owner.customer(OWNER_ID));
        assertThat(LotMapper.invoiceId(peer)).isEmpty();
    }

    @Test
    public void decodesEveryOwnerTypeGnuCashCanAttach() {
        assertThat(LotMapper.owner(lot(ownerFrame("3", OWNER_ID)))).contains(Owner.job(OWNER_ID));
        assertThat(LotMapper.owner(lot(ownerFrame("4", OWNER_ID)))).contains(Owner.vendor(OWNER_ID));
        assertThat(LotMapper.owner(lot(ownerFrame("5", OWNER_ID)))).contains(Owner.employee(OWNER_ID));
        // NONE and UNDEFINED name nobody; garbage is not guessed at.
        assertThat(LotMapper.owner(lot(ownerFrame("0", OWNER_ID)))).isEmpty();
        assertThat(LotMapper.owner(lot(ownerFrame("1", OWNER_ID)))).isEmpty();
        assertThat(LotMapper.owner(lot(ownerFrame("customer", OWNER_ID)))).isEmpty();
    }

    @Test
    public void ignoresWhatIsNotAnOwnerFrame() {
        assertThat(LotMapper.owner(lot(string("title", "Invoice 1")))).isEmpty();
        assertThat(LotMapper.owner(lot(string("gncOwner", "not a frame")))).isEmpty();
        assertThat(LotMapper.owner(lot())).isEmpty();
        assertThat(LotMapper.owner(new GncAccount.ActLots.GncLot())).isEmpty();
    }

    @Test
    public void readsTheInvoiceFrameThroughTheWhitespaceJaxbKeeps() {
        // Mixed content: the frame's value holds indentation text nodes around the inner slots.
        Slot frame = slot("gncInvoice", "frame", "\n        ", slot("invoice-guid", "guid", INVOICE_ID), "\n      ");

        assertThat(LotMapper.invoiceId(lot(frame, string("title", "Invoice 000006"))))
                .contains(INVOICE_ID);
    }

    @Test
    public void mapsTitleAndNotesAndNothingElse() {
        GncAccount.ActLots.GncLot peer =
                lot(ownerFrame("2", OWNER_ID), string("title", "Prepayment"), string("notes", "n"));

        assertThat(LotMapper.map(peer, "acct"))
                .isEqualTo(new Lot(LOT_ID, "acct", Optional.of("Prepayment"), Optional.of("n")));
    }

    @Test
    public void mergingKeepsTheFramesAndWritesOnlyTitleAndNotes() {
        GncAccount.ActLots.GncLot peer = lot(ownerFrame("2", OWNER_ID), string("title", "old"));

        LotMapper.applyTo(peer, new Lot(LOT_ID, "acct", Optional.empty(), Optional.of("only notes now")));

        assertThat(LotMapper.map(peer, "acct").title()).isEmpty();
        assertThat(LotMapper.map(peer, "acct").notes()).contains("only notes now");
        assertThat(LotMapper.owner(peer)).contains(Owner.customer(OWNER_ID));
        assertThat(LotMapper.hasSlots(peer)).isTrue();
    }

    @Test
    public void aFreshLotCarriesItsVersionAndId() {
        GncAccount.ActLots.GncLot peer = LotMapper.toGnc(Lot.of(LOT_ID, "acct", "Fees"));

        assertThat(peer.getVersion()).isEqualTo("2.0.0");
        assertThat(peer.getLotId().getValue()).isEqualTo(LOT_ID);
        assertThat(peer.getLotId().getType()).isEqualTo("guid");
        assertThat(LotMapper.hasSlots(peer)).isTrue();
        assertThat(LotMapper.hasSlots(LotMapper.toGnc(new Lot(LOT_ID, "acct", Optional.empty(), Optional.empty()))))
                .isFalse();
    }

    // ========== Wire-shape builders ==========

    private static GncAccount.ActLots.GncLot lot(Slot... slots) {
        GncAccount.ActLots.GncLot peer = new GncAccount.ActLots.GncLot();
        peer.setVersion("2.0.0");
        GncAccount.ActLots.GncLot.LotId id = new GncAccount.ActLots.GncLot.LotId();
        id.setType("guid");
        id.setValue(LOT_ID);
        peer.setLotId(id);
        SlotsType container = new SlotsType();
        container.getSlot().addAll(List.of(slots));
        peer.setLotSlots(container);
        return peer;
    }

    private static Slot ownerFrame(String type, String guid) {
        return slot("gncOwner", "frame", slot("owner-type", "integer", type), slot("owner-guid", "guid", guid));
    }

    private static Slot string(String key, String value) {
        return slot(key, "string", value);
    }

    private static Slot slot(String key, String type, Object... content) {
        Slot slot = new Slot();
        slot.setSlotKey(key);
        SlotValue value = new SlotValue();
        value.setType(type);
        value.getContent().addAll(List.of(content));
        slot.setSlotValue(value);
        return slot;
    }
}
