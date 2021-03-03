package pl.touk.tmpl.rafm.ra.model

import javax.persistence.*

enum class RecordType {
    MSS_CALL, MSS_SMS, UDR
}

@Embeddable
data class RecordId(
    @Column(name = "RECORD_ID")
    val id: String,

    @Column(name = "RECORD_TYPE")
    @Enumerated(EnumType.STRING)
    val type: RecordType
)

@Entity
@Table(name = "UR_RESULTS_RECORDS")
data class ResultRecord(
    @Id @GeneratedValue
    var id: Long? = null,

    @Embedded
    val recordId: RecordId,

    @ManyToOne
    @JoinColumn(name = "SUMMARY_ID")
    val summary: RunSummary

)
