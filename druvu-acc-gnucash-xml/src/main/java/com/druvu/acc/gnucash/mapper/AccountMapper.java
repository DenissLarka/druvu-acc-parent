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

        return new Account(
                peer.getActId().getValue(),
                peer.getActName(),
                type,
                Optional.ofNullable(peer.getActCode()),
                Optional.ofNullable(peer.getActDescription()),
                commodityId,
                parentId);
    }

    /**
     * Maps an {@link Account} business object to a GnuCash XML {@link GncAccount} element.
     *
     * @param account the account to map
     * @return the GnuCash XML representation
     */
    public static GncAccount toGnc(Account account) {
        GncAccount peer = new GncAccount();
        peer.setVersion(GncConstants.VERSION);
        peer.setActName(account.name());

        GncAccount.ActId id = new GncAccount.ActId();
        id.setType(GncConstants.GUID);
        id.setValue(account.id());
        peer.setActId(id);

        peer.setActType(account.type().name());

        account.commodity().ifPresent(commodity -> {
            GncAccount.ActCommodity actCommodity = new GncAccount.ActCommodity();
            actCommodity.setCmdtySpace(commodity.namespace());
            actCommodity.setCmdtyId(commodity.id());
            peer.setActCommodity(actCommodity);
            peer.setActCommodityScu(GncConstants.DEFAULT_SCU);
        });

        account.code().ifPresent(peer::setActCode);
        account.description().ifPresent(peer::setActDescription);

        account.parentId().ifPresent(parentId -> {
            GncAccount.ActParent parent = new GncAccount.ActParent();
            parent.setType(GncConstants.GUID);
            parent.setValue(parentId);
            peer.setActParent(parent);
        });

        return peer;
    }
}
