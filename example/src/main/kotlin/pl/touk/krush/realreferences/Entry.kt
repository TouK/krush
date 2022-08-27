package pl.touk.krush.realreferences

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "entry")
data class Entry(
    @Id
    val uuid: UUID = UUID.randomUUID(),

    @Column
    val name: String = "Data Entry",

    @ManyToOne
    val category: Category? = null,
    
    @JoinTable(name = "related_entries")
    @ManyToMany
    val relatedEntries: List<Entry> = emptyList()
)
