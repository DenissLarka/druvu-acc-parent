package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Job;
import com.druvu.acc.gnucash.generated.GncV2;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML jobs to {@link Job} business objects.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class JobMapper {

    public static Job map(GncV2.GncBook.GncGncJob peer) {
        return new Job(
                peer.getJobGuid().getValue(),
                peer.getJobId(),
                peer.getJobName(),
                Optional.ofNullable(peer.getJobReference()).filter(reference -> !reference.isBlank()),
                OwnerCodes.map(
                        peer.getJobOwner().getOwnerType(), peer.getJobOwner().getOwnerId()),
                peer.getJobActive() != 0);
    }

    /** Builds a fresh GnuCash element for a new job. */
    public static GncV2.GncBook.GncGncJob toGnc(Job job) {
        GncV2.GncBook.GncGncJob peer = new GncV2.GncBook.GncGncJob();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncJob.JobGuid guid = new GncV2.GncBook.GncGncJob.JobGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(job.id());
        peer.setJobGuid(guid);

        applyTo(peer, job);
        return peer;
    }

    /** Writes the job's fields onto the element already in the book. */
    public static void applyTo(GncV2.GncBook.GncGncJob peer, Job job) {
        peer.setJobId(job.number());
        peer.setJobName(job.name());
        peer.setJobReference(job.reference().orElse(null));

        GncV2.GncBook.GncGncJob.JobOwner owner = new GncV2.GncBook.GncGncJob.JobOwner();
        owner.setVersion(GncConstants.VERSION);
        owner.setOwnerType(OwnerCodes.wireType(job.owner()));
        owner.setOwnerId(OwnerCodes.ownerId(job.owner()));
        peer.setJobOwner(owner);

        peer.setJobActive(job.active() ? 1 : 0);
    }
}
