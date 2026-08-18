package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML addresses to the {@link Address} business object. GnuCash numbers the lines {@code addr1..addr4};
 * the business object carries them as a list.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class AddressMapper {

    public static Address map(com.druvu.acc.gnucash.generated.Address peer) {
        if (peer == null) {
            return Address.EMPTY;
        }
        List<String> lines = new ArrayList<>();
        for (String line :
                new String[] {peer.getAddrAddr1(), peer.getAddrAddr2(), peer.getAddrAddr3(), peer.getAddrAddr4()}) {
            if (line != null && !line.isBlank()) {
                lines.add(line);
            }
        }
        return new Address(
                Optional.ofNullable(peer.getAddrName()),
                lines,
                Optional.ofNullable(peer.getAddrPhone()),
                Optional.ofNullable(peer.getAddrFax()),
                Optional.ofNullable(peer.getAddrEmail()));
    }

    /** An address is fully modelled, so writing rebuilds the whole element - nothing unknown can be lost. */
    public static com.druvu.acc.gnucash.generated.Address toGnc(Address address) {
        com.druvu.acc.gnucash.generated.Address peer = new com.druvu.acc.gnucash.generated.Address();
        peer.setVersion(GncConstants.VERSION);
        peer.setAddrName(address.name().orElse(null));
        List<String> lines = address.lines();
        peer.setAddrAddr1(lines.size() > 0 ? lines.get(0) : null);
        peer.setAddrAddr2(lines.size() > 1 ? lines.get(1) : null);
        peer.setAddrAddr3(lines.size() > 2 ? lines.get(2) : null);
        peer.setAddrAddr4(lines.size() > 3 ? lines.get(3) : null);
        peer.setAddrPhone(address.phone().orElse(null));
        peer.setAddrFax(address.fax().orElse(null));
        peer.setAddrEmail(address.email().orElse(null));
        return peer;
    }
}
