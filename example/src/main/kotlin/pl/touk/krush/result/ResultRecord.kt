package pl.touk.krush.result

import javax.persistence.*

enum class RecordType {
    CALL
}

@Embeddable
data class RecordId(
    @Column(name = "RUN_ID")
    val runId: String,

    @Column(name = "RECORD_ID")
    val id: String,

    @Column(name = "RECORD_TYPE")
    @Enumerated(EnumType.STRING)
    val type: RecordType
)

@Entity
@Table(name = "RESULTS_RECORDS")
data class ResultRecord(
    @EmbeddedId
    val id: RecordId,

    @ManyToOne
    @JoinColumn(name = "RUN_ID")
    val summary: RunSummary

)
