package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.api.entity.OwnerType;
import com.druvu.acc.gnucash.generated.OwnerId;
import lombok.experimental.UtilityClass;

/**
 * GnuCash wire values for {@link Owner} references - {@code gnc-owner-xml-v2.cpp}'s vocabulary.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
final class OwnerCodes {

    static Owner map(String wireType, OwnerId id) {
        OwnerType type =
                switch (wireType == null ? "" : wireType) {
                    case "gncCustomer" -> OwnerType.CUSTOMER;
                    case "gncVendor" -> OwnerType.VENDOR;
                    case "gncEmployee" -> OwnerType.EMPLOYEE;
                    case "gncJob" -> OwnerType.JOB;
                    default -> throw new IllegalArgumentException("Unknown GnuCash owner type: " + wireType);
                };
        return new Owner(type, id.getValue());
    }

    static String wireType(Owner owner) {
        return switch (owner.type()) {
            case CUSTOMER -> "gncCustomer";
            case VENDOR -> "gncVendor";
            case EMPLOYEE -> "gncEmployee";
            case JOB -> "gncJob";
        };
    }

    static OwnerId ownerId(Owner owner) {
        OwnerId id = new OwnerId();
        id.setType(GncConstants.GUID);
        id.setValue(owner.id());
        return id;
    }
}
