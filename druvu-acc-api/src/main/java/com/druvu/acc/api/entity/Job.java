package com.druvu.acc.api.entity;

import java.util.Optional;
import lombok.NonNull;

/**
 * A job groups documents under a customer or vendor - "the kitchen renovation", "the Q3 audit". Invoices and orders may
 * name a job as their owner instead of the party directly; the job's own owner then says whose it is.
 *
 * @param id the job ID
 * @param number the human-facing job number shown on documents
 * @param name the job name
 * @param reference the other party's reference for this job, e.g. their PO number
 * @param owner whose job this is - a {@link OwnerType#CUSTOMER} or {@link OwnerType#VENDOR}
 * @param active whether the job is offered in document dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Job(
        @NonNull String id,
        @NonNull String number,
        @NonNull String name,
        @NonNull Optional<String> reference,
        @NonNull Owner owner,
        boolean active) {

    /**
     * The common case: an active job.
     *
     * @param id the job ID
     * @param number the human-facing job number
     * @param name the job name
     * @param owner whose job this is - a customer or vendor
     * @return the job
     */
    public static Job of(String id, String number, String name, Owner owner) {
        return new Job(id, number, name, Optional.empty(), owner, true);
    }

    /**
     * @param reference the other party's reference, e.g. their PO number
     * @return a copy carrying that reference
     */
    public Job withReference(String reference) {
        return new Job(id, number, name, Optional.of(reference), owner, active);
    }

    /**
     * @param active whether the job is offered in document dialogs
     * @return a copy carrying that state
     */
    public Job withActive(boolean active) {
        return new Job(id, number, name, reference, owner, active);
    }
}
