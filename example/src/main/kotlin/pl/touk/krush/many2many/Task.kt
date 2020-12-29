package pl.touk.krush.many2many

import javax.persistence.*

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
