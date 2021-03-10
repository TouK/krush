package pl.touk.krush.one2one

import javax.persistence.*

@Entity
@Table(name = "RUNS")
data class Run(

    @Id
    @Column(name = "RUN_ID")
    var runId: String
)

@Entity
@Table(name = "UR_RESULTS_SUMMARY")
data class RunSummary(
    @Id
    @Column(name = "RUN_ID")
    var runId: String,

    @OneToOne
    @JoinColumn(name = "RUN_ID")
    val run: Run,

    @OneToMany(mappedBy = "summary")
    val records: List<ResultRecord>
)
