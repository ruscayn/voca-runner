package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.ruscayn.vocarunner.exam.domain.Examinee;

@Embeddable
public record ExamineeEmbeddable(
        @Column(name = "device_id", nullable = false, length = 36) String deviceId,
        @Column(name = "client_ip", nullable = false, length = 45) String clientIp
) {

    static ExamineeEmbeddable from(Examinee examinee) {
        return new ExamineeEmbeddable(examinee.deviceId(), examinee.clientIp());
    }

    Examinee toDomain() {
        return new Examinee(deviceId, clientIp);
    }

}
