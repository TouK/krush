package pl.touk.krush.one2one

import javax.persistence.*

@Entity
@Table(name = "RUNS")
data class Run(

    @Id @GeneratedValue
    var id: Long? = null,

)

@Entity
@Table(name = "UR_RESULTS_SUMMARY")
data class RunSummary(
    @Id @GeneratedValue
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "RUN_ID")
    val run: Run,

    @OneToMany(mappedBy = "summary")
    val records: List<ResultRecord>
)
