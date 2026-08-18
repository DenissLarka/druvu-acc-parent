package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Employee;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.Fractions;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML employees to {@link Employee} business objects. The access-control string and default credit-card
 * account are GnuCash bookkeeping, unmodelled; {@link #applyTo} leaves both untouched.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class EmployeeMapper {

    public static Employee map(GncV2.GncBook.GncGncEmployee peer) {
        return new Employee(
                peer.getEmployeeGuid().getValue(),
                peer.getEmployeeId(),
                peer.getEmployeeUsername(),
                AddressMapper.map(peer.getEmployeeAddr()),
                Optional.ofNullable(peer.getEmployeeLanguage()).filter(language -> !language.isBlank()),
                parseOrZero(peer.getEmployeeWorkday()),
                parseOrZero(peer.getEmployeeRate()),
                new CommodityId(
                        peer.getEmployeeCurrency().getCmdtySpace(),
                        peer.getEmployeeCurrency().getCmdtyId()),
                peer.getEmployeeActive() != 0);
    }

    private static BigDecimal parseOrZero(String fraction) {
        return fraction == null ? BigDecimal.ZERO : Fractions.parse(fraction);
    }

    /** Builds a fresh GnuCash element for a new employee. */
    public static GncV2.GncBook.GncGncEmployee toGnc(Employee employee) {
        GncV2.GncBook.GncGncEmployee peer = new GncV2.GncBook.GncGncEmployee();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncEmployee.EmployeeGuid guid = new GncV2.GncBook.GncGncEmployee.EmployeeGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(employee.id());
        peer.setEmployeeGuid(guid);

        applyTo(peer, employee);
        return peer;
    }

    /** Writes the employee's fields onto the element already in the book; acl and ccard survive untouched. */
    public static void applyTo(GncV2.GncBook.GncGncEmployee peer, Employee employee) {
        peer.setEmployeeId(employee.number());
        peer.setEmployeeUsername(employee.username());
        peer.setEmployeeAddr(AddressMapper.toGnc(employee.address()));
        peer.setEmployeeLanguage(employee.language().orElse(null));
        peer.setEmployeeWorkday(Fractions.format(employee.workday()));
        peer.setEmployeeRate(Fractions.format(employee.rate()));

        GncV2.GncBook.GncGncEmployee.EmployeeCurrency currency = new GncV2.GncBook.GncGncEmployee.EmployeeCurrency();
        currency.setCmdtySpace(employee.currency().namespace());
        currency.setCmdtyId(employee.currency().id());
        peer.setEmployeeCurrency(currency);

        peer.setEmployeeActive(employee.active() ? 1 : 0);
    }
}
