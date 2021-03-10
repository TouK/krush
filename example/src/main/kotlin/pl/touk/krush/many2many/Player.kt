package pl.touk.krush.many2many

import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinTable
import javax.persistence.ManyToMany

@Embeddable
data class CharacterId(
    val id: Long,
    val season: Int
)

@Entity
data class Character(
    @EmbeddedId
    val id: CharacterId,

    val name: String
)

@Entity
data class Player(
    @Id @GeneratedValue
    val id: Long? = null,

    val name: String,

    @ManyToMany
    @JoinTable(name = "player_characters")
    val characters: List<Character>
)
