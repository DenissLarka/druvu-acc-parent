package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.api.entity.OwnerType;
import com.druvu.acc.gnucash.generated.OwnerId;
import java.util.Optional;
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

    /**
     * The owner type as GnuCash stores it on a lot: the ordinal of its {@code GncOwnerType} enum ({@code NONE},
     * {@code UNDEFINED}, {@code CUSTOMER}, {@code JOB}, {@code VENDOR}, {@code EMPLOYEE}), written as an integer slot.
     *
     * @return the owner type, or empty for the two values that name no owner
     */
    static Optional<OwnerType> fromLotCode(long code) {
        return switch ((int) code) {
            case 2 -> Optional.of(OwnerType.CUSTOMER);
            case 3 -> Optional.of(OwnerType.JOB);
            case 4 -> Optional.of(OwnerType.VENDOR);
            case 5 -> Optional.of(OwnerType.EMPLOYEE);
            default -> Optional.empty();
        };
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
