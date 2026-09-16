///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.0

// Whose money is this? - see whose-money.md. Run: jbang WhoseMoney.java your-book.gnucash

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Customer;
import java.nio.file.Path;

public class WhoseMoney {

    public static void main(String[] args) throws Exception {
        var store = AccStore.load(Path.of(args[0]));   // read-only: nothing can be changed

        for (var tx : store.transactions()) {
            var who = store.customerForTransaction(tx.id())
                    .map(Customer::name)
                    .orElse("-");
            IO.println("%s  %-28s %s".formatted(tx.datePosted(), tx.description(), who));
        }
    }
}
