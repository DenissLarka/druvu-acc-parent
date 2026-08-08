package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.generated.GncAccount;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncAccount entity to AccAccount business object.
 *
 * @author Deniss Larka <br>
 *     on 12 Jan 2026
 */
@UtilityClass
public final class AccountMapper {

    /** GnuCash refuses to post transactions to an account carrying this slot. */
    private static final String SLOT_PLACEHOLDER = "placeholder";

    /** Hides the account in GnuCash's account tree. */
    private static final String SLOT_HIDDEN = "hidden";

    /** Marks the account as relevant to tax reports. */
    private static final String SLOT_TAX_RELATED = "tax-related";

    /** Free-text notes; GnuCash keeps these in a slot rather than in {@code act:description}. */
    private static final String SLOT_NOTES = "notes";

    /** Display colour, e.g. {@code "rgb(237,236,235)"}. */
    private static final String SLOT_COLOR = "color";

    public static Account map(GncAccount peer) {
        var commodity = peer.getActCommodity();
        Optional<CommodityId> commodityId = commodity != null
                ? Optional.of(new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()))
                : Optional.empty();

        var parent = peer.getActParent();
        Optional<String> parentId = parent != null ? Optional.of(parent.getValue()) : Optional.empty();

        AccountType type;
        try {
            type = AccountType.valueOf(peer.getActType());
        } catch (IllegalArgumentException _) {
            type = AccountType.ASSET;
        }

        var slots = peer.getActSlots();
        return new Account(
                peer.getActId().getValue(),
                peer.getActName(),
                type,
                Optional.ofNullable(peer.getActCode()),
                Optional.ofNullable(peer.getActDescription()),
                commodityId,
                parentId,
                SlotMapper.bool(slots, SLOT_PLACEHOLDER),
                SlotMapper.bool(slots, SLOT_HIDDEN),
                SlotMapper.bool(slots, SLOT_TAX_RELATED),
                SlotMapper.string(slots, SLOT_NOTES),
                SlotMapper.string(slots, SLOT_COLOR));
    }

    /**
     * Builds a fresh GnuCash element for a new account.
     *
     * @param account the account to map
     * @param scu the smallest currency unit of the account's commodity - 100 for most currencies, but 1 for JPY and
     *     10000 for many securities. Writing a flat 100 for everything misstates the account's precision.
     * @return the JAXB peer
     */
    public static GncAccount toGnc(Account account, int scu) {
        GncAccount peer = new GncAccount();
        peer.setVersion(GncConstants.VERSION);

        GncAccount.ActId id = new GncAccount.ActId();
        id.setType(GncConstants.GUID);
        id.setValue(account.id());
        peer.setActId(id);

        account.commodity().ifPresent(commodity -> peer.setActCommodityScu(scu));

        applyTo(peer, account);
        return peer;
    }

    /**
     * Writes an account's fields onto an existing GnuCash element, <em>leaving everything else on it untouched</em>.
     *
     * <p>This is what makes an edit non-destructive. The element a book was loaded from carries far more than this
     * library models - slot keys it does not know, and in a newer GnuCash possibly whole child elements - and
     * rebuilding the element from the record would silently discard all of it. Only the fields {@link Account} actually
     * represents are written; the smallest-currency-unit, unknown slots and anything else stay as they were.
     *
     * @param peer the element to update
     * @param account the account whose fields to write
     */
    public static void applyTo(GncAccount peer, Account account) {
        peer.setActName(account.name());
        peer.setActType(account.type().name());

        account.commodity().ifPresent(commodity -> {
            GncAccount.ActCommodity actCommodity = new GncAccount.ActCommodity();
            actCommodity.setCmdtySpace(commodity.namespace());
            actCommodity.setCmdtyId(commodity.id());
            peer.setActCommodity(actCommodity);
        });

        peer.setActCode(account.code().orElse(null));
        peer.setActDescription(account.description().orElse(null));

        account.parentId().ifPresent(parentId -> {
            GncAccount.ActParent parent = new GncAccount.ActParent();
            parent.setType(GncConstants.GUID);
            parent.setValue(parentId);
            peer.setActParent(parent);
        });

        var slots = peer.getActSlots();
        slots = SlotMapper.setBool(slots, SLOT_PLACEHOLDER, account.placeholder());
        slots = SlotMapper.setBool(slots, SLOT_HIDDEN, account.hidden());
        slots = SlotMapper.setBool(slots, SLOT_TAX_RELATED, account.taxRelated());
        slots = SlotMapper.setString(slots, SLOT_NOTES, account.notes());
        slots = SlotMapper.setString(slots, SLOT_COLOR, account.color());
        peer.setActSlots(slots);
    }
}
