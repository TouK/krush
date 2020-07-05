package pl.touk.krush.many2many

import javax.persistence.*

// Uncomment to test M2M with real references (requires krush.references to be set to real)
/*@Entity
@Table(name = "tasks")
data class Task(

        @Id @GeneratedValue
        val id: Long? = null,

        val description: String = "",

        @JoinTable(name = "task_requirements")
        @ManyToMany
        val requirements: Collection<Task> = emptyList()
)*/