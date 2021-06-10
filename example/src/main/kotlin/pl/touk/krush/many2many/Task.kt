package pl.touk.krush.many2many

import javax.persistence.*

@Entity
@Table(name = "board")
data class Board(
    @Id @GeneratedValue
    val id: Long? = null,

    @OneToMany
    @JoinColumn(name = "board_id")
    val tasks: List<Task> = emptyList()
)

@Entity
@Table(name = "tasks")
data class Task(

    @Id @GeneratedValue
    val id: Long? = null,

    val description: String = "",

    @JoinTable(name = "task_requirements")
    @ManyToMany
    val requirements: Collection<Task> = emptyList()
)
